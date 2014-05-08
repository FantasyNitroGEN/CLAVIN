package com.bericotech.clavin.gazetteer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

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
 * FeatureCodeBuilder.java
 *
 *###################################################################*/

/**
 * Generates {@link FeatureClass} enum definitions from GeoNames
 * featureCodes_en.txt file.
 *
 * TODO: clean this up and make it part of the install/build process
 *
 */
public class FeatureCodeBuilder {
    /**
     * Reads-in featureCodes_en.txt file, spits-out
     * {@link FeatureClass} enum definitions.
     * @param args the command line arguments
     *
     * @throws FileNotFoundException if ./featureCodes_en.txt is not found on the classpath
     * @throws IOException if an error occurs reading the featureCodes file
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        InputStream codeStream = FeatureCodeBuilder.class.getClassLoader().getResourceAsStream("featureCodes_en.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(codeStream, Charset.forName("UTF-8")));
//        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("./featureCodes_en.txt"), Charset.forName("UTF-8")));
        String line;
        while ((line = in.readLine()) != null) {
            String[] tokens = line.split("\t");
            if (tokens[0].equals("null")) {
                System.out.println("NULL(FeatureClass.NULL, \"not available\", \"\", false);");
            } else {
                String[] codes = tokens[0].split("\\.");
                boolean historical = tokens[1].toLowerCase().startsWith("historical") && codes[1].toUpperCase().endsWith("H");
                System.out.printf("%s(FeatureClass.%s, \"%s\", \"%s\", %s),%n", codes[1], codes[0], tokens[1],
                        tokens.length == 3 ? tokens[2] : "", historical);
            }
        }
        in.close();
    }
}
