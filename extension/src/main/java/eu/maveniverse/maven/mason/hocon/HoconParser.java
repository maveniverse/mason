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

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

/**
 * HOCON parser that implements Jackson's JsonParser for streaming-based parsing.
 * This allows it to directly emit events with proper location information.
 */
public class HoconParser extends ParserBase {

    protected ObjectCodec codec;
    protected Reader reader;
    protected HoconLexer tokenizer;
    protected HoconToken currentToken;
    protected HoconToken nextToken;
    protected String currentText;
    protected RootState implicitRoot = RootState.UNKNOWN;

    enum RootState {
        UNKNOWN,
        EXPLICIT,
        IMPLICIT_BEFORE,
        IMPLICIT_WITHIN,
        IMPLICIT_DONE
    }

    public HoconParser(IOContext ctxt, int features, ObjectCodec codec, Reader reader) {
        this(ctxt, features, codec, reader, new HoconLexer(reader));
    }

    public HoconParser(IOContext ctxt, int features, ObjectCodec codec, Reader reader, HoconLexer tokenizer) {
        super(ctxt, features);
        this.codec = codec;
        this.reader = reader;
        this.tokenizer = tokenizer;
    }

    @Override
    protected void _closeInput() throws IOException {
        tokenizer.close();
        if (reader != null) {
            reader.close();
        }
    }

    @Override
    public void setCodec(ObjectCodec oc) {
        codec = oc;
    }

    @Override
    public ObjectCodec getCodec() {
        return codec;
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonToken token = doNextToken();
        return token;
    }

    protected JsonToken doNextToken() throws IOException {
        if (implicitRoot == RootState.UNKNOWN) {
            doReadNextToken();
            if (currentToken.type() == HoconToken.TokenType.LEFT_BRACE) {
                implicitRoot = RootState.EXPLICIT;
                return _currToken;
            } else {
                implicitRoot = RootState.IMPLICIT_BEFORE;
                nextToken = currentToken;
                currentToken = new HoconToken(HoconToken.TokenType.LEFT_BRACE, "{", -1, -1);
                return _updateToken(JsonToken.START_OBJECT);
            }
        } else if (implicitRoot == RootState.IMPLICIT_BEFORE) {
            implicitRoot = RootState.IMPLICIT_WITHIN;
            return doReadNextToken();
        } else if (implicitRoot == RootState.IMPLICIT_WITHIN) {
            JsonToken token = doReadNextToken();
            if (token == null) {
                implicitRoot = RootState.IMPLICIT_DONE;
                currentToken = new HoconToken(HoconToken.TokenType.RIGHT_BRACE, "}", -1, -1);
                return _updateToken(JsonToken.END_OBJECT);
            }
            return token;
        } else {
            return doReadNextToken();
        }
    }

    private boolean isNumeric(String value) {
        try {
            // Try parsing as integer first
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            try {
                // Try parsing as double if integer fails
                Double.parseDouble(value);
                return true;
            } catch (NumberFormatException e2) {
                return false;
            }
        }
    }

    private boolean isInteger(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected JsonToken doReadNextToken() throws IOException {
        if (nextToken != null) {
            currentToken = nextToken;
        } else {
            currentToken = tokenizer.yylex();
        }
        _currInputRow = currentToken.line();
        _inputPtr = currentToken.column();
        nextToken = null;

        return _updateToken(
                switch (currentToken.type()) {
                    case LEFT_BRACE -> JsonToken.START_OBJECT;
                    case RIGHT_BRACE -> JsonToken.END_OBJECT;
                    case LEFT_BRACKET -> JsonToken.START_ARRAY;
                    case RIGHT_BRACKET -> JsonToken.END_ARRAY;
                    case UNQUOTED_TEXT, STRING, MULTILINE_STRING, SUBSTITUTION, OPTIONAL_SUBSTITUTION -> bufferValue();
                    case DOT, PATH_TEXT, QUOTED_PATH -> bufferKey();
                    case PLUS -> throw new HoconParseException("Concatenation operator (+) not supported");
                    case INCLUDE -> throw new HoconParseException("include directive not supported");
                    case COMMENT, BLOCK_COMMENT, WHITESPACE, NEWLINE, EQUALS, COLON, COMMA -> doReadNextToken();
                    case EOF -> null;
                });
    }

    private JsonToken bufferKey() throws IOException {
        return buffer(
                JsonToken.FIELD_NAME,
                Set.of(HoconToken.TokenType.DOT, HoconToken.TokenType.PATH_TEXT, HoconToken.TokenType.QUOTED_PATH));
    }

    private JsonToken bufferValue() throws IOException {
        JsonToken token = buffer(
                JsonToken.VALUE_STRING,
                Set.of(
                        HoconToken.TokenType.STRING,
                        HoconToken.TokenType.MULTILINE_STRING,
                        HoconToken.TokenType.UNQUOTED_TEXT,
                        HoconToken.TokenType.WHITESPACE,
                        HoconToken.TokenType.SUBSTITUTION,
                        HoconToken.TokenType.OPTIONAL_SUBSTITUTION));

        // Check if the buffered value is numeric
        if (currentText != null) {
            String value = currentText.trim();
            if (isInteger(value)) {
                _numTypesValid = NR_LONG;
                _numberLong = Long.parseLong(value);
                return _updateToken(JsonToken.VALUE_NUMBER_INT);
            } else if (isNumeric(value)) {
                _numTypesValid = NR_DOUBLE;
                _numberDouble = Double.parseDouble(value);
                return _updateToken(JsonToken.VALUE_NUMBER_FLOAT);
            } else if (value.equalsIgnoreCase("true")) {
                return _updateToken(JsonToken.VALUE_TRUE);
            } else if (value.equalsIgnoreCase("false")) {
                return _updateToken(JsonToken.VALUE_FALSE);
            }
        }
        return token;
    }

    private JsonToken buffer(JsonToken token, Set<HoconToken.TokenType> types) throws IOException {
        StringBuilder buffer = null;
        currentText = text(currentToken);
        while (true) {
            nextToken = tokenizer.yylex();
            if (nextToken == null || !types.contains(nextToken.type())) {
                break;
            }
            currentToken = nextToken;
            if (buffer == null) {
                buffer = new StringBuilder(currentText);
            }
            buffer.append(text(currentToken));
        }
        if (buffer != null) {
            currentText = buffer.toString().trim();
        }
        return _updateToken(token);
    }

    private String text(HoconToken token) {
        return switch (token.type()) {
            case SUBSTITUTION -> "${" + token.value() + "}";
            case OPTIONAL_SUBSTITUTION -> "${?" + token.value() + "}";
            case STRING, QUOTED_PATH -> token.value().substring(1, token.value().length() - 1);
            default -> currentToken.value();
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getCurrentName() throws IOException {
        return currentText;
    }

    @Override
    public String getText() throws IOException {
        return currentText;
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        return currentText != null ? currentText.toCharArray() : null;
    }

    @Override
    public int getTextLength() throws IOException {
        return currentText != null ? currentText.length() : 0;
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }
}
