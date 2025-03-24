/*******************************************************************************
 * Copyright (c) 2025 Guillaume Nodet
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at:
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.maveniverse.maven.mason.hocon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class HoconParserTest {

    private HoconParser createParser(String input) {
        IOContext ioContext = new IOContext(
                StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(),
                ContentReference.rawReference(input),
                false);
        return new HoconParser(ioContext, 0, null, new StringReader(input));
    }

    private void assertTokenSequence(String input, JsonToken... expectedTokens) throws IOException {
        try (HoconParser parser = createParser(input)) {
            for (JsonToken expected : expectedTokens) {
                JsonToken actual = parser.nextToken();
                assertEquals(expected, actual, "Expected token " + expected + " but got " + actual);
            }
            assertNull(parser.nextToken(), "Expected no more tokens");
        }
    }

    @Test
    void testEmptyObject() throws IOException {
        String input = "{}";
        assertTokenSequence(input, JsonToken.START_OBJECT, JsonToken.END_OBJECT);
    }

    @Test
    void testBasicParsing() throws IOException {
        String input =
                """
            name = "test"
            numbers: [1, 2, 3]
            object {
                key1: "value1"
                key2 = 42
                nested {
                    a: true
                    b: 3.14
                }
            }
            """;

        try (HoconParser parser = createParser(input)) {
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("name", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
            assertEquals("test", parser.getText());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("numbers", parser.getCurrentName());
            assertEquals(JsonToken.START_ARRAY, parser.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals("1", parser.getText());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals("2", parser.getText());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals("3", parser.getText());
            assertEquals(JsonToken.END_ARRAY, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("object", parser.getCurrentName());
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("key1", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
            assertEquals("value1", parser.getText());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("key2", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals("42", parser.getText());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("nested", parser.getCurrentName());
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("a", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_TRUE, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("b", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
            assertEquals("3.14", parser.getText());

            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        }
    }

    @Test
    void testArrays() throws IOException {
        String input =
                """
            empty: []
            mixed: [1, "two", 3.14, true]
            nested: [[1,2], [3,4]]
            """;

        try (HoconParser parser = createParser(input)) {
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("empty", parser.getCurrentName());
            assertEquals(JsonToken.START_ARRAY, parser.nextToken());
            assertEquals(JsonToken.END_ARRAY, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("mixed", parser.getCurrentName());
            assertEquals(JsonToken.START_ARRAY, parser.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
            assertEquals(JsonToken.VALUE_TRUE, parser.nextToken());
            assertEquals(JsonToken.END_ARRAY, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("nested", parser.getCurrentName());
            assertEquals(JsonToken.START_ARRAY, parser.nextToken());
            assertEquals(JsonToken.START_ARRAY, parser.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(JsonToken.END_ARRAY, parser.nextToken());
            assertEquals(JsonToken.START_ARRAY, parser.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(JsonToken.END_ARRAY, parser.nextToken());
            assertEquals(JsonToken.END_ARRAY, parser.nextToken());

            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        }
    }

    @Test
    void testStrings() throws IOException {
        String input =
                """
            doubleQuoted: "hello world"
            unquoted: hello world value
            escaped: "hello\\nworld\\t!"
            spacePrefix:     hello world
            spaceSuffix: hello world
            multipleSpaces: hello    world    value
            """;

        try (HoconParser parser = createParser(input)) {
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());

            // Test each field
            for (String field :
                    new String[] {"doubleQuoted", "unquoted", "escaped", "spacePrefix", "spaceSuffix", "multipleSpaces"
                    }) {
                assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
                assertEquals(field, parser.getCurrentName());
                assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
            }

            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        }
    }

    @Test
    void testUnclosedStructures() {
        assertThrows(HoconParseException.class, () -> {
            try (HoconParser parser = createParser("key = { unclosed")) {
                while (parser.nextToken() != null) {}
            }
        });

        assertThrows(HoconParseException.class, () -> {
            try (HoconParser parser = createParser("key = [ unclosed")) {
                while (parser.nextToken() != null) {}
            }
        });
    }

    @Test
    void testNumbers() throws IOException {
        String input =
                """
            integer: 42
            negative: -42
            decimal: 3.14
            negativeDecimal: -3.14
            version: 4.0.0
            """;

        try (HoconParser parser = createParser(input)) {
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("integer", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals("42", parser.getText());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("negative", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals("-42", parser.getText());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("decimal", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
            assertEquals("3.14", parser.getText());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("negativeDecimal", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
            assertEquals("-3.14", parser.getText());

            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("version", parser.getCurrentName());
            assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
            assertEquals("4.0.0", parser.getText());

            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        }
    }
}
