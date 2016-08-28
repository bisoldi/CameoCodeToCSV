/**
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */
package com.intellnet.cameotocsv;

import com.google.common.base.Splitter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class Converter {

    private static final Logger LOG = Logger.getLogger(Converter.class);
    
    /**
     * Pattern to match the CAMEO Code.  Allows for an unlimited number of digits
     * however the CAMEO spec indicates a max of 4 digits.
     */
    private static final Pattern CODE_MATCHER = Pattern.compile("([0-9]{1,})",
            Pattern.UNICODE_CASE | Pattern.CANON_EQ | Pattern.CASE_INSENSITIVE);

    /**
     * Pattern to match the description of the CAMEO code.
     */
    private static final Pattern CODE_DESCRIPTION_MATCHER = Pattern.compile("([a-z,A-Z].*)");

    /**
     * Format for the CSVPrinter.  Sets the column headers and quotes everything.
     */
    private static final CSVFormat CSV_FILE_FORMAT = CSVFormat.DEFAULT
            .withHeader("tier1code", "tier2code", "tier3code", "description")
            .withQuoteMode(QuoteMode.ALL)
            .withTrim();

    public Converter() {
        //You can put the cameocode txt file in the resources directory before compiling
        InputStream input = this.getClass().getClassLoader().getResourceAsStream("cameocodes.txt");

        if (input == null) {
            try {
                //If you didn't download the CAMEO code file, download it and use it
                URL url = new URL("http://gdeltproject.org/data/lookups/CAMEO.eventcodes.txt");
                input = url.openStream();
            } catch (MalformedURLException ex) {
                LOG.error(ex);
            } catch (IOException ex) {
                LOG.error(ex);
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                CSVPrinter csvPrinter = new CSVPrinter(System.out, CSV_FILE_FORMAT)) {

            String json = null;

            while ((json = reader.readLine()) != null) {
                Matcher codeMatcher = CODE_MATCHER.matcher(json);
                Matcher descMatcher = CODE_DESCRIPTION_MATCHER.matcher(json);

                if (codeMatcher.find() && descMatcher.find()) {
                    //Only process lines that match the two Patterns
                    List<String> splitCode = splitCode(codeMatcher.group());

                    for (String code : splitCode) {
                        csvPrinter.print(code);
                    }
                    
                    for (int i = splitCode.size(); i < 3; i++) {
                        csvPrinter.print(null);
                    }

                    //For consistency, convert everything to lower case.  It can 
                    //always be changed later.
                    String description = StringUtils.lowerCase(descMatcher.group());

                    csvPrinter.print(description);
                    csvPrinter.println();
                }
            }

            csvPrinter.flush();
        } catch (IOException ex) {
            LOG.error(ex);
        }
    }

    /**
     * Parses the CAMEO code into its coded hierarchy.  Assumes it's provided only 
     * the CAMEO code.
     */
    private List<String> splitCode(String code) {
        //Split character by character (number by number)
        List<String> split = Splitter.fixedLength(1).splitToList(code);
        List<String> codes = new ArrayList<>();

        if (split.size() >= 2) {
            //Add the top-level code (always 2 digits)
            codes.add(split.get(0) + split.get(1));

            //If there are any subsequent tier codes, add them 1 by 1
            for (int i = 2; i < split.size(); i++) {
                codes.add(split.get(i));
            }
        }

        return codes;
    }

    public static void main(String[] args) {
        new Converter();
    }
}
