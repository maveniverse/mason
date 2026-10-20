/*******************************************************************************
 * Copyright (c) 2025 Guillaume Nodet
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution
 * and is available at: https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.maveniverse.maven.mason;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.api.Dialect;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.spi.DialectProvider;
import org.apache.maven.api.spi.ModelDialectProvider;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.apache.maven.api.spi.ModelWriter;

/**
 * Registers Mason's dialects (yaml, toml, json, hocon) as {@link ModelDialectProvider} instances
 * and also contributes the corresponding {@link Dialect} enum values via {@link DialectProvider}.
 *
 * <p>Each inner class handles one format. The {@link MasonParser} is reused for reading;
 * {@link MasonModelWriter} handles writing.
 *
 * @since 0.4.0
 */
public class MasonModelDialectProvider {

    // -------------------------------------------------------------------------
    // Dialect constants — declared here so the whole mason module shares them
    // -------------------------------------------------------------------------

    public static final Dialect YAML = dialectOf("yaml");
    public static final Dialect TOML = dialectOf("toml");
    public static final Dialect JSON = dialectOf("json");
    public static final Dialect HOCON = dialectOf("hocon");

    /**
     * Creates a simple {@link Dialect} implementation for the given id.
     * We cannot call the package-private {@code ExtensibleEnums.dialect()} from here,
     * so we implement the interface directly.
     */
    private static Dialect dialectOf(String id) {
        return new Dialect() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public int hashCode() {
                return id.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Dialect && id.equals(((Dialect) obj).id());
            }

            @Override
            public String toString() {
                return "Dialect[" + id + "]";
            }
        };
    }

    // -------------------------------------------------------------------------
    // DialectProvider — registers the four mason dialects in the registry
    // -------------------------------------------------------------------------

    @Named("mason")
    @Singleton
    public static class MasonDialectProvider implements DialectProvider {
        @Override
        public List<Dialect> provides() {
            return List.of(YAML, TOML, JSON, HOCON);
        }
    }

    // -------------------------------------------------------------------------
    // Shared ModelParser adapter that delegates to MasonParser
    // -------------------------------------------------------------------------

    private static final class SingleFormatParser implements ModelParser {
        private final MasonParser delegate;
        private final String[] extensions;

        SingleFormatParser(MasonParser delegate, String... extensions) {
            this.delegate = delegate;
            this.extensions = extensions;
        }

        @Override
        public Optional<Source> locate(Path dir) {
            for (String ext : extensions) {
                Path pom = dir.resolve("pom" + ext);
                if (pom.toFile().exists()) {
                    return Optional.of(Sources.fromPath(pom));
                }
            }
            return Optional.empty();
        }

        @Override
        public Model parse(Source source, Map<String, ?> options) throws ModelParserException {
            return delegate.parse(source, options);
        }
    }

    // -------------------------------------------------------------------------
    // Concrete ModelDialectProvider per format
    // -------------------------------------------------------------------------

    @Named("mason-yaml")
    @Singleton
    public static class YamlDialectProvider implements ModelDialectProvider {

        private final ModelParser parser;
        private final ModelWriter writer;

        @Inject
        public YamlDialectProvider(MasonParser masonParser) {
            JsonFactory yamlFactory = YAMLFactory.builder().build();
            this.parser = new SingleFormatParser(masonParser, ".yaml", ".yml");
            this.writer = new MasonModelWriter("pom.yaml", yamlFactory);
        }

        @Override
        @Nonnull
        public Dialect getDialect() {
            return YAML;
        }

        @Override
        @Nonnull
        public ModelParser getModelParser() {
            return parser;
        }

        @Override
        @Nonnull
        public ModelWriter getModelWriter() {
            return writer;
        }
    }

    @Named("mason-toml")
    @Singleton
    public static class TomlDialectProvider implements ModelDialectProvider {

        private final ModelParser parser;
        private final ModelWriter writer;

        @Inject
        public TomlDialectProvider(MasonParser masonParser) {
            JsonFactory tomlFactory = TomlFactory.builder().build();
            this.parser = new SingleFormatParser(masonParser, ".toml");
            this.writer = new MasonModelWriter("pom.toml", tomlFactory);
        }

        @Override
        @Nonnull
        public Dialect getDialect() {
            return TOML;
        }

        @Override
        @Nonnull
        public ModelParser getModelParser() {
            return parser;
        }

        @Override
        @Nonnull
        public ModelWriter getModelWriter() {
            return writer;
        }
    }

    @Named("mason-json")
    @Singleton
    public static class JsonDialectProvider implements ModelDialectProvider {

        private final ModelParser parser;
        private final ModelWriter writer;

        @Inject
        public JsonDialectProvider(MasonParser masonParser) {
            JsonFactory jsonFactory = JsonFactory.builder()
                    .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                    .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                    .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                    .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                    .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                    .build();
            this.parser = new SingleFormatParser(masonParser, ".json");
            this.writer = new MasonModelWriter("pom.json", jsonFactory);
        }

        @Override
        @Nonnull
        public Dialect getDialect() {
            return JSON;
        }

        @Override
        @Nonnull
        public ModelParser getModelParser() {
            return parser;
        }

        @Override
        @Nonnull
        public ModelWriter getModelWriter() {
            return writer;
        }
    }

    @Named("mason-hocon")
    @Singleton
    public static class HoconDialectProvider implements ModelDialectProvider {

        private final ModelParser parser;
        private final ModelWriter writer;

        @Inject
        public HoconDialectProvider(MasonParser masonParser) {
            // HOCON write uses JSON format (HOCON is a superset of JSON)
            JsonFactory jsonFactory = JsonFactory.builder()
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                    .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                    .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                    .build();
            this.parser = new SingleFormatParser(masonParser, ".hocon", ".conf");
            this.writer = new MasonModelWriter("pom.hocon", jsonFactory);
        }

        @Override
        @Nonnull
        public Dialect getDialect() {
            return HOCON;
        }

        @Override
        @Nonnull
        public ModelParser getModelParser() {
            return parser;
        }

        @Override
        @Nonnull
        public ModelWriter getModelWriter() {
            return writer;
        }
    }
}
