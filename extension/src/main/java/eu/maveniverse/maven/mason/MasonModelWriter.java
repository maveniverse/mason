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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.ActivationOS;
import org.apache.maven.api.model.ActivationProperty;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.BuildBase;
import org.apache.maven.api.model.CiManagement;
import org.apache.maven.api.model.ConfigurationContainer;
import org.apache.maven.api.model.Contributor;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.DeploymentRepository;
import org.apache.maven.api.model.Developer;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.Extension;
import org.apache.maven.api.model.FileSet;
import org.apache.maven.api.model.IssueManagement;
import org.apache.maven.api.model.License;
import org.apache.maven.api.model.MailingList;
import org.apache.maven.api.model.Mixin;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Notifier;
import org.apache.maven.api.model.Organization;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.model.Prerequisites;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.ReportSet;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.RepositoryPolicy;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.model.Site;
import org.apache.maven.api.spi.ModelWriter;
import org.apache.maven.api.spi.ModelWriterException;
import org.apache.maven.api.xml.XmlNode;

/**
 * Writes a Maven {@link Model} to a file using a Jackson {@link JsonFactory}.
 * The same code path handles YAML, TOML, and JSON by swapping the factory.
 * <p>
 * The output structure mirrors exactly what {@link MavenJsonReader} can read back:
 * same field names, same shorthand conventions (GAV strings), same XmlNode mapping.
 *
 * @since 0.4.0
 */
public class MasonModelWriter implements ModelWriter {

    /** Filename to write (e.g. "pom.yaml", "pom.toml", "pom.json"). */
    private final String filename;

    /** Factory that determines the output format. */
    private final JsonFactory factory;

    public MasonModelWriter(String filename, JsonFactory factory) {
        this.filename = filename;
        this.factory = factory;
    }

    // -------------------------------------------------------------------------
    // ModelWriter contract
    // -------------------------------------------------------------------------

    @Override
    public Optional<Path> target(Path dir) {
        if (Files.isDirectory(dir)) {
            Path target = dir.resolve(filename);
            // refuse if the target is itself a directory
            if (!Files.isDirectory(target)) {
                return Optional.of(target);
            }
        }
        return Optional.empty();
    }

