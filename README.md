# Mason
[![License](https://img.shields.io/badge/License-EPL--2.0-blue.svg)](https://opensource.org/licenses/EPL-2.0) [![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.mason/mason.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.jline/jline)

Mason is a Maven extension that provides alternative model readers for Maven POM files, supporting multiple formats including:

* YAML
* TOML
* JSON
* HOCON

## Requirements

* Java 17+
* Maven 4.0.0-rc-3+

## Usage

Add the extension to your Maven installation:

```xml
<extension>
  <groupId>eu.maveniverse.maven.mason</groupId>
  <artifactId>mason</artifactId>
  <version>0.1.0</version>
</extension>
```

Then you can use alternative formats for your POM files:

### YAML Example (pom.yaml)

```yaml
modelVersion: 4.0.0
parent: org.apache.maven.extensions:maven-extensions:43
id: org.apache.maven.extensions:maven-yaml-extension:1.0.0-SNAPSHOT
packaging: jar
dependencies:
  - org.apache.maven:maven-api-spi:${maven.version}@provided
  - org.apache.maven:maven-api-core:${maven.version}@provided
```

### TOML Example (pom.toml)

> ⚠️ **Warning**: Location tracking (line numbers in error messages) is not supported for TOML files.

```toml
modelVersion = "4.0.0"
parent = "org.apache.maven.extensions:maven-extensions:43"
id = "org.apache.maven.extensions:maven-yaml-extension:1.0.0-SNAPSHOT"
packaging = "jar"
dependencies = [
  "org.apache.maven:maven-api-spi:${maven.version}@provided",
  "org.apache.maven:maven-api-core:${maven.version}@provided"
]
```

### JSON Example (pom.json)

```json
{
  "modelVersion": "4.0.0",
  "parent": "org.apache.maven.extensions:maven-extensions:43",
  "id": "org.apache.maven.extensions:maven-yaml-extension:1.0.0-SNAPSHOT",
  "packaging": "jar",
  "dependencies": [
    "org.apache.maven:maven-api-spi:${maven.version}@provided",
    "org.apache.maven:maven-api-core:${maven.version}@provided"
  ]
}
```

### HOCON Example (pom.conf or pom.hocon)

> ⚠️ **Warning**: Advanced HOCON features like substitutions (${...}), includes, and object merging are not supported.

```hocon
modelVersion: 4.0.0
parent: "org.apache.maven.extensions:maven-extensions:43"
id: "org.apache.maven.extensions:maven-yaml-extension:1.0.0-SNAPSHOT"
packaging: jar
dependencies: [
  "org.apache.maven:maven-api-spi:${maven.version}@provided",
  "org.apache.maven:maven-api-core:${maven.version}@provided"
]
```

## Building

```bash
mvn clean install
```

## License

[Eclipse Public License, Version 2.0](https://opensource.org/licenses/EPL-2.0)

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.