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
package eu.maveniverse.maven.mason.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class FlexibleJsonFactory extends JsonFactory {

    public FlexibleJsonFactory() {
        super(new JsonFactoryBuilder()
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS));
    }

    @Override
    protected JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        InputStream wrapped = new InputStream() {
            private boolean prefixRead = false;
            private boolean suffixNeeded = true;

            @Override
            public int read() throws IOException {
                if (!prefixRead) {
                    prefixRead = true;
                    return '{';
                }

                int b = in.read();
                if (b < 0 && suffixNeeded) {
                    suffixNeeded = false;
                    return '}';
                }
                return b;
            }

            @Override
            public void close() throws IOException {
                in.close();
            }
        };
        JsonParser delegate = super._createParser(wrapped, ctxt);
        return new Delegate(delegate);
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        Reader wrapped = new Reader() {
            private boolean prefixRead = false;
            private boolean suffixNeeded = true;

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                if (!prefixRead) {
                    prefixRead = true;
                    cbuf[off] = '{';
                    int read = r.read(cbuf, off + 1, len - 1);
                    return read < 0 ? 1 : read + 1;
                }

                int read = r.read(cbuf, off, len);
                if (read < 0 && suffixNeeded) {
                    suffixNeeded = false;
                    cbuf[off] = '}';
                    return 1;
                }
                return read;
            }

            @Override
            public void close() throws IOException {
                r.close();
            }
        };
        JsonParser delegate = super._createParser(wrapped, ctxt);
        return new Delegate(delegate);
    }

    private static class Delegate extends JsonParserDelegate {
        public Delegate(JsonParser delegate) {
            super(delegate);
        }

        @Override
        public JsonLocation currentLocation() {
            return offset(super.currentLocation());
        }

        @Override
        public JsonLocation currentTokenLocation() {
            return offset(super.currentLocation());
        }

        private static JsonLocation offset(JsonLocation location) {
            return new JsonLocation(
                    location.contentReference(),
                    location.getByteOffset() - 1,
                    location.getCharOffset() - 1,
                    location.getLineNr(),
                    location.getColumnNr() - (location.getLineNr() == 0 ? 1 : 0));
        }

        @Override
        public JsonLocation getTokenLocation() {
            return this.currentTokenLocation();
        }

        @Override
        public JsonLocation getCurrentLocation() {
            return this.currentLocation();
        }
    }
}
