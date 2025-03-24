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
package eu.maveniverse.maven.mason;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.xml.XmlNodeImpl;

/**
 * Helper methods for JSON/YAML parsing in Maven model readers.
 */
public class JsonReaderHelper {

    private JsonReaderHelper() {
        // prevent instantiation
    }

    /**
     * Converts a plural form to singular.
     */
    public static String toSingular(String plural) {
        if (plural.endsWith("ies")) {
            return plural.substring(0, plural.length() - 3) + "y";
        } else if (plural.endsWith("s")) {
            return plural.substring(0, plural.length() - 1);
        }
        return plural;
    }

    /**
     * Known Maven dependency scopes.
     */
    public static final Set<String> KNOWN_SCOPES =
            Set.of("compile-only", "compile", "runtime", "provided", "test-only", "test", "test-runtime", "system");

    /**
     * Parses a GAV (GroupId:ArtifactId:Version) string.
     * @return String array containing [groupId, artifactId, version]
     */
    public static String[] parseGavString(String str, JsonParser parser) throws IOException {
        if (str == null) {
            throw new IOException("GAV string cannot be null at line "
                    + parser.currentLocation().getLineNr() + ", column "
                    + parser.currentLocation().getColumnNr());
        }
        String[] parts = str.split(":");
        if (parts.length < 2 || parts.length > 3) {
            throw new IOException("GAV string must have 2 or 3 parts (groupId:artifactId[:version]), found "
                    + parts.length + " parts in '" + str + "' at line "
                    + parser.currentLocation().getLineNr()
                    + ", column " + parser.currentLocation().getColumnNr());
        }

        String[] result = new String[3]; // [groupId, artifactId, version]
        result[0] = parts[0].isEmpty() ? null : parts[0]; // groupId
        result[1] = parts[1].isEmpty() ? null : parts[1]; // artifactId

        if (result[0] == null || result[1] == null) {
            throw new IOException("GroupId and artifactId cannot be empty in GAV string '" + str
                    + "' at line " + parser.currentLocation().getLineNr()
                    + ", column " + parser.currentLocation().getColumnNr());
        }

        if (parts.length == 3) {
            result[2] = parts[2].isEmpty() ? null : parts[2]; // version
        }

        return result;
    }

    /**
     * Parses a GASVTCO (GroupId:ArtifactId[:Scope][:Version][:Type][:Classifier][?]) string.
     * @return String array containing [groupId, artifactId, scope, version, type, classifier, optional]
     */
    public static String[] parseGasvtcoString(String str, JsonParser parser) throws IOException {
        if (str == null) {
            throw new IOException("GASVTC string cannot be null at line "
                    + parser.currentLocation().getLineNr() + ", column "
                    + parser.currentLocation().getColumnNr());
        }

        // Handle optional marker
        boolean optional = str.endsWith("?");
        if (optional) {
            str = str.substring(0, str.length() - 1);
        }

        // Split scope from main coordinates
        String[] scopeSplit = str.split("@", 2);
        String coords = scopeSplit[0];
        String scope = scopeSplit.length > 1 ? scopeSplit[1] : null;

        if (scope != null && !KNOWN_SCOPES.contains(scope)) {
            throw new IOException("Unknown scope '" + scope + "' in GASVTC string '" + str
                    + "' at line " + parser.currentLocation().getLineNr()
                    + ", column " + parser.currentLocation().getColumnNr());
        }

        String[] parts = coords.split(":");
        if (parts.length < 2 || parts.length > 5) {
            throw new IOException(
                    "GASVTC string must have between 2 and 5 parts (groupId:artifactId[:version][:type][:classifier]), found "
                            + parts.length + " parts in '" + str + "' at line "
                            + parser.currentLocation().getLineNr()
                            + ", column " + parser.currentLocation().getColumnNr());
        }

        String[] result = new String[7]; // [groupId, artifactId, scope, version, type, classifier, optional]

        // Required parts
        result[0] = parts[0].isEmpty() ? null : parts[0]; // groupId
        result[1] = parts[1].isEmpty() ? null : parts[1]; // artifactId

        if (result[0] == null || result[1] == null) {
            throw new IOException("GroupId and artifactId cannot be empty in GASVTC string '" + str
                    + "' at line " + parser.currentLocation().getLineNr()
                    + ", column " + parser.currentLocation().getColumnNr());
        }

        // Optional parts
        if (parts.length > 2) result[3] = parts[2].isEmpty() ? null : parts[2]; // version
        if (parts.length > 3) result[4] = parts[3].isEmpty() ? null : parts[3]; // type
        if (parts.length > 4) result[5] = parts[4].isEmpty() ? null : parts[4]; // classifier

        // Set scope and optional flag
        result[2] = scope; // scope
        result[6] = optional ? "true" : null;

        return result;
    }

    /**
     * Creates an InputLocation if location tracking is enabled.
     */
    public static InputLocation createLocation(
            JsonParser parser, InputSource inputSrc, boolean addLocationInformation) {
        return addLocationInformation
                ? new InputLocation(
                        parser.currentLocation().getLineNr(),
                        parser.currentLocation().getColumnNr(),
                        inputSrc)
                : null;
    }

    /**
     * Gets a boolean value from a string with a default value.
     */
    public static boolean getBooleanValue(String s, String attribute, JsonParser parser, boolean defaultValue)
            throws IOException {
        if (s != null && !s.isEmpty()) {
            return Boolean.parseBoolean(s);
        }
        return defaultValue;
    }

