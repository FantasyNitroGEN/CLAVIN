package com.bericotech.clavin.resolver;

import ch.qos.logback.classic.Logger;
import com.bericotech.clavin.extractor.LocationOccurrence;
import com.bericotech.clavin.gazetteer.CountryCode;
import com.bericotech.clavin.index.BinarySimilarity;
import com.bericotech.clavin.index.WhitespaceLowerCaseAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
//import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.apache.lucene.queryparser.classic.QueryParserBase.escape;

/**
 * Resolves multipart location names from structured data into GeoName objects.
 *
 * Takes multipart location names, such as what's often found in structured data
 * like a spreadsheet or database table (e.g., [Reston][Virginia][United States]),
 * and resolves them into the appropriate geographic entities by identifying the
 * most logical match in a gazetteer, trying to enforce some kind of notional
 * hierarchy of place names (e.g., city --> state/province/etc. --> country).
 */
public class MultipartLocationResolver {

    public final static Logger logger = (Logger)LoggerFactory.getLogger(MultipartLocationResolver.class);

    private IndexSearcher indexSearcher;
    private static Analyzer indexAnalyzer;

    // TODO: this seems like a reasonable magic number...
    private static final int maxHitDepth = 2000;

    // custom Lucene sorting based on Lucene match score and the
    // population of the GeoNames gazetteer entry represented by the
    // matched index document
    private static final Sort populationSort =
            new Sort(SortField.FIELD_SCORE, new SortField("population", SortField.Type.LONG, true));

    public MultipartLocationResolver(File indexDir) throws IOException, ParseException {
        // load the Lucene index directory from disk
        FSDirectory index = FSDirectory.open(indexDir.toPath());

        // index employs simple lower-casing & tokenizing on whitespace
        indexAnalyzer = new WhitespaceLowerCaseAnalyzer();
        indexSearcher = new IndexSearcher(DirectoryReader.open(index));

        // override default TF/IDF score to ignore multiple appearances
        indexSearcher.setSimilarity(new BinarySimilarity());

        // run an initial throw-away query just to "prime the pump" for
        // the cache, so we can accurately measure performance speed
        // per: http://wiki.apache.org/lucene-java/ImproveSearchingSpeed
        //indexSearcher.search(new AnalyzingQueryParser("indexNane", indexAnalyzer).parse("Reston"),maxHitDepth,populationSort); //6.0 version
        indexSearcher.search(new QueryParser("indexNane", indexAnalyzer).parse("Reston"),maxHitDepth,populationSort);
        //indexSearcher.search(new AnalyzingQueryParser("indexName", indexAnalyzer).parse("Reston"), null, maxHitDepth, populationSort);
    }

    private List<ResolvedLocation> partResolver(String locationName, String codePrefix, boolean fuzzy)
            throws IOException, ParseException {

        if (locationName == null)
            return new ArrayList<ResolvedLocation>();

        // sanitize the query input
        String sanitizedLocationName = escape(locationName.toLowerCase());

        try {
            // Lucene query used to look for matches based on the
            // "indexName" field
            //Query q = new AnalyzingQueryParser("indexName", indexAnalyzer).parse("\"" + sanitizedLocationName + "\"");
            Query q = new QueryParser("indexName", indexAnalyzer).parse("\"" + sanitizedLocationName + "\"");

            // collect all the hits up to maxHits, and sort them based
            // on Lucene match score and population for the associated
            // GeoNames record
            TopDocs results = indexSearcher.search(q, maxHitDepth, populationSort);

            // initialize the return object
            List<ResolvedLocation> candidateMatches = new ArrayList<ResolvedLocation>();

            // see if anything was found
            if (results.scoreDocs.length > 0) {
                // one or more exact String matches found for this location name
                for (int i = 0; i < results.scoreDocs.length; i++) {
                    // add each matching location to the list of candidates
                    ResolvedLocation location = new ResolvedLocation(indexSearcher.doc(results.scoreDocs[i].doc),
                            new LocationOccurrence(locationName, 0), false);
                    if (getPrefix(location).startsWith(codePrefix)) {
                        logger.debug("{}", location);
                        candidateMatches.add(location);
                    }
                }
            }

            if (fuzzy && (candidateMatches.size() == 0)) { // only if fuzzy matching is turned on
                // no exact String matches found -- fallback to fuzzy search
                //q = new AnalyzingQueryParser( "indexName", indexAnalyzer).parse(sanitizedLocationName + "~");
                q = new QueryParser( "indexName", indexAnalyzer).parse(sanitizedLocationName
                        + "~");
                results = indexSearcher.search(q, maxHitDepth, populationSort);

                // see if anything was found with fuzzy matching
                if (results.scoreDocs.length > 0) {
                    // one or more fuzzy matches found for this location name
                    for (int i = 0; i < results.scoreDocs.length; i++) {
                        // add each matching location to the list of candidates
                        ResolvedLocation location = new ResolvedLocation(indexSearcher.doc(results.scoreDocs[i].doc),
                                new LocationOccurrence(locationName, 0), true);
                        if (getPrefix(location).startsWith(codePrefix)) {
                            logger.debug(location + "{fuzzy}");
                            candidateMatches.add(location);
                        }
                    }
                }
            }

            if (candidateMatches.size() == 0) {
                // no matches found
                logger.debug("No match found for: '{}'", locationName);
            }

            return candidateMatches;
        } catch (ParseException e) {
            logger.error(String.format("Error resolving location for : '%s'", locationName), e);
            throw e;
        } catch (IOException e) {
            logger.error(String.format("Error resolving location for : '%s'", locationName), e);
            throw e;
        }
    }

