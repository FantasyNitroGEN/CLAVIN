/*
 * Copyright 2014 Berico Technologies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bericotech.clavin.gazetteer;

import static org.junit.Assert.*;

import com.bericotech.clavin.ClavinException;
import com.bericotech.clavin.extractor.LocationOccurrence;
import com.bericotech.clavin.resolver.ResolvedLocation;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Ensures non-heuristic matching and fuzzy matching features are working properly
 * in {@link LuceneGazetteer}.
 */
public class LuceneGazetteerTest {
    private static final File INDEX_DIRECTORY = new File("./IndexDirectory");

    LuceneGazetteer instance;
    List<ResolvedLocation> resolvedLocations;

    // expected geonameID numbers for given location names
    int BOSTON_MA = 4930956;
    int RESTON_VA = 4781530;
    int STRAßENHAUS_DE = 2826158;
    int GUN_BARREL_CITY_TX = 4695535;
    int USSR = 8354411;

    //this convenience method turns an array of location name strings into a list of occurrences with fake positions.
    //(useful for tests that don't care about position in the document)
    public static List<LocationOccurrence> makeOccurrencesFromNames(String[] locationNames) {
        List<LocationOccurrence> locations = new ArrayList<LocationOccurrence>(locationNames.length);
        for(int i = 0; i < locationNames.length; ++i ) {
            locations.add(new LocationOccurrence(locationNames[i], i));
        }
        return locations;
    }

    @Before
    public void setUp() throws ClavinException {
        instance = new LuceneGazetteer(INDEX_DIRECTORY);
    }
    /**
     * Ensure {@link LuceneGazetteer#getClosestLocations(List, boolean)} isn't
     * choking on input.
     */
    @Test
    public void testResolveLocations() throws ClavinException {
        Object[][] testCases = new Object[][] {
            new Object[] { "Reston", RESTON_VA, "Gazetteer failed exact String match" },
            new Object[] { "reston", RESTON_VA, "Gazetteer failed on all lowercase" },
            new Object[] { "RESTON", RESTON_VA, "Gazetteer failed on all uppercase" },
            new Object[] { "Рестон", RESTON_VA, "Gazetteer failed on alternate name" },
            new Object[] { "Straßenhaus", STRAßENHAUS_DE, "Gazetteer failed on UTF8 chars" }
        };
        for (Object[] test : testCases) {
            // match a single location without fuzzy matching
            List<ResolvedLocation> locs = instance.getClosestLocations(new LocationOccurrence((String)test[0], 0), 1, false);
            assertNotNull("Null results list received from Gazetteer", locs);
            assertEquals("Expected single result from Gazetteer", 1, locs.size());
            assertEquals((String)test[2], test[1], locs.get(0).getGeoname().getGeonameID());
        }
    }

    /**
     * Test fuzzy matching.
     */
    @Test
    public void testResolveLocations_Fuzzy() throws ClavinException {
        Object[][] testCases = new Object[][] {
            new Object[] { "Bostonn", BOSTON_MA, "Gazetteer failed on extra char" },
            new Object[] { "Reston12", RESTON_VA, "Gazetteer failed on extra chars" },
            new Object[] { "Bostn", BOSTON_MA, "Gazetteer failed on missing char" },
            new Object[] { "Straßenha", STRAßENHAUS_DE, "Gazetteer failed on missing chars" },
            new Object[] { "Straßenhaus Airport", STRAßENHAUS_DE, "Gazetteer failed on extra term" },
            new Object[] { "Gun Barrel", GUN_BARREL_CITY_TX, "Gazetteer failed on missing term" }
        };
        for (Object[] test : testCases) {
            // match a single location with fuzzy matching
            List<ResolvedLocation> locs = instance.getClosestLocations(new LocationOccurrence((String)test[0], 0), 1, true);
            assertNotNull("Null results list received from Gazetteer", locs);
            assertEquals("Expected single result from Gazetteer", 1, locs.size());
            assertEquals((String)test[2], test[1], locs.get(0).getGeoname().getGeonameID());
        }
    }

    @Test
    public void testResolveLocations_EmptyInput() throws ClavinException {
        List<ResolvedLocation> locs = instance.getClosestLocations(new LocationOccurrence("", 0), 1, true);
        assertEquals("Expected empty results list for empty input.", Collections.EMPTY_LIST, locs);
    }

