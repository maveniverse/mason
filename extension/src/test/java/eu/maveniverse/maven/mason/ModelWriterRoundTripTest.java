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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.spi.ModelWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Round-trip tests: parse a source file, write it in the same or different format,
 * parse it back, and assert structural equality on key fields.
 */
class ModelWriterRoundTripTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // YAML → YAML round-trip
    // -------------------------------------------------------------------------

    @Test
    void yamlRoundTrip() throws Exception {
        Model original = parseYaml("yaml/pom.yaml");

        Path written = writeAndRead(original, "pom.yaml", YAMLFactory.builder().build());

        Model roundTripped = parseYamlFrom(written);

        assertEquals(original.getModelVersion(), roundTripped.getModelVersion(), "modelVersion");
        assertEquals(original.getGroupId(), roundTripped.getGroupId(), "groupId");
        assertEquals(original.getArtifactId(), roundTripped.getArtifactId(), "artifactId");
        assertEquals(original.getVersion(), roundTripped.getVersion(), "version");

        assertNotNull(original.getParent(), "original has parent");
        assertNotNull(roundTripped.getParent(), "round-tripped has parent");
        assertEquals(original.getParent().getGroupId(), roundTripped.getParent().getGroupId(), "parent.groupId");
        assertEquals(
                original.getParent().getArtifactId(), roundTripped.getParent().getArtifactId(), "parent.artifactId");
        assertEquals(original.getParent().getVersion(), roundTripped.getParent().getVersion(), "parent.version");

        assertFalse(original.getDependencies().isEmpty(), "original has dependencies");
        assertFalse(roundTripped.getDependencies().isEmpty(), "round-tripped has dependencies");
        assertEquals(
                original.getDependencies().size(),
                roundTripped.getDependencies().size(),
                "dependency count");
        assertEquals(
                original.getDependencies().get(0).getGroupId(),
                roundTripped.getDependencies().get(0).getGroupId(),
                "dep[0].groupId");
        assertEquals(
                original.getDependencies().get(0).getArtifactId(),
                roundTripped.getDependencies().get(0).getArtifactId(),
                "dep[0].artifactId");

        assertFalse(original.getProperties().isEmpty(), "original has properties");
        assertEquals(original.getProperties(), roundTripped.getProperties(), "properties");
    }

    // -------------------------------------------------------------------------
    // YAML → JSON round-trip
    // -------------------------------------------------------------------------

    @Test
    void yamlToJsonRoundTrip() throws Exception {
        Model original = parseYaml("yaml/pom.yaml");

        JsonFactory jsonFactory = new JsonFactory();
        Path written = writeAndRead(original, "pom.json", jsonFactory);

        // Verify the written JSON parses back correctly
        Model roundTripped = parsePath(written, jsonFactory);

        assertEquals(original.getModelVersion(), roundTripped.getModelVersion(), "modelVersion");
        assertEquals(original.getGroupId(), roundTripped.getGroupId(), "groupId");
        assertEquals(original.getArtifactId(), roundTripped.getArtifactId(), "artifactId");
        assertEquals(
                original.getDependencies().size(),
                roundTripped.getDependencies().size(),
                "dependency count");
    }

    // -------------------------------------------------------------------------
    // YAML → TOML round-trip
    // -------------------------------------------------------------------------

    @Test
    void yamlToTomlRoundTrip() throws Exception {
        Model original = parseYaml("yaml/pom.yaml");

        Path written = writeAndRead(original, "pom.toml", TomlFactory.builder().build());

        Model roundTripped = parsePath(written, TomlFactory.builder().build());

        assertEquals(original.getModelVersion(), roundTripped.getModelVersion(), "modelVersion");
        assertEquals(original.getGroupId(), roundTripped.getGroupId(), "groupId");
        assertEquals(original.getArtifactId(), roundTripped.getArtifactId(), "artifactId");
    }

    // -------------------------------------------------------------------------
    // writer.target() contract
    // -------------------------------------------------------------------------

    @Test
    void targetRefusesNonDirectory() throws Exception {
        ModelWriter writer =
                new MasonModelWriter("pom.yaml", YAMLFactory.builder().build());
        Optional<Path> result = writer.target(tempDir.resolve("does-not-exist.yaml"));
        // non-existent path is not a directory → refused
        assertTrue(result.isEmpty(), "should refuse non-directory path");
    }

    @Test
    void targetAcceptsDirectory() throws Exception {
        ModelWriter writer =
                new MasonModelWriter("pom.yaml", YAMLFactory.builder().build());
        Optional<Path> result = writer.target(tempDir);
        assertTrue(result.isPresent(), "should accept temp directory");
        assertEquals(tempDir.resolve("pom.yaml"), result.get(), "target path");
    }

    // -------------------------------------------------------------------------
    // Full round-trip with build section (plugin configuration)
    // -------------------------------------------------------------------------

    @Test
    void buildPluginsRoundTrip() throws Exception {
        Model original = parseYaml("yaml/build-plugins.yaml");
        assertNotNull(original.getBuild(), "original has build");
        assertFalse(original.getBuild().getPlugins().isEmpty(), "original has plugins");

        Path written = writeAndRead(original, "pom.yaml", YAMLFactory.builder().build());
        Model roundTripped = parseYamlFrom(written);

        assertNotNull(roundTripped.getBuild(), "round-tripped has build");
        assertEquals(
                original.getBuild().getPlugins().size(),
                roundTripped.getBuild().getPlugins().size(),
                "plugin count");
        assertEquals(
                original.getBuild().getPlugins().get(0).getArtifactId(),
                roundTripped.getBuild().getPlugins().get(0).getArtifactId(),
                "plugin[0].artifactId");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Model parseYaml(String resourcePath) throws Exception {
        Path path =
                Path.of(getClass().getClassLoader().getResource(resourcePath).toURI());
        return new MasonParser().parse(Sources.fromPath(path), Map.of());
    }

    private Model parseYamlFrom(Path file) throws Exception {
        return parsePath(file, YAMLFactory.builder().build());
    }

    private Model parsePath(Path file, JsonFactory factory) throws Exception {
        return new MavenJsonReader(factory).read(Files.newInputStream(file), false, null);
    }

    /**
     * Writes the model to {@code tempDir/filename} and returns the written path.
     */
    private Path writeAndRead(Model model, String filename, JsonFactory factory) throws Exception {
        MasonModelWriter writer = new MasonModelWriter(filename, factory);
        Optional<Path> result = writer.targetAndWrite(tempDir, model, null);
        assertTrue(result.isPresent(), "writer should accept temp directory");
        assertTrue(Files.exists(result.get()), "written file should exist");
        assertTrue(Files.size(result.get()) > 0, "written file should be non-empty");
        return result.get();
    }
}
