/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
