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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class HoconFactory extends JsonFactory {

    private String sourcePath = "unknown";

    public static class Builder extends JsonFactoryBuilder {
        private String sourcePath = "unknown";

        public Builder() {
            super(new HoconFactory());
        }

        public Builder withSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        @Override
        public HoconFactory build() {
            HoconFactory factory = new HoconFactory();
            factory.setSourcePath(sourcePath);
            return factory;
        }
    }

    public HoconFactory() {
        super();
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        Reader reader = new java.io.InputStreamReader(in);
        return _createParser(reader, ctxt);
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        // Create a parser with location tracking enabled that directly implements JsonParser
        return new HoconParser(ctxt, 0, null, r);
    }
}