    /**
     * Gets an integer value from a string with a default value.
     */
    public static int getIntegerValue(String s, String attribute, JsonParser parser, boolean strict, int defaultValue)
            throws IOException {
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                if (strict) {
                    throw new IOException("Unable to parse element '" + attribute + "', must be an integer");
                }
            }
        }
        return defaultValue;
    }

    /**
     * Builds an XmlNode from a JSON parser stream.
     */
    public static XmlNode buildXmlNode(JsonParser parser) throws IOException {
        return buildXmlNode(parser, null, false);
    }

    /**
     * Builds an XmlNode from a JSON parser stream.
     */
    public static XmlNode buildXmlNode(JsonParser parser, InputSource inputSrc, boolean addLocationInformation)
            throws IOException {
        String name = parser.currentName();
        if (name == null) {
            throw new IOException("Node name cannot be null at line "
                    + parser.currentLocation().getLineNr() + ", column "
                    + parser.currentLocation().getColumnNr());
        }

        String value = null;
        Map<String, String> attributes = new LinkedHashMap<>();
        List<XmlNode> children = new ArrayList<>();
        InputLocation location = addLocationInformation
                ? new InputLocation(
                        parser.currentLocation().getLineNr(),
                        parser.currentLocation().getColumnNr(),
                        inputSrc)
                : null;

        JsonToken token = parser.nextToken();
        if (token == JsonToken.VALUE_NULL) {
            return new XmlNodeImpl(name, null, attributes, children, location);
        }

        while (token != JsonToken.END_OBJECT && token != null) {
            if (token == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                token = parser.nextToken();

                if (token == JsonToken.START_OBJECT) {
                    children.add(buildXmlNode(parser, inputSrc, addLocationInformation));
                } else if (token == JsonToken.START_ARRAY) {
                    List<XmlNode> arrayChildren = new ArrayList<>();
                    String singularName = toSingular(fieldName);

                    while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
                        if (token == JsonToken.START_OBJECT) {
                            // Look ahead to see if this is a wrapper object
                            token = parser.nextToken();
                            if (token == JsonToken.FIELD_NAME) {
                                String firstField = parser.currentName();
                                if (firstField.equals(singularName)) {
                                    // Skip the wrapper field, use its contents directly
                                    token = parser.nextToken(); // Move to the start of inner object
                                    if (token == JsonToken.START_OBJECT) {
                                        List<XmlNode> objectChildren = new ArrayList<>();
                                        while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
                                            if (token == JsonToken.FIELD_NAME) {
                                                String objFieldName = parser.currentName();
                                                token = parser.nextToken();
                                                objectChildren.add(new XmlNodeImpl(
                                                        objFieldName,
                                                        parser.getText(),
                                                        new LinkedHashMap<>(),
                                                        new ArrayList<>(),
                                                        createLocation(parser, inputSrc, addLocationInformation)));
                                            }
                                        }
                                        parser.nextToken(); // Skip the outer object's END_OBJECT
                                        arrayChildren.add(new XmlNodeImpl(
                                                singularName,
                                                null,
                                                new LinkedHashMap<>(),
                                                objectChildren,
                                                createLocation(parser, inputSrc, addLocationInformation)));
                                    }
                                } else {
                                    // Regular object, process its fields
                                    List<XmlNode> objectChildren = new ArrayList<>();
                                    parser.currentName(); // Reset to first field
                                    while (token == JsonToken.FIELD_NAME) {
                                        String objFieldName = parser.currentName();
                                        token = parser.nextToken();
                                        objectChildren.add(new XmlNodeImpl(
                                                objFieldName,
                                                parser.getText(),
                                                new LinkedHashMap<>(),
                                                new ArrayList<>(),
                                                createLocation(parser, inputSrc, addLocationInformation)));
                                        token = parser.nextToken();
                                    }
                                    arrayChildren.add(new XmlNodeImpl(
                                            singularName,
                                            null,
                                            new LinkedHashMap<>(),
                                            objectChildren,
                                            createLocation(parser, inputSrc, addLocationInformation)));
                                }
                            }
                        } else if (token.isScalarValue()) {
                            arrayChildren.add(new XmlNodeImpl(
                                    singularName,
                                    parser.getText(),
                                    new LinkedHashMap<>(),
                                    new ArrayList<>(),
                                    createLocation(parser, inputSrc, addLocationInformation)));
                        }
                    }
                    children.add(new XmlNodeImpl(
                            fieldName,
                            null,
                            new LinkedHashMap<>(),
                            arrayChildren,
                            createLocation(parser, inputSrc, addLocationInformation)));
                } else if (token.isScalarValue()) {
                    if (fieldName.startsWith("@")) {
                        attributes.put(fieldName.substring(1), parser.getText());
                    } else {
                        children.add(new XmlNodeImpl(
                                fieldName,
                                parser.getText(),
                                new LinkedHashMap<>(),
                                new ArrayList<>(),
                                createLocation(parser, inputSrc, addLocationInformation)));
                    }
                }
            } else if (token.isScalarValue()) {
                value = parser.getText();
            }
            token = parser.nextToken();
        }

        return new XmlNodeImpl(name, value, attributes, children, location);
    }
}
