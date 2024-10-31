/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 4290801 4942982 5102005 8008577 8021121 8210153 8227313
 * @summary Basic tests for currency formatting.
 *          Tests both COMPAT and CLDR data.
 * @modules jdk.localedata
 * @run junit/othervm -Djava.locale.providers=COMPAT CurrencyFormat
 * @run junit/othervm -Djava.locale.providers=CLDR CurrencyFormat
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyFormat {

    // Expected data is switched depending on COMPAT or CLDR
    // currencySymbolsTest() is only ran for COMPAT
    private static final boolean isCompat =
            "COMPAT".equals(System.getProperty("java.locale.providers"));

    // Tests the formatting of data for COMPAT + CLDR under various currencies
    // Using a NumberFormat generated by getCurrencyInstance()
    @ParameterizedTest
    @MethodSource("currencyFormatDataProvider")
    public void currencyFormatTest(String expected, Currency currency,
                                   NumberFormat format, Locale locale) {
        if (currency != null) {
            format.setCurrency(currency);
            int digits = currency.getDefaultFractionDigits();
            format.setMinimumFractionDigits(digits);
            format.setMaximumFractionDigits(digits);
        }
        String result = format.format(1234.56);
        assertEquals(expected, result, String.format("Failed with locale: %s%s",
                locale, (currency == null ? ", default currency" : (", currency: " + currency))));
    }

    // Generate a combination of expected data for 1234.56 formatted
    // under various currencies/locale provider/locale
    private static Stream<Arguments> currencyFormatDataProvider() {
        ArrayList<Arguments> data = new ArrayList<Arguments>();
        Locale[] locales = {
                Locale.US,
                Locale.JAPAN,
                Locale.GERMANY,
                Locale.ITALY,
                new Locale("it", "IT", "EURO"),
                Locale.forLanguageTag("de-AT"),
                Locale.forLanguageTag("fr-CH"),
        };
        Currency[] currencies = {
                null,
                Currency.getInstance("USD"),
                Currency.getInstance("JPY"),
                Currency.getInstance("DEM"),
                Currency.getInstance("EUR"),
        };
        String[][] expectedCOMPATData = {
                {"$1,234.56", "$1,234.56", "JPY1,235", "DEM1,234.56", "EUR1,234.56"},
                {"\uFFE51,235", "USD1,234.56", "\uFFE51,235", "DEM1,234.56", "EUR1,234.56"},
                {"1.234,56 \u20AC", "1.234,56 USD", "1.235 JPY", "1.234,56 DM", "1.234,56 \u20AC"},
                {"\u20AC 1.234,56", "USD 1.234,56", "JPY 1.235", "DEM 1.234,56", "\u20AC 1.234,56"},
                {"\u20AC 1.234,56", "USD 1.234,56", "JPY 1.235", "DEM 1.234,56", "\u20AC 1.234,56"},
                {"\u20AC 1.234,56", "USD 1.234,56", "JPY 1.235", "DEM 1.234,56", "\u20AC 1.234,56"},
                {"SFr. 1'234.56", "USD 1'234.56", "JPY 1'235", "DEM 1'234.56", "EUR 1'234.56"},
        };
        String[][] expectedCLDRData = {
                {"$1,234.56", "$1,234.56", "\u00a51,235", "DEM1,234.56", "\u20ac1,234.56"},
                {"\uFFE51,235", "$1,234.56", "\uFFE51,235", "DEM1,234.56", "\u20ac1,234.56"},
                {"1.234,56\u00a0\u20ac", "1.234,56\u00a0$", "1.235\u00a0\u00a5", "1.234,56\u00a0DM", "1.234,56\u00a0\u20ac"},
                {"1.234,56\u00a0\u20ac", "1.234,56\u00a0USD", "1.235\u00a0JPY", "1.234,56\u00a0DEM", "1.234,56\u00a0\u20ac"},
                {"1.234,56\u00a0\u20ac", "1.234,56\u00a0USD", "1.235\u00a0JPY", "1.234,56\u00a0DEM", "1.234,56\u00a0\u20ac"},
                {"\u20ac\u00a01.234,56", "$\u00a01.234,56", "\u00a5\u00a01.235", "DM\u00a01.234,56", "\u20ac\u00a01.234,56"},
                {"1\u202f234.56\u00a0CHF", "1\u202f234.56\u00a0$US", "1\u202f235\u00a0JPY", "1\u202f234.56\u00a0DEM", "1\u202f234.56\u00a0\u20ac"},
        };
        for (int i = 0; i < locales.length; i++) {
            Locale locale = locales[i];
            NumberFormat format = NumberFormat.getCurrencyInstance(locale);
            for (int j = 0; j < currencies.length; j++) {
                Currency currency = currencies[j];
                String expected = isCompat ? expectedCOMPATData[i][j] : expectedCLDRData[i][j];
                data.add(Arguments.of(expected, currency, format, locale));
            }
        }
        return data.stream();
    }

    // Compares the expected currency symbol of a locale to the value returned by
    // DecimalFormatSymbols.getCurrencySymbol().
    @ParameterizedTest
    @MethodSource("currencySymbolsDataProvider")
    public void currencySymbolsTest(String expected, Locale locale) throws ParseException {
        if (!isCompat) {
            return; // For COMPAT only.
        }
        if (expected == null) {
            System.out.println("Warning: No expected currency symbol defined for locale " + locale);
        } else {
            // Reserved for when a currency will change its symbol at a given time in the future
            if (expected.contains(";")) {
                expected = getFutureSymbol(expected);
            }
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
            String result = symbols.getCurrencySymbol();
            assertEquals(expected, result, "Wrong currency symbol for locale " +
                        locale + ", expected: " + expected + ", got: " + result);
        }
    }

    // Grabs the custom CurrencySymbols.properties and loads the file into a Properties
    // instance. Building the data set, which consists of the currency symbol for the locale.
    private static Stream<Arguments> currencySymbolsDataProvider() throws IOException {
        ArrayList<Arguments> data = new ArrayList<Arguments>();
        FileInputStream stream = new FileInputStream(new File(
                System.getProperty("test.src", "."), "CurrencySymbols.properties"));
        Properties props = new Properties();
        props.load(stream);
        Locale[] locales = NumberFormat.getAvailableLocales();
        for (Locale locale : locales) {
            String expected = (String) props.get(locale.toString());
            data.add(Arguments.of(expected, locale));
        }
        return data.stream();
    }

    // Utility to grab the future symbol if in the right format and date cut-over allows
    private static String getFutureSymbol(String expected) throws ParseException {
        StringTokenizer tokens = new StringTokenizer(expected, ";");
        int tokensCount = tokens.countTokens();
        if (tokensCount == 3) {
            expected = tokens.nextToken();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            format.setLenient(false);
            if (format.parse(tokens.nextToken()).getTime() < System.currentTimeMillis()) {
                expected = tokens.nextToken();
            }
        }
        return expected;
    }
}