    private String getPrefix(ResolvedLocation location) {
        return location.getGeoname().getFeatureClass().toString() + "."
                + location.getGeoname().getFeatureCode().toString();
    }

    /**
     * Resolves a multipart location name, such as what's often found
     * in structured data like a spreadsheet or database table (e.g.,
     * [Reston][Virginia][United States]), into a {@link ResolvedMultipartLocation}
     * containing {@link com.bericotech.clavin.gazetteer.GeoName} objects.
     *
     * @param location      multipart location name to be resolved
     * @param fuzzy         switch for turning on/off fuzzy matching
     * @return              resolved multipart location name
     * @throws Exception
     */
    public ResolvedMultipartLocation resolveMultipartLocation(MultipartLocationName location, boolean fuzzy)
            throws Exception {

        // start by finding all valid component locations in the gazetteer
        List<ResolvedLocation> countries = partResolver(location.getCountry(), "A.PC", fuzzy);
        List<ResolvedLocation> states = partResolver(location.getState(), "A.A", fuzzy);
        List<ResolvedLocation> cities = partResolver(location.getCity(), "P", fuzzy);

        // get all valid country codes for this multipart location
        HashSet<CountryCode> countryCodes = new HashSet<CountryCode>();
        for (ResolvedLocation country : countries)
            countryCodes.add(country.getGeoname().getPrimaryCountryCode());

        // filter out any states/provinces/etc. that don't match the valid country codes
        List<ResolvedLocation> filteredStates = new ArrayList<ResolvedLocation>();
        HashSet<String> admin1Codes = new HashSet<String>();
        // keep track of which countries we've already found admin1Codes for,
        // in order to avoid issue of county names in one state matching the
        // names of other states (e.g., Virginia County in Missouri)
        HashSet<CountryCode> stateCountries = new HashSet<CountryCode>();
        for (ResolvedLocation state : states)
            // if   this state/province/etc. is in a valid country
            // and  either it's a first-order administrative division
            //      or we've seen nothing so far in this country...
            if ((countryCodes.contains(state.getGeoname().getPrimaryCountryCode()) || countryCodes.isEmpty())
                    && (state.getGeoname().getAdmin1Code().startsWith("ADM1")
                    || !stateCountries.contains(state.getGeoname().getPrimaryCountryCode()))) {
                filteredStates.add(state);
                admin1Codes.add(state.getGeoname().getAdmin1Code());
                stateCountries.add(state.getGeoname().getPrimaryCountryCode());
            }

        // filter out any cities that don't match the valid admin1 & country codes
        List<ResolvedLocation> filteredCities = new ArrayList<ResolvedLocation>();
        for (ResolvedLocation city : cities)
            if ((countryCodes.contains(city.getGeoname().getPrimaryCountryCode()) || countryCodes.isEmpty())
                    && (admin1Codes.contains(city.getGeoname().getAdmin1Code()) || admin1Codes.isEmpty()))
                filteredCities.add(city);

        // initialize return objects components
        ResolvedLocation finalCity = null;
        ResolvedLocation finalState = null;
        ResolvedLocation finalCountry = null;
        String finalAdmin1Code = null;
        CountryCode finalCountryCode = null;

        // assume the most populous valid city is the correct one return
        // note: this should be a reasonably safe assumption since we've attempted to enforce the
        // notional hierarchy of given place names (e.g., city --> state/province/etc. --> country)
        // and have therefore weeded out all other matches that don't fit this hierarchy
        if (!filteredCities.isEmpty()) {
            finalCity = filteredCities.get(0);
            finalAdmin1Code = finalCity.getGeoname().getAdmin1Code();
            finalCountryCode = finalCity.getGeoname().getPrimaryCountryCode();
        }

        if (!filteredStates.isEmpty()) {
            // if we couldn't find a valid city, just take the most populous valid state/province/etc.
            if ((finalCity == null) || (finalAdmin1Code == null) || (finalCountryCode == null)) {
                finalState = filteredStates.get(0);
                finalCountryCode = finalState.getGeoname().getPrimaryCountryCode();
            } else {
                // get the state/province/etc. that matches the selected city
                for (ResolvedLocation state : filteredStates)
                    if (state.getGeoname().getAdmin1Code().equalsIgnoreCase(finalAdmin1Code)
                            && state.getGeoname().getPrimaryCountryCode().equals(finalCountryCode)) {
                        finalState = state;
                        break;
                    }
            }
        }

        if (!countries.isEmpty()) {
            // if we couldn't find a valid city or state, just take the most populous valid country
            if (finalCountryCode == null) {
                finalCountry = countries.get(0);
            } else {
                // get the country that matches the selected city or state/province/etc.
                for (ResolvedLocation country : countries)
                    if (country.getGeoname().getPrimaryCountryCode().equals(finalCountryCode)) {
                        finalCountry = country;
                        break;
                    }
            }
        }

        return new ResolvedMultipartLocation(finalCity, finalState, finalCountry);
    }

    /**
     * TODO:
     * - even more testing (including for container classes, territories, etc.)
     * - clean, comment, commit
     */
}
