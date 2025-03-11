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
package eu.maveniverse.maven.mason;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import eu.maveniverse.maven.mason.hocon.HoconFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;

public class MasonParser implements ModelParser {

    @Override
    public Optional<Source> locate(Path dir) {
        for (String ext : new String[] {".json", ".yaml", ".yml", ".toml", ".hocon", ".conf"}) {
            Path path = dir.resolve("pom" + ext);
            if (path.toFile().exists()) {
                return Optional.of(Sources.fromPath(path));
            }
        }
        return Optional.empty();
    }

    private JsonFactory createFactory(Source source) {
        if (source.getPath() != null) {
            String path = source.getPath().toString().toLowerCase();
            if (path.endsWith(".json")) {
                return new JsonFactory();
            } else if (path.endsWith(".yaml") || path.endsWith(".yml")) {
                return YAMLFactory.builder().build();
            } else if (path.endsWith(".toml")) {
                return TomlFactory.builder().build();
            } else if (path.endsWith(".hocon") || path.endsWith(".conf")) {
                return HoconFactory.builder().withSourcePath(path).build();
            } else {
                throw new ModelParserException("Unsupported file extension: " + path);
            }
        }
        throw new ModelParserException("Only file based sources are supported");
    }

    @Override
    public Model parse(Source source, Map<String, ?> options) throws ModelParserException {
        try {
            JsonFactory factory = createFactory(source);
            MavenJsonReader reader = new MavenJsonReader(factory);
            try (InputStream is = source.openStream()) {
                boolean strict =
                        options.containsKey(ModelParser.STRICT) ? (Boolean) options.get(ModelParser.STRICT) : true;
                InputSource inputSource = new InputSource(
                        source.getLocation(),
                        source.getPath() != null ? source.getPath().toString() : null);
                return reader.read(is, strict, inputSource);
            }
        } catch (IOException e) {
            String location = source.getLocation();
            String path = source.getPath() != null ? source.getPath().toString() : location;
            throw new ModelParserException("Failed to parse " + path + ": " + e.getMessage(), e);
        }
    }
}
