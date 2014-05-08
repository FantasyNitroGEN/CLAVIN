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

package com.bericotech.clavin.index;

/**
 * The fields of the Lucene gazetteer index.
 */
public enum IndexField {
    INDEX_NAME("indexName"),
    GEONAME("geoname"),
    GEONAME_ID("geonameID"),
    POPULATION("population"),
    HISTORICAL("historical"),
    FEATURE_CODE("featureCode");

    private final String key;

    private IndexField(final String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /**
     * Gets the integer value representing the provided boolean value in
     * the Lucene index.
     * @param inBool the boolean value
     * @return the numeric value representing the boolean in the index
     */
    public static int getBooleanIndexValue(final boolean inBool) {
        return inBool ? 1 : 0;
    }
}
