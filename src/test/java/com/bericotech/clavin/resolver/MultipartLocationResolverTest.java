package com.bericotech.clavin.resolver;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
 * LuceneLocationResolverHeuristicsTest.java
 * 
 *###################################################################*/

/**
 * Tests the mapping of location names into
 * {@link com.bericotech.clavin.resolver.ResolvedLocation} objects as performed by
 * {@link com.bericotech.clavin.resolver.LocationResolver#resolveLocations(java.util.List, boolean)}.
 */
public class MultipartLocationResolverTest {

    public final static Logger logger = LoggerFactory.getLogger(MultipartLocationResolverTest.class);

    // objects required for running tests
    File indexDirectory;
    MultipartLocationResolver resolver;
    MultipartLocationName locationNames;
    ResolvedMultipartLocation resolvedLocations;

    // expected geonameID numbers for given location names
    int UNITED_STATES = 6252001;
        int MASSACHUSETTS = 6254926;
            int BOSTON_MA = 4930956;
            int HAVERHILL_MA = 4939085;
            int WORCESTER_MA = 4956184;
            int SPRINGFIELD_MA = 4951788;
        int MISSOURI = 4398678;
            int SPRINGFIELD_MO = 4409896;
        int ILLINOIS = 4896861;
            int SPRINGFIELD_IL = 4250542;
        int VIRGINIA = 6254928;
            int SPRINGFIELD_VA = 4787117;
        int OREGON = 5744337;
            int SPRINGFIELD_OR = 5754005;
        int DELAWARE = 4142224;
            int BETHEL_DE_US = 4141443;
    int GERMANY = 2921044;
        int NR_WESTPHALIA = 2861876; // state of North Rhine-Westphalia
            int BETHEL_GER = 2949766;
    int UNITED_KINGDOM = 2635167;
        int ENGLAND = 6269131;
            int LONDON_UK = 2643741;
            int HAVERHILL_UK = 2647310;
            int WORCESTER_UK = 2633563;
        int OXFORDSHIRE = 2640726;
            int OXFORD_UK = 2640729;
    int CANADA = 6251999;
        int ONTARIO = 6093943;
            int LONDON_ON = 6058560;
    int PHILIPPINES = 1694008;
        int DAVAO = 7521309;
        int DAVAO_ORIENTAL = 1715342;
            int BOSTON_PH = 1723862;
    int SWITZERLAND = 2658434;
        int ZURICH_CANTON = 2657895;
            int ZURICH_CITY = 2657896;

    /**
     * Instantiate a {@link com.bericotech.clavin.resolver.LuceneLocationResolver} objects with
     * context-based heuristic matching turned on.
     *
     * @throws java.io.IOException
     * @throws org.apache.lucene.queryparser.classic.ParseException
     */
    @Before
    public void setUp() throws IOException, ParseException {
        indexDirectory = new File("./IndexDirectory");
        resolver = new MultipartLocationResolver(indexDirectory);
    }

    /**
     * Ensure we select the correct {@link com.bericotech.clavin.resolver.ResolvedLocation} objects
     * without using context-based heuristic matching.
     *
     * Without heuristics, {@link com.bericotech.clavin.resolver.LuceneLocationResolver} will default to
     * mapping location name Strings to the matching
     * {@link com.bericotech.clavin.resolver.ResolvedLocation} object with the greatest population.
     *
     * @throws java.io.IOException
     * @throws org.apache.lucene.queryparser.classic.ParseException
     */
    @Test
    public void testResolveMultipartLocation() throws Exception {

        locationNames = new MultipartLocationName("Springfield", "Massachusetts", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", SPRINGFIELD_MA, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", MASSACHUSETTS, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Springfield", "Illinois", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", SPRINGFIELD_IL, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ILLINOIS, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Springfield", "Missouri", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", SPRINGFIELD_MO, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", MISSOURI, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Springfield", "Virginia", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", SPRINGFIELD_VA, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", VIRGINIA, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Springfield", "Oregon", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", SPRINGFIELD_OR, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", OREGON, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Boston", "Massachusetts", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", BOSTON_MA, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", MASSACHUSETTS, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Haverhill", "Massachusetts", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", HAVERHILL_MA, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", MASSACHUSETTS, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Worcester", "Massachusetts", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", WORCESTER_MA, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", MASSACHUSETTS, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Haverhill", "England", "United Kingdom");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", HAVERHILL_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Worcester", "England", "United Kingdom");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", WORCESTER_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Oxford", "England", "United Kingdom");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", OXFORD_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Oxford", "Oxfordshire", "United Kingdom");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", OXFORD_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", OXFORDSHIRE, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("London", "England", "United Kingdom");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("London", "Ontario", "Canada");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_ON, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ONTARIO, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", CANADA, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Boston", "Davao", "Philippines");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", BOSTON_PH, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", DAVAO, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", PHILIPPINES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Boston", "Davao Oriental", "Philippines");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", BOSTON_PH, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", DAVAO_ORIENTAL, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", PHILIPPINES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Bethel", "Delaware", "United States");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", BETHEL_DE_US, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", DELAWARE, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Bethel", "North Rhine-Westphalia", "Germany");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", BETHEL_GER, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", NR_WESTPHALIA, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", GERMANY, resolvedLocations.getCountry().getGeoname().getGeonameID());
    }