    @Override
    public void write(Path target, Model model, Map<String, ?> options) throws ModelWriterException {
        try (OutputStream out = Files.newOutputStream(target);
                JsonGenerator gen = factory.createGenerator(out)) {
            gen.setPrettyPrinter(new DefaultPrettyPrinter());
            writeModel(gen, model);
            gen.flush();
        } catch (IOException e) {
            throw new ModelWriterException("Failed to write model to " + target + ": " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Model serialisation
    // -------------------------------------------------------------------------

    private void writeModel(JsonGenerator gen, Model model) throws IOException {
        gen.writeStartObject();

        writeStringField(gen, "modelVersion", model.getModelVersion());
        writeParent(gen, model.getParent());
        writeMixins(gen, model.getMixins());
        writeStringField(gen, "groupId", model.getGroupId());
        writeStringField(gen, "artifactId", model.getArtifactId());
        writeStringField(gen, "version", model.getVersion());
        writeStringField(gen, "packaging", model.getPackaging());
        writeStringField(gen, "name", model.getName());
        writeStringField(gen, "description", model.getDescription());
        writeStringField(gen, "url", model.getUrl());
        writeBooleanField(gen, "root", model.isRoot(), false);
        writeStringField(gen, "inceptionYear", model.getInceptionYear());
        writeOrganization(gen, model.getOrganization());
        writeLicenses(gen, model.getLicenses());
        writeDevelopers(gen, model.getDevelopers());
        writeContributors(gen, model.getContributors());
        writeMailingLists(gen, model.getMailingLists());
        writePrerequisites(gen, model.getPrerequisites());
        writeScm(gen, model.getScm());
        writeIssueManagement(gen, model.getIssueManagement());
        writeCiManagement(gen, model.getCiManagement());
        writeBuild(gen, model.getBuild());
        writeProfiles(gen, model.getProfiles());
        writeModules(gen, model.getModules());
        writeRepositories(gen, "repositories", model.getRepositories());
        writeRepositories(gen, "pluginRepositories", model.getPluginRepositories());
        writeDependencies(gen, "dependencies", model.getDependencies());
        writeReporting(gen, model.getReporting());
        writeDependencyManagement(gen, model.getDependencyManagement());
        writeDistributionManagement(gen, model.getDistributionManagement());
        writeStringMap(gen, "properties", model.getProperties());

        gen.writeEndObject();
    }

    // -------------------------------------------------------------------------
    // Top-level model sections
    // -------------------------------------------------------------------------

    private void writeParent(JsonGenerator gen, Parent parent) throws IOException {
        if (parent == null) return;
        gen.writeObjectFieldStart("parent");
        writeStringField(gen, "groupId", parent.getGroupId());
        writeStringField(gen, "artifactId", parent.getArtifactId());
        writeStringField(gen, "version", parent.getVersion());
        writeStringField(gen, "relativePath", parent.getRelativePath());
        gen.writeEndObject();
    }

    private void writeMixins(JsonGenerator gen, List<Mixin> mixins) throws IOException {
        if (mixins == null || mixins.isEmpty()) return;
        gen.writeArrayFieldStart("mixins");
        for (Mixin m : mixins) {
            gen.writeStartObject();
            writeStringField(gen, "version", m.getVersion());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeOrganization(JsonGenerator gen, Organization org) throws IOException {
        if (org == null) return;
        gen.writeObjectFieldStart("organization");
        writeStringField(gen, "name", org.getName());
        writeStringField(gen, "url", org.getUrl());
        gen.writeEndObject();
    }

    private void writeLicenses(JsonGenerator gen, List<License> licenses) throws IOException {
        if (licenses == null || licenses.isEmpty()) return;
        gen.writeArrayFieldStart("licenses");
        for (License l : licenses) {
            gen.writeStartObject();
            writeStringField(gen, "name", l.getName());
            writeStringField(gen, "url", l.getUrl());
            writeStringField(gen, "distribution", l.getDistribution());
            writeStringField(gen, "comments", l.getComments());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeDevelopers(JsonGenerator gen, List<Developer> devs) throws IOException {
        if (devs == null || devs.isEmpty()) return;
        gen.writeArrayFieldStart("developers");
        for (Developer d : devs) {
            gen.writeStartObject();
            writeStringField(gen, "id", d.getId());
            writeContributorFields(gen, d);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeContributors(JsonGenerator gen, List<Contributor> contributors) throws IOException {
        if (contributors == null || contributors.isEmpty()) return;
        gen.writeArrayFieldStart("contributors");
        for (Contributor c : contributors) {
            gen.writeStartObject();
            writeContributorFields(gen, c);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeContributorFields(JsonGenerator gen, Contributor c) throws IOException {
        writeStringField(gen, "name", c.getName());
        writeStringField(gen, "email", c.getEmail());
        writeStringField(gen, "url", c.getUrl());
        writeStringField(gen, "organization", c.getOrganization());
        writeStringField(gen, "organizationUrl", c.getOrganizationUrl());
        writeStringList(gen, "roles", c.getRoles());
        writeStringField(gen, "timezone", c.getTimezone());
        writeStringMap(gen, "properties", c.getProperties());
    }

    private void writeMailingLists(JsonGenerator gen, List<MailingList> lists) throws IOException {
        if (lists == null || lists.isEmpty()) return;
        gen.writeArrayFieldStart("mailingLists");
        for (MailingList ml : lists) {
            gen.writeStartObject();
            writeStringField(gen, "name", ml.getName());
            writeStringField(gen, "subscribe", ml.getSubscribe());
            writeStringField(gen, "unsubscribe", ml.getUnsubscribe());
            writeStringField(gen, "post", ml.getPost());
            writeStringField(gen, "archive", ml.getArchive());
            writeStringList(gen, "otherArchives", ml.getOtherArchives());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writePrerequisites(JsonGenerator gen, Prerequisites prereqs) throws IOException {
        if (prereqs == null) return;
        gen.writeObjectFieldStart("prerequisites");
        writeStringField(gen, "maven", prereqs.getMaven());
        gen.writeEndObject();
    }

    private void writeScm(JsonGenerator gen, Scm scm) throws IOException {
        if (scm == null) return;
        gen.writeObjectFieldStart("scm");
        writeStringField(gen, "connection", scm.getConnection());
        writeStringField(gen, "developerConnection", scm.getDeveloperConnection());
        writeStringField(gen, "tag", scm.getTag());
        writeStringField(gen, "url", scm.getUrl());
        gen.writeEndObject();
    }

    private void writeIssueManagement(JsonGenerator gen, IssueManagement im) throws IOException {
        if (im == null) return;
        gen.writeObjectFieldStart("issueManagement");
        writeStringField(gen, "system", im.getSystem());
        writeStringField(gen, "url", im.getUrl());
        gen.writeEndObject();
    }

    private void writeCiManagement(JsonGenerator gen, CiManagement ci) throws IOException {
        if (ci == null) return;
        gen.writeObjectFieldStart("ciManagement");
        writeStringField(gen, "system", ci.getSystem());
        writeStringField(gen, "url", ci.getUrl());
        if (ci.getNotifiers() != null && !ci.getNotifiers().isEmpty()) {
            gen.writeArrayFieldStart("notifiers");
            for (Notifier n : ci.getNotifiers()) {
                gen.writeStartObject();
                writeStringField(gen, "type", n.getType());
                writeBooleanField(gen, "sendOnError", n.isSendOnError(), true);
                writeBooleanField(gen, "sendOnFailure", n.isSendOnFailure(), true);
                writeBooleanField(gen, "sendOnSuccess", n.isSendOnSuccess(), true);
                writeBooleanField(gen, "sendOnWarning", n.isSendOnWarning(), true);
                writeStringField(gen, "address", n.getAddress());
                writeStringMap(gen, "configuration", n.getConfiguration());
                gen.writeEndObject();
            }
            gen.writeEndArray();
        }
        gen.writeEndObject();
    }

    private void writeBuild(JsonGenerator gen, Build build) throws IOException {
        if (build == null) return;
        gen.writeObjectFieldStart("build");
        writeStringField(gen, "sourceDirectory", build.getSourceDirectory());
        writeStringField(gen, "scriptSourceDirectory", build.getScriptSourceDirectory());
        writeStringField(gen, "testSourceDirectory", build.getTestSourceDirectory());
        writeStringField(gen, "outputDirectory", build.getOutputDirectory());
        writeStringField(gen, "testOutputDirectory", build.getTestOutputDirectory());
        writeExtensions(gen, build.getExtensions());
        writeBuildBaseFields(gen, build);
        gen.writeEndObject();
    }

    private void writeBuildBaseFields(JsonGenerator gen, BuildBase build) throws IOException {
        writeStringField(gen, "defaultGoal", build.getDefaultGoal());
        writeResources(gen, "resources", build.getResources());
        writeResources(gen, "testResources", build.getTestResources());
        writeStringField(gen, "directory", build.getDirectory());
        writeStringField(gen, "finalName", build.getFinalName());
        writeStringList(gen, "filters", build.getFilters());
        writePluginManagement(gen, build.getPluginManagement());
        writePlugins(gen, build.getPlugins());
    }

    private void writeExtensions(JsonGenerator gen, List<Extension> extensions) throws IOException {
        if (extensions == null || extensions.isEmpty()) return;
        gen.writeArrayFieldStart("extensions");
        for (Extension e : extensions) {
            gen.writeStartObject();
            writeStringField(gen, "groupId", e.getGroupId());
            writeStringField(gen, "artifactId", e.getArtifactId());
            writeStringField(gen, "version", e.getVersion());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeResources(JsonGenerator gen, String fieldName, List<Resource> resources) throws IOException {
        if (resources == null || resources.isEmpty()) return;
        gen.writeArrayFieldStart(fieldName);
        for (Resource r : resources) {
            gen.writeStartObject();
            writeStringField(gen, "targetPath", r.getTargetPath());
            writeBooleanField(gen, "filtering", r.isFiltering(), false);
            writeFileSetFields(gen, r);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeFileSetFields(JsonGenerator gen, FileSet fs) throws IOException {
        writeStringField(gen, "directory", fs.getDirectory());
        writeStringList(gen, "includes", fs.getIncludes());
        writeStringList(gen, "excludes", fs.getExcludes());
    }

    private void writePluginManagement(JsonGenerator gen, PluginManagement pm) throws IOException {
        if (pm == null) return;
        gen.writeObjectFieldStart("pluginManagement");
        writePlugins(gen, pm.getPlugins());
        gen.writeEndObject();
    }

    private void writePlugins(JsonGenerator gen, List<Plugin> plugins) throws IOException {
        if (plugins == null || plugins.isEmpty()) return;
        gen.writeArrayFieldStart("plugins");
        for (Plugin p : plugins) {
            gen.writeStartObject();
            writeStringField(gen, "groupId", p.getGroupId());
            writeStringField(gen, "artifactId", p.getArtifactId());
            writeStringField(gen, "version", p.getVersion());
            writeBooleanField(gen, "extensions", p.isExtensions(), false);
            writePluginExecutions(gen, p.getExecutions());
            writeDependencies(gen, "dependencies", p.getDependencies());
            writeConfigurationField(gen, p);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writePluginExecutions(JsonGenerator gen, List<PluginExecution> executions) throws IOException {
        if (executions == null || executions.isEmpty()) return;
        gen.writeArrayFieldStart("executions");
        for (PluginExecution e : executions) {
            gen.writeStartObject();
            writeStringField(gen, "id", e.getId());
            writeStringField(gen, "phase", e.getPhase());
            writeStringList(gen, "goals", e.getGoals());
            writeConfigurationField(gen, e);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeProfiles(JsonGenerator gen, List<Profile> profiles) throws IOException {
        if (profiles == null || profiles.isEmpty()) return;
        gen.writeArrayFieldStart("profiles");
        for (Profile p : profiles) {
            gen.writeStartObject();
            writeStringField(gen, "id", p.getId());
            writeActivation(gen, p.getActivation());
            writeBuildBase(gen, p.getBuild());
            writeModules(gen, p.getModules());
            writeRepositories(gen, "repositories", p.getRepositories());
            writeRepositories(gen, "pluginRepositories", p.getPluginRepositories());
            writeDependencies(gen, "dependencies", p.getDependencies());
            writeReporting(gen, p.getReporting());
            writeDependencyManagement(gen, p.getDependencyManagement());
            writeDistributionManagement(gen, p.getDistributionManagement());
            writeStringMap(gen, "properties", p.getProperties());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeBuildBase(JsonGenerator gen, BuildBase build) throws IOException {
        if (build == null) return;
        gen.writeObjectFieldStart("build");
        writeBuildBaseFields(gen, build);
        gen.writeEndObject();
    }

    private void writeActivation(JsonGenerator gen, Activation act) throws IOException {
        if (act == null) return;
        gen.writeObjectFieldStart("activation");
        writeBooleanField(gen, "activeByDefault", act.isActiveByDefault(), false);
        writeStringField(gen, "jdk", act.getJdk());
        writeActivationOS(gen, act.getOs());
        writeActivationProperty(gen, act.getProperty());
        writeActivationFile(gen, act.getFile());
        gen.writeEndObject();
    }

    private void writeActivationOS(JsonGenerator gen, ActivationOS os) throws IOException {
        if (os == null) return;
        gen.writeObjectFieldStart("os");
        writeStringField(gen, "name", os.getName());
        writeStringField(gen, "family", os.getFamily());
        writeStringField(gen, "arch", os.getArch());
        writeStringField(gen, "version", os.getVersion());
        gen.writeEndObject();
    }

    private void writeActivationProperty(JsonGenerator gen, ActivationProperty prop) throws IOException {
        if (prop == null) return;
        gen.writeObjectFieldStart("property");
        writeStringField(gen, "name", prop.getName());
        writeStringField(gen, "value", prop.getValue());
        gen.writeEndObject();
    }

    private void writeActivationFile(JsonGenerator gen, ActivationFile file) throws IOException {
        if (file == null) return;
        gen.writeObjectFieldStart("file");
        writeStringField(gen, "missing", file.getMissing());
        writeStringField(gen, "exists", file.getExists());
        gen.writeEndObject();
    }

    private void writeModules(JsonGenerator gen, List<String> modules) throws IOException {
        writeStringList(gen, "modules", modules);
    }

    private void writeRepositories(JsonGenerator gen, String fieldName, List<Repository> repos) throws IOException {
        if (repos == null || repos.isEmpty()) return;
        gen.writeArrayFieldStart(fieldName);
        for (Repository r : repos) {
            gen.writeStartObject();
            writeStringField(gen, "id", r.getId());
            writeStringField(gen, "name", r.getName());
            writeStringField(gen, "url", r.getUrl());
            writeStringField(gen, "layout", r.getLayout());
            writeRepositoryPolicy(gen, "releases", r.getReleases());
            writeRepositoryPolicy(gen, "snapshots", r.getSnapshots());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeRepositoryPolicy(JsonGenerator gen, String fieldName, RepositoryPolicy policy)
            throws IOException {
        if (policy == null) return;
        gen.writeObjectFieldStart(fieldName);
        writeBooleanField(gen, "enabled", policy.isEnabled(), true);
        writeStringField(gen, "updatePolicy", policy.getUpdatePolicy());
        writeStringField(gen, "checksumPolicy", policy.getChecksumPolicy());
        gen.writeEndObject();
    }

    private void writeDependencies(JsonGenerator gen, String fieldName, List<Dependency> deps) throws IOException {
        if (deps == null || deps.isEmpty()) return;
        gen.writeArrayFieldStart(fieldName);
        for (Dependency d : deps) {
            gen.writeStartObject();
            writeStringField(gen, "groupId", d.getGroupId());
            writeStringField(gen, "artifactId", d.getArtifactId());
            writeStringField(gen, "version", d.getVersion());
            writeStringField(gen, "type", d.getType());
            writeStringField(gen, "classifier", d.getClassifier());
            writeStringField(gen, "scope", d.getScope());
            writeStringField(gen, "systemPath", d.getSystemPath());
            writeExclusions(gen, d.getExclusions());
            writeBooleanField(gen, "optional", d.isOptional(), false);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeExclusions(JsonGenerator gen, List<Exclusion> exclusions) throws IOException {
        if (exclusions == null || exclusions.isEmpty()) return;
        gen.writeArrayFieldStart("exclusions");
        for (Exclusion e : exclusions) {
            gen.writeStartObject();
            writeStringField(gen, "groupId", e.getGroupId());
            writeStringField(gen, "artifactId", e.getArtifactId());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeReporting(JsonGenerator gen, Reporting reporting) throws IOException {
        if (reporting == null) return;
        gen.writeObjectFieldStart("reporting");
        writeBooleanField(gen, "excludeDefaults", reporting.isExcludeDefaults(), false);
        writeStringField(gen, "outputDirectory", reporting.getOutputDirectory());
        writeReportPlugins(gen, reporting.getPlugins());
        gen.writeEndObject();
    }

    private void writeReportPlugins(JsonGenerator gen, List<ReportPlugin> plugins) throws IOException {
        if (plugins == null || plugins.isEmpty()) return;
        gen.writeArrayFieldStart("plugins");
        for (ReportPlugin p : plugins) {
            gen.writeStartObject();
            writeStringField(gen, "groupId", p.getGroupId());
            writeStringField(gen, "artifactId", p.getArtifactId());
            writeStringField(gen, "version", p.getVersion());
            writeReportSets(gen, p.getReportSets());
            writeConfigurationField(gen, p);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeReportSets(JsonGenerator gen, List<ReportSet> reportSets) throws IOException {
        if (reportSets == null || reportSets.isEmpty()) return;
        gen.writeArrayFieldStart("reportSets");
        for (ReportSet rs : reportSets) {
            gen.writeStartObject();
            writeStringField(gen, "id", rs.getId());
            writeStringList(gen, "reports", rs.getReports());
            writeConfigurationField(gen, rs);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    private void writeDependencyManagement(JsonGenerator gen, DependencyManagement dm) throws IOException {
        if (dm == null) return;
        gen.writeObjectFieldStart("dependencyManagement");
        writeDependencies(gen, "dependencies", dm.getDependencies());
        gen.writeEndObject();
    }

    private void writeDistributionManagement(JsonGenerator gen, DistributionManagement dm) throws IOException {
        if (dm == null) return;
        gen.writeObjectFieldStart("distributionManagement");
        writeDeploymentRepository(gen, "repository", dm.getRepository());
        writeDeploymentRepository(gen, "snapshotRepository", dm.getSnapshotRepository());
        writeSite(gen, dm.getSite());
        writeStringField(gen, "downloadUrl", dm.getDownloadUrl());
        writeStringField(gen, "status", dm.getStatus());
        gen.writeEndObject();
    }

    private void writeDeploymentRepository(JsonGenerator gen, String fieldName, DeploymentRepository repo)
            throws IOException {
        if (repo == null) return;
        gen.writeObjectFieldStart(fieldName);
        writeStringField(gen, "id", repo.getId());
        writeStringField(gen, "name", repo.getName());
        writeStringField(gen, "url", repo.getUrl());
        writeStringField(gen, "layout", repo.getLayout());
        writeRepositoryPolicy(gen, "releases", repo.getReleases());
        writeRepositoryPolicy(gen, "snapshots", repo.getSnapshots());
        writeBooleanField(gen, "uniqueVersion", repo.isUniqueVersion(), true);
        gen.writeEndObject();
    }

    private void writeSite(JsonGenerator gen, Site site) throws IOException {
        if (site == null) return;
        gen.writeObjectFieldStart("site");
        writeStringField(gen, "id", site.getId());
        writeStringField(gen, "name", site.getName());
        writeStringField(gen, "url", site.getUrl());
        gen.writeEndObject();
    }

    // -------------------------------------------------------------------------
    // XmlNode → Jackson
    // -------------------------------------------------------------------------

    /**
     * Writes the {@code configuration} of a {@link ConfigurationContainer} element,
     * serialising the {@link XmlNode} tree as a nested JSON/YAML object.
     */
    private void writeConfigurationField(JsonGenerator gen, ConfigurationContainer container) throws IOException {
        XmlNode cfg = container.getConfiguration();
        if (cfg == null) return;
        gen.writeFieldName("configuration");
        writeXmlNode(gen, cfg);
    }

    private void writeXmlNode(JsonGenerator gen, XmlNode node) throws IOException {
        List<XmlNode> children = node.getChildren();
        Map<String, String> attrs = node.getAttributes();
        String value = node.getValue();

        boolean hasChildren = children != null && !children.isEmpty();
        boolean hasAttrs = attrs != null && !attrs.isEmpty();

        if (!hasChildren && !hasAttrs) {
            // leaf node — write as plain scalar
            gen.writeString(value != null ? value : "");
            return;
        }

        gen.writeStartObject();
        if (attrs != null) {
            for (Map.Entry<String, String> e : attrs.entrySet()) {
                gen.writeStringField("@" + e.getKey(), e.getValue());
            }
        }
        if (value != null && !value.isBlank()) {
            gen.writeStringField("#text", value);
        }
        if (hasChildren) {
            // Group children by name: repeated names → arrays
            java.util.LinkedHashMap<String, List<XmlNode>> byName = new java.util.LinkedHashMap<>();
            for (XmlNode child : children) {
                byName.computeIfAbsent(child.getName(), k -> new java.util.ArrayList<>())
                        .add(child);
            }
            for (Map.Entry<String, List<XmlNode>> e : byName.entrySet()) {
                List<XmlNode> group = e.getValue();
                if (group.size() == 1) {
                    gen.writeFieldName(e.getKey());
                    writeXmlNode(gen, group.get(0));
                } else {
                    gen.writeArrayFieldStart(e.getKey() + "s"); // pluralise
                    for (XmlNode child : group) {
                        writeXmlNode(gen, child);
                    }
                    gen.writeEndArray();
                }
            }
        }
        gen.writeEndObject();
    }

    // -------------------------------------------------------------------------
    // Primitive write helpers
    // -------------------------------------------------------------------------

    private void writeStringField(JsonGenerator gen, String field, String value) throws IOException {
        if (value != null && !value.isEmpty()) {
            gen.writeStringField(field, value);
        }
    }

    private void writeBooleanField(JsonGenerator gen, String field, boolean value, boolean defaultValue)
            throws IOException {
        if (value != defaultValue) {
            gen.writeBooleanField(field, value);
        }
    }

    private void writeStringList(JsonGenerator gen, String field, List<String> values) throws IOException {
        if (values == null || values.isEmpty()) return;
        gen.writeArrayFieldStart(field);
        for (String v : values) {
            gen.writeString(v);
        }
        gen.writeEndArray();
    }

    private void writeStringMap(JsonGenerator gen, String field, Map<String, String> map) throws IOException {
        if (map == null || map.isEmpty()) return;
        gen.writeObjectFieldStart(field);
        for (Map.Entry<String, String> e : map.entrySet()) {
            gen.writeStringField(e.getKey(), e.getValue());
        }
        gen.writeEndObject();
    }
}
