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