    @Test
    public void testResolveMultipartLocationAbbreviations() throws Exception {
        locationNames = new MultipartLocationName("Bethel", "DE", "US");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", BETHEL_DE_US, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", DELAWARE, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_STATES, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Bethel", "NRW", "DE");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", BETHEL_GER, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", NR_WESTPHALIA, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", GERMANY, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("London", "ENG", "UK");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("London", "ENG", "GB");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("London", "ON", "CA");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_ON, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ONTARIO, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", CANADA, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("London", "ON", "CAN");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_ON, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ONTARIO, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", CANADA, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("Zurich", "ZH", "CH");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", ZURICH_CITY, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ZURICH_CANTON, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", SWITZERLAND, resolvedLocations.getCountry().getGeoname().getGeonameID());
    }

    @Test
    public void testResolveMultipartLocationMissing() throws Exception {
        locationNames = new MultipartLocationName("", "ENG", "UK");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertNull("LocationResolver chose the wrong city", resolvedLocations.getCity());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("London", "", "UK");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertNull("LocationResolver chose the wrong state/province/etc.", resolvedLocations.getState());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("London", "ENG", "");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertNull("LocationResolver chose the wrong country", resolvedLocations.getCountry());

        locationNames = new MultipartLocationName("London", "", "");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertEquals("LocationResolver chose the wrong city", LONDON_UK, resolvedLocations.getCity().getGeoname().getGeonameID());
        assertNull("LocationResolver chose the wrong state/province/etc.", resolvedLocations.getState());
        assertNull("LocationResolver chose the wrong country", resolvedLocations.getCountry());

        locationNames = new MultipartLocationName("", "ENG", "");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertNull("LocationResolver chose the wrong city", resolvedLocations.getCity());
        assertEquals("LocationResolver chose the wrong state/province/etc.", ENGLAND, resolvedLocations.getState().getGeoname().getGeonameID());
        assertNull("LocationResolver chose the wrong country", resolvedLocations.getCountry());

        locationNames = new MultipartLocationName("", "", "UK");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertNull("LocationResolver chose the wrong city", resolvedLocations.getCity());
        assertNull("LocationResolver chose the wrong state/province/etc.", resolvedLocations.getState());
        assertEquals("LocationResolver chose the wrong country", UNITED_KINGDOM, resolvedLocations.getCountry().getGeoname().getGeonameID());

        locationNames = new MultipartLocationName("", "", "");
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertNull("LocationResolver chose the wrong city", resolvedLocations.getCity());
        assertNull("LocationResolver chose the wrong state/province/etc.", resolvedLocations.getState());
        assertNull("LocationResolver chose the wrong country", resolvedLocations.getCountry());

        locationNames = new MultipartLocationName(null, null, null);
        resolvedLocations = resolver.resolveMultipartLocation(locationNames, false);
        assertNull("LocationResolver chose the wrong city", resolvedLocations.getCity());
        assertNull("LocationResolver chose the wrong state/province/etc.", resolvedLocations.getState());
        assertNull("LocationResolver chose the wrong country", resolvedLocations.getCountry());
    }
}