    @Test
    public void testResolveLocations_WhitespaceInput() throws ClavinException {
        List<ResolvedLocation> locs = instance.getClosestLocations(new LocationOccurrence("\n \t\t \n", 0), 1, true);
        assertEquals("Expected empty results list for whitespace input.", Collections.EMPTY_LIST, locs);
    }

    @Test
    public void testResolveLocations_NullInput() throws ClavinException {
        List<ResolvedLocation> locs = instance.getClosestLocations(new LocationOccurrence(null, 0), 1, true);
        assertEquals("Expected empty results list for null location name.", Collections.EMPTY_LIST, locs);

        locs = instance.getClosestLocations(null, 1, true);
        assertEquals("Expected empty results list for null occurrence.", Collections.EMPTY_LIST, locs);
    }

    /**
     * Ensures Lucene isn't choking on reserved words or unescaped
     * characters.
     */
    @Test
    public void testSanitizedInput() {
        String[] locations = {"OR", "IN", "A + B", "A+B", "A +B", "A+ B", "A OR B", "A IN B", "A / B", "A \\ B",
                "Dallas/Fort Worth Airport", "New Delhi/Chennai", "Falkland ] Islands", "Baima ] County",
                "MUSES \" City Hospital", "North \" Carolina State"};
        for (String loc : locations) {
            try {
                instance.getClosestLocations(new LocationOccurrence(loc, 0), 1, true);
            } catch (ClavinException e) {
                fail(String.format("Input sanitization failed for string '%s': %s", loc, e.getMessage()));
            }
        }
    }

    /**
     * Tests some border cases involving the resolver.
     */
    @Test
    public void testBorderCases() throws ClavinException {
        // ensure we get no matches for this crazy String
        LocationOccurrence loc = new LocationOccurrence("jhadghaoidhg", 0);
        assertTrue("Gazetteer fuzzy off, no match", instance.getClosestLocations(loc, 1, false).isEmpty());
        assertTrue("Gazetteer fuzzy on, no match", instance.getClosestLocations(loc, 1, true).isEmpty());
    }

    /**
     * Ensure exception is thrown when trying to read non-existent index.
     */
    @Test(expected=ClavinException.class)
    public void testNonExistentIndex() throws ClavinException {
        new LuceneGazetteer(new File("./IMAGINARY_FILE"));
    }

    /**
     * Ensure correct GeoName is returned when searched for by ID.
     */
    @Test
    public void testGetGeoName() throws ClavinException {
        Object[][] testCases = new Object[][] {
            new Object[] { RESTON_VA, "Reston, VA" },
            new Object[] { BOSTON_MA, "Boston, MA" },
            new Object[] { STRAßENHAUS_DE, "Straßenhaus, DE" },
            new Object[] { GUN_BARREL_CITY_TX, "Gun Barrell City, TX" }
        };
        for (Object[] test : testCases) {
            GeoName geoname = instance.getGeoName((Integer)test[0]);
            assertNotNull(String.format("Unexpected null returned by Gazetteer for '%s'", test[1]), geoname);
            assertEquals(String.format("Expected GeoName ID [%d] for '%s'", test[0], test[1]), test[0], geoname.getGeonameID());
        }
    }

    /**
     * Ensure null GeoName is returned when ID is not found.
     */
    @Test
    public void testGetNullGeoName() throws ClavinException {
        assertNull("Expected null GeoName for unknown ID [-1]", instance.getGeoName(-1));
    }

    /**
     * Ensure historical records are not matched by getClosestActiveLocations.
     */
    @Test
    public void testFindHistoricalLocations() throws ClavinException {
        LocationOccurrence sovietUnion = new LocationOccurrence("Soviet Union", 0);
        List<ResolvedLocation> withHistorical = instance.getClosestLocations(sovietUnion, 10, true);
        List<ResolvedLocation> activeOnly = instance.getClosestActiveLocations(sovietUnion, 10, true);

        // verify that historical Soviet Union is found when searching all locations
        assertEquals("expected only one result for Soviet Union with historical", 1, withHistorical.size());
        assertEquals("unexpected ID for Soviet Union", USSR, withHistorical.get(0).getGeoname().getGeonameID());

        // verify that historical Soviet Union is not included in active only results
        for (ResolvedLocation loc : activeOnly) {
            assertNotEquals("Soviet Union should not be in active only results", USSR, loc.getGeoname().getGeonameID());
        }
    }
}
