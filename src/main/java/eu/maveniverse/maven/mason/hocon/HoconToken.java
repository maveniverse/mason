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

public record HoconToken(TokenType type, String value, int line, int column) {
    public enum TokenType {
        LEFT_BRACE, // {
        RIGHT_BRACE, // }
        LEFT_BRACKET, // [
        RIGHT_BRACKET, // ]
        EQUALS, // =
        COLON, // :
        COMMA, // ,
        PLUS, // +
        STRING, // "hello" or 'hello'
        MULTILINE_STRING, // """hello""" (multiline)
        UNQUOTED_TEXT, // unquoted
        PATH_TEXT, // Path element in the key (left side of = or :)
        QUOTED_PATH, // Quoted path element
        DOT, // . (path separator)
        SUBSTITUTION, // ${path}
        OPTIONAL_SUBSTITUTION, // ${?path}
        INCLUDE, // include "file" directive
        WHITESPACE, // whitespace
        NEWLINE, // \n or \r\n
        COMMENT, // Single-line comments (// or #)
        BLOCK_COMMENT, // Multi-line comments (/* */)
        EOF // End of input
    }

    @Override
    public String toString() {
        return "Token{type=" + type + ", value='" + value + "', pos=" + line + ":" + column + "}";
    }
}
