package com.bericotech.clavin.index;

import static com.bericotech.clavin.index.IndexField.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.bericotech.clavin.gazetteer.CountryCode;
import com.bericotech.clavin.gazetteer.FeatureCode;
import com.bericotech.clavin.gazetteer.GeoName;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
//import org.apache.lucene.document.IntField;
//import org.apache.lucene.document.LongField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * IndexDirectoryBuilder.java
 *
 *###################################################################*/

/**
 * Builds a Lucene index of geographic entries based on
 * the GeoNames gazetteer.
 *
 * This program is run one-time before CLAVIN can be used.
 *
 */
public class AncestryIndexDirectoryBuilder {
    private final static Logger LOG = LoggerFactory.getLogger(AncestryIndexDirectoryBuilder.class);

    // the GeoNames gazetteer file to be loaded
    static String pathToGazetteer = "./allCountries.txt";

    private final Map<String, GeoName> adminMap;
    private final Map<String, Set<GeoName>> unresolvedMap;

    private IndexWriter indexWriter;
    private int indexCount;

    private AncestryIndexDirectoryBuilder() {
        adminMap = new TreeMap<String, GeoName>();
        unresolvedMap = new TreeMap<String, Set<GeoName>>();
    }

    public void buildIndex(File idir) throws IOException {
        LOG.info("Indexing... please wait.");

        indexCount = 0;

        // Create a new index file on disk, allowing Lucene to choose
        // the best FSDirectory implementation given the environment.
        FSDirectory index = FSDirectory.open(idir.toPath());

        // indexing by lower-casing & tokenizing on whitespace
        Analyzer indexAnalyzer = new WhitespaceLowerCaseAnalyzer();

        // create the object that will actually build the Lucene index
        indexWriter = new IndexWriter(index, new IndexWriterConfig(indexAnalyzer));

        // open the gazetteer files to be loaded
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(new File(pathToGazetteer)), "UTF-8"));
//        BufferedReader r2 = new BufferedReader(new InputStreamReader(new FileInputStream(new File("./src/main/resources/SupplementaryGazetteer.txt")), "UTF-8"));

        String line;

        // let's see how long this takes...
        Date start = new Date();

        // load GeoNames gazetteer into Lucene index
        int count = 0;
        while ((line = r.readLine()) != null) {
            try {
                count += 1;
                // print progress update to console
                if (count % 100000 == 0 ) {
                    LOG.info("rowcount: " + count);
                }
                GeoName geoName = GeoName.parseFromGeoNamesRecord(line);
                resolveAncestry(geoName);
            } catch (IOException e) {
                LOG.info("Skipping... Error on line: {}", line);
            } catch (RuntimeException re) {
                LOG.info("Skipping... Error on line: {}", line);
            }
        }

        // add supplementary gazetteer records to index
//        while ((line = r2.readLine()) != null) {
//            addToIndex(line);
//        }

        // that wasn't so long, was it?
        Date stop = new Date();

        LOG.info("[DONE]");
        LOG.info(indexWriter.getDocStats() + " geonames added to index.");
        LOG.info("Merging indices... please wait.");

        LOG.info("Unresolved GeoNames (Pre-resolution)");
        logUnresolved();

        resolveUnresolved();

        LOG.info("Unresolved GeoNames (Post-resolution)");
        logUnresolved();

        LOG.info("Indexing unresolved GeoNames.");
        for (Set<GeoName> geos : unresolvedMap.values()) {
            for (GeoName nm : geos) {
                indexGeoName(nm);
            }
        }

        indexWriter.close();
        index.close();
        r.close();
