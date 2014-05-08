/*#####################################################################
 *
 * CLAVIN (Cartographic Location And Vicinity INdexer)
 * ---------------------------------------------------
 *
 * Copyright (C) 2012-2013 Berico Technologies
 * http://clavin.bericotechnologies.com
 *
 * ====================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * ====================================================================
 *
 * LuceneGazetteer.java
 *
 *###################################################################*/
package com.bericotech.clavin.gazetteer;

import static com.bericotech.clavin.index.IndexField.*;
import static org.apache.lucene.queryparser.classic.QueryParserBase.escape;

import com.bericotech.clavin.ClavinException;
import com.bericotech.clavin.extractor.LocationOccurrence;
import com.bericotech.clavin.index.BinarySimilarity;
import com.bericotech.clavin.index.IndexField;
import com.bericotech.clavin.index.WhitespaceLowerCaseAnalyzer;
import com.bericotech.clavin.resolver.LuceneLocationResolver;
import com.bericotech.clavin.resolver.ResolvedLocation;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of Gazetteer that uses Lucene to rapidly search
 * known locations.
 */
public class LuceneGazetteer implements Gazetteer {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LuceneGazetteer.class);

    /**
     * Index employs simple lower-casing & tokenizing on whitespace.
     */
    private static final Analyzer INDEX_ANALYZER = new WhitespaceLowerCaseAnalyzer();

    /**
     * Custom Lucene sorting based on Lucene match score and the
     * population of the GeoNames gazetteer entry represented by the
     * matched index document.
     */
    private static final Sort POPULATION_SORT = new Sort(new SortField[]
            {SortField.FIELD_SCORE, new SortField(POPULATION.key(), SortField.Type.LONG, true)});

    /**
     * The default number of results to return.
     */
    private static final int DEFAULT_MAX_RESULTS = 5;

    // Lucene index built from GeoNames gazetteer
    private final FSDirectory index;
    private final IndexSearcher indexSearcher;

    /**
     * Builds a {@link LuceneLocationResolver} by loading a pre-built Lucene
     * index from disk and setting configuration parameters for
     * resolving location names to GeoName objects.
     *
     * @param indexDir              Lucene index directory to be loaded
     * @throws ClavinException      if an error occurs opening the index
     */
    public LuceneGazetteer(final File indexDir) throws ClavinException {
        try {
        // load the Lucene index directory from disk
        index = FSDirectory.open(indexDir);

        indexSearcher = new IndexSearcher(DirectoryReader.open(index));

        // override default TF/IDF score to ignore multiple appearances
        indexSearcher.setSimilarity(new BinarySimilarity());

        // run an initial throw-away query just to "prime the pump" for
        // the cache, so we can accurately measure performance speed
        // per: http://wiki.apache.org/lucene-java/ImproveSearchingSpeed
        indexSearcher.search(new AnalyzingQueryParser(Version.LUCENE_47, INDEX_NAME.key(),
                INDEX_ANALYZER).parse("Reston"), null, DEFAULT_MAX_RESULTS, POPULATION_SORT);
        } catch (ParseException pe) {
            throw new ClavinException("Error executing priming query.", pe);
        } catch (IOException ioe) {
            throw new ClavinException("Error opening gazetteer index.", ioe);
        }
    }

    /**
     * Finds all matches (capped at maxResults) in the Lucene index for a given location name,
     * searching both current and active locations.
     *
     * @param locationName      name of the geographic location to be resolved
     * @param maxResults        the maximum number of results to return
     * @param fuzzy             switch for turning on/off fuzzy matching
     * @return                  list of ResolvedLocations as potential matches
     * @throws ClavinException  if an error occurs searching the index
     */
    @Override
    public List<ResolvedLocation> getClosestLocations(final LocationOccurrence locationName, final int maxResults,
            final boolean fuzzy) throws ClavinException {
        return internalGetClosestLocations(locationName, maxResults, fuzzy, true);
    }

    /**
     * Finds all matches (capped at maxResults) in the Lucene index for a given location name,
     * restricting the search to only active locations.
     *
     * @param locationName      name of the geographic location to be resolved
     * @param maxResults        the maximum number of results to return
     * @param fuzzy             switch for turning on/off fuzzy matching
     * @return                  list of ResolvedLocations as potential matches
     * @throws ClavinException  if an error occurs searching the index
     */
    @Override
    public List<ResolvedLocation> getClosestActiveLocations(final LocationOccurrence locationName, final int maxResults,
            final boolean fuzzy) throws ClavinException {
        return internalGetClosestLocations(locationName, maxResults, fuzzy, false);
    }

    /**
     * Finds all matches (capped at maxResults) in the Lucene index for a given location name.
     *
     * @param locationName      name of the geographic location to be resolved
     * @param maxResults        the maximum number of results to return
     * @param fuzzy             switch for turning on/off fuzzy matching
     * @param includeHistorical <code>true</code> to include historical locations in the search
     * @return                  list of ResolvedLocations as potential matches
     * @throws ClavinException  if an error occurs searching the index
     */
    @SuppressWarnings("unchecked")
    private List<ResolvedLocation> internalGetClosestLocations(final LocationOccurrence locationName, final int maxResults,
            final boolean fuzzy, final boolean includeHistorical) throws ClavinException {
        // short-circuit if no location name was provided
        String name = locationName != null && locationName.getText() != null ? locationName.getText().trim().toLowerCase() : "";
        if ("".equals(name)) {
            return Collections.EMPTY_LIST;
        }
        // santize the query input
        String sanitizedLocationName = escape(name);

        try {
            // Lucene query used to look for matches based on the
            // "indexName" field
            Query q = buildHistoricalQuery(new AnalyzingQueryParser(Version.LUCENE_47, INDEX_NAME.key(),
                    INDEX_ANALYZER).parse("\"" + sanitizedLocationName + "\""), includeHistorical);

            // collect all the hits up to maxResults, and sort them based
            // on Lucene match score and population for the associated
            // GeoNames record
            TopDocs results = indexSearcher.search(q, null, maxResults, POPULATION_SORT);

            // initialize the return object
            List<ResolvedLocation> candidateMatches = new ArrayList<ResolvedLocation>();

            // see if anything was found
            if (results.scoreDocs.length > 0) {
                // one or more exact String matches found for this location name
                for (ScoreDoc scoreDoc : results.scoreDocs) {
                    // add each matching location to the list of candidates
                    ResolvedLocation location = new ResolvedLocation(indexSearcher.doc(scoreDoc.doc), locationName, false);
                    LOG.debug("{}", location);
                    candidateMatches.add(location);
                }
            } else if (fuzzy) { // only if fuzzy matching is turned on
                // no exact String matches found -- fallback to fuzzy search

                // Using the tilde "~" makes this a fuzzy search. I compared this to FuzzyQuery
                // with TopTermsBoostOnlyBooleanQueryRewrite, I like the output better this way.
                // With the other method, we failed to match things like "Stra��enhaus Airport"
                // as <Stra��enhaus>, and the match scores didn't make as much sense.
                q = buildHistoricalQuery(new AnalyzingQueryParser(Version.LUCENE_47, INDEX_NAME.key(), INDEX_ANALYZER).
                        parse(sanitizedLocationName + "~"), includeHistorical);

                // collect all the fuzzy matches up to maxHits, and sort
                // them based on Lucene match score and population for the
                // associated GeoNames record
                results = indexSearcher.search(q, null, maxResults, POPULATION_SORT);

                // see if anything was found with fuzzy matching
                if (results.scoreDocs.length > 0) {
                    // one or more fuzzy matches found for this location name
                    for (ScoreDoc scoreDoc : results.scoreDocs) {
                        // add each matching location to the list of candidates
                        ResolvedLocation location = new ResolvedLocation(indexSearcher.doc(scoreDoc.doc), locationName, true);
                        LOG.debug("{}[fuzzy]", location);
                        candidateMatches.add(location);
                    }
                } else {
                    // drats, foiled again! no fuzzy matches found either!
                    // in this case, we'll return an empty list of
                    // candidate matches
                    LOG.debug("No match found for: '{}'", locationName.getText());
                }
            } else {
                // no matches found and fuzzy matching is turned off
                LOG.debug("No match found for: '{}'", locationName.getText());
            }

            return candidateMatches;

        } catch (ParseException e) {
            String msg = String.format("Error resolving location for : '%s'", locationName.getText());
            LOG.error(msg, e);
            throw new ClavinException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Error resolving location for : '%s'", locationName.getText());
            LOG.error(msg, e);
            throw new ClavinException(msg, e);
        }
    }

    private Query buildHistoricalQuery(final Query query, final boolean includeHistorical) {
        Query historicalQuery = query;
        // if historical data is included, we don't need to restrict the query at all
        if (!includeHistorical) {
            BooleanQuery bq = new BooleanQuery();
            int notHistorical = IndexField.getBooleanIndexValue(false);
            bq.add(NumericRangeQuery.newIntRange(HISTORICAL.key(), notHistorical, notHistorical, true, true),
                    BooleanClause.Occur.MUST);
            bq.add(query, BooleanClause.Occur.MUST);
            historicalQuery = bq;
        }
        return historicalQuery;
    }

    /**
     * Retrieves the GeoName with the provided ID.
     * @param geonameId          the ID of the requested GeoName
     * @return                   the requested GeoName or <code>null</code> if not found
     * @throws ClavinException   if an error occurs
     */
    @Override
    public GeoName getGeoName(final int geonameId) throws ClavinException {
        try {
            GeoName geoName = null;
            // Lucene query used to look for exact match on the "geonameID" field
            Query q = NumericRangeQuery.newIntRange(GEONAME_ID.key(), geonameId, geonameId, true, true);
            // retrieve only one matching document
            TopDocs results = indexSearcher.search(q, 1);
            if (results.scoreDocs.length > 0) {
                geoName = GeoName.parseFromGeoNamesRecord(indexSearcher.doc(results.scoreDocs[0].doc).get(GEONAME.key()));
            } else {
                LOG.debug("No geoname found for ID: {}", geonameId);
            }
            return geoName;
        } catch (IOException e) {
            String msg = String.format("Error retrieving geoname with ID : %d", geonameId);
            LOG.error(msg, e);
            throw new ClavinException(msg, e);
        }
    }
}