//        r2.close();

        LOG.info("[DONE]");

        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        long elapsed_MILLIS = stop.getTime() - start.getTime();
        LOG.info("Process started: " + df.format(start) + ", ended: " + df.format(stop)
                + "; elapsed time: " + MILLISECONDS.toSeconds(elapsed_MILLIS) + " seconds.");
    }
    /**
     * Turns a GeoNames gazetteer file into a Lucene index, and adds
     * some supplementary gazetteer records at the end.
     *
     * @param args              not used
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // exit gracefully if the directory exists
        File idir = new File("./IndexDirectory");
        if (idir.exists() ) {
            LOG.info("IndexDirectory exists. Remove the directory and try again.");
            System.exit(-1);
        }
        new AncestryIndexDirectoryBuilder().buildIndex(idir);
    }

    private void resolveAncestry(final GeoName geoname) throws IOException {
        // set this GeoName's parent if it is known
        String parentKey = geoname.getParentAncestryKey();
        if (parentKey != null) {
            // if we cannot successfully set the parent, add to the unresolved map,
            // waiting for a parent to be set
            if (!geoname.setParent(adminMap.get(parentKey)) || !geoname.isAncestryResolved()) {
                Set<GeoName> unresolved = unresolvedMap.get(parentKey);
                if (unresolved == null) {
                    unresolved = new HashSet<GeoName>();
                    unresolvedMap.put(parentKey, unresolved);
                }
                unresolved.add(geoname);
            }
        }
        // if this geoname is fully resolved, add it to the index
        if (geoname.isAncestryResolved()) {
            indexGeoName(geoname);
        }

        // if this is an administrative division, configure the parent of any waiting
        // GeoNames and notify all 2nd level and further descendants their tree has been
        // updated
        String myKey = geoname.getAncestryKey();
        if (myKey != null) {
            GeoName conflict = adminMap.get(myKey);
            if (conflict != null) {
                LOG.error(String.format("Resolved duplicate admin key [%s] for GeoNames (%d %s:%s %s) and (%d %s:%s %s)",
                        myKey, conflict.getGeonameID(), conflict.getFeatureClass(), conflict.getFeatureCode(), conflict.getName(),
                        geoname.getGeonameID(), geoname.getFeatureClass(), geoname.getFeatureCode(), geoname.getName()));
            }
            adminMap.put(myKey, geoname);
            checkDescendantsResolved(geoname, true);
        }
    }

    private void checkDescendantsResolved(final GeoName geoname, final boolean setParent) throws IOException {
        String key = geoname.getAncestryKey();
        if (key != null) {
            Set<GeoName> descendants = unresolvedMap.get(key);
            if (descendants != null) {
                // use an iterator so we can remove elements
                Iterator<GeoName> iter = descendants.iterator();
                while (iter.hasNext()) {
                    GeoName desc = iter.next();
                    if (setParent) {
                        if (!desc.setParent(geoname)) {
                            LOG.error("Error setting parent [{}] of GeoName [{}].", geoname, desc);
                        }
                    }
                    if (desc.isAncestryResolved()) {
                        checkDescendantsResolved(desc, false);
                        indexGeoName(desc);
                        iter.remove();
                    }
                }
                if (descendants.isEmpty()) {
                    unresolvedMap.remove(key);
                }
            }
        }
    }

    private void resolveUnresolved() throws IOException {
        // sort keys in ascending order by level of specificity and name
        Set<String> keys = new TreeSet<String>(new Comparator<String>() {
            @Override
            public int compare(final String strA, final String strB) {
                int specA = strA.split("\\.").length;
                int specB = strB.split("\\.").length;
                return specA != specB ? specA - specB : strA.compareTo(strB);
            }
        });
        keys.addAll(unresolvedMap.keySet());

        // iterate over keys, attempting to resolve less specific keys first; if
        // they are resolved, this may result in more specific keys being resolved
        // as well
        for (String key : keys) {
            String subKey = key;
            GeoName parent = null;
            int lastDot;
            while (parent == null && (lastDot = subKey.lastIndexOf(".")) > 0) {
                subKey = key.substring(0, lastDot);
                parent = adminMap.get(subKey);
            }
            if (parent != null) {
                Set<GeoName> unresolved = unresolvedMap.get(key);
                if (unresolved == null) {
                    // resolving a higher-level key also resolved this key; do nothing
                    break;
                }
                Iterator<GeoName> iter = unresolved.iterator();
                // use iterator so we can remove
                while (iter.hasNext()) {
                    GeoName geoName = iter.next();
                    // first check to see if a previous loop resolved all parents
                    if (geoName.isAncestryResolved()) {
                        indexGeoName(geoName);
                        iter.remove();
                    } else if (geoName.setParent(parent)) {
                        if (geoName.isAncestryResolved()) {
                            // ancestry has been resolved, remove from the unresolved collection
                            indexGeoName(geoName);
                            iter.remove();
                        } else {
                            LOG.error("GeoName [{}] should be fully resolved. (parent: {})", geoName, parent);
                        }
                    } else {
                        LOG.error("Unable to set parent of {} to {}", geoName, parent);
                    }
                }
                if (unresolved.isEmpty()) {
                    unresolvedMap.remove(key);
                }
            } else {
                LOG.error("Unable to resolve parent for GeoName key: {}", key);
            }
        }
    }

    /**
     * Builds a set of Lucene documents for the provided GeoName, indexing
     * each using all available names and storing the entire ancestry path
     * for each GeoName in the index.  See {@link IndexField} for descriptions
     * of the fields indexed for each document.
     *
     * @param geoName the GeoName to index
     * @return the documents to be added to the index
     * @throws IOException if an error occurs while indexing
     */
    private void indexGeoName(final GeoName geoName) throws IOException {
        indexCount++;
        // find all unique names for this GeoName
        String nm = geoName.getName();
        String asciiNm = geoName.getAsciiName();
        Set<String> names = new HashSet<String>();
        names.add(nm);
        names.add(asciiNm);
        names.addAll(geoName.getAlternateNames());
        // if this is a top-level administrative division, add its primary and alternate country codes
        // if they are not already found in the name or alternate names
        if (geoName.isTopLevelAdminDivision()) {
            if (geoName.getPrimaryCountryCode() != null) {
                names.add(geoName.getPrimaryCountryCode().name());
            }
            for (CountryCode cc : geoName.getAlternateCountryCodes()) {
                names.add(cc.name());
            }
        }
        names.remove(null);
        names.remove("");

        // cache index fields that are the same for each document so we don't
        // need to make N method calls
        String gazetteerRecord = geoName.getGazetteerRecordWithAncestry();
        int geoNameID = geoName.getGeonameID();
        long population = geoName.getPopulation();
        FeatureCode code = geoName.getFeatureCode();
        int historical = IndexField.getBooleanIndexValue(code.isHistorical());

        // create a unique Document for each name of this GeoName
        for (String name : names) {
            Document doc = new Document();
            doc.add(new TextField(INDEX_NAME.key(), name, Field.Store.YES));
            doc.add(new StoredField(GEONAME.key(), gazetteerRecord));
            doc.add(new IntPoint(GEONAME_ID.key(), geoNameID));
            doc.add(new StoredField(GEONAME_ID.key(),geoNameID));
            doc.add(new SortedNumericDocValuesField(POPULATION.key(), population));
            doc.add(new StoredField(POPULATION.key(), population));
            doc.add(new IntPoint(HISTORICAL.key(), historical));
            doc.add(new StringField(FEATURE_CODE.key(), code.name(), Field.Store.NO));
            indexWriter.addDocument(doc);
        }
    }

    private void logUnresolved() {
        int unresolvedGeoCount = 0;
        Map<String, Integer> unresolvedCodeMap = new TreeMap<String, Integer>();
        Map<String, Integer> missingCodeMap = new TreeMap<String, Integer>();
        for (Map.Entry<String, Set<GeoName>> entry : unresolvedMap.entrySet()) {
            LOG.trace("{}: {} unresolved GeoNames", entry.getKey(), entry.getValue().size());
            unresolvedGeoCount += entry.getValue().size();
            FeatureCode code;
            switch (entry.getKey().split("\\.").length) {
                case 1:
                    code = FeatureCode.PCL;
                    break;
                case 2:
                    code = FeatureCode.ADM1;
                    break;
                case 3:
                    code = FeatureCode.ADM2;
                    break;
                case 4:
                    code = FeatureCode.ADM3;
                    break;
                case 5:
                    code = FeatureCode.ADM4;
                    break;
                default:
                    LOG.error("Unexpected ancestry key: {}", entry.getKey());
                    code = FeatureCode.NULL;
                    break;
            }
            if (missingCodeMap.containsKey(code.name())) {
                missingCodeMap.put(code.name(), missingCodeMap.get(code.name())+1);
            } else {
                missingCodeMap.put(code.name(), 1);
            }

            for (GeoName geo : entry.getValue()) {
                String featKey = String.format("%s:%s", geo.getFeatureClass(), geo.getFeatureCode());
                if (unresolvedCodeMap.containsKey(featKey)) {
                    unresolvedCodeMap.put(featKey, unresolvedCodeMap.get(featKey)+1);
                } else {
                    unresolvedCodeMap.put(featKey, 1);
                }
            }
        }
        LOG.info("Found {} administrative divisions.", adminMap.size());
        LOG.info("Found {} missing administrative keys.", unresolvedMap.size());
        for (String code : missingCodeMap.keySet()) {
            LOG.info("{}: {}", code, missingCodeMap.get(code));
        }
        LOG.info("{} total unresolved GeoNames", unresolvedGeoCount);
        for (String key : unresolvedCodeMap.keySet()) {
            LOG.trace("{}: {}", key, unresolvedCodeMap.get(key));
        }
    }
}
