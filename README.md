# Mason
[![License](https://img.shields.io/badge/License-EPL--2.0-blue.svg)](https://opensource.org/licenses/EPL-2.0) [![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.mason/mason.svg?label=Maven%20Central)](https://search.maven.org/artifact/eu.maveniverse.maven.mason/mason)

Mason is a Maven extension that provides alternative model readers for Maven POM files, supporting multiple formats including:

* YAML
* JSON5
* TOML (limited support)
* HOCON  (limited support)

## Requirements

* Java 17+
* Maven 4.0.0-rc-3+

## Getting Started

1. Make sure you are using **Maven 4.0.0-rc-3 or later**. Mason does not work with Maven 3.x.

2. Create the file `.mvn/extensions.xml` **in your project root directory** (next to your `pom.yaml` or `pom.json5`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.2.0">
  <extension>
    <groupId>eu.maveniverse.maven.mason</groupId>
    <artifactId>mason</artifactId>
    <version>0.3.0</version>
  </extension>
</extensions>
```

3. Replace your `pom.xml` with a `pom.yaml`, `pom.json`, `pom.conf`, or `pom.toml` file.

4. Run Maven as usual: `mvn verify`

> **Note:** The `.mvn/extensions.xml` file must be in the project root directory (the same directory as your POM file). For multi-module projects, it only needs to be in the root project — child modules will inherit the extension.

## Format Examples

The following examples show how to write a POM in each supported format:

### YAML Example (pom.yaml)

```yaml
modelVersion: 4.0.0
parent: org.apache.maven.extensions:maven-extensions:43
id: org.apache.maven.extensions:maven-yaml-extension:1.0.0-SNAPSHOT
packaging: jar
dependencyManagement:
  dependencies:
    - org.junit:junit-bom:5.12.0@import
dependencies:
  - org.apache.maven:maven-api-spi:${maven.version}@provided
  - org.apache.maven:maven-api-core:${maven.version}@provided
  - org.junit.jupiter:junit-jupiter-api@test
build:
  plugins:
    - id: org.codehaus.modello:modello-maven-plugin:2.1.1
      executions:
        - id: generate-yaml-reader
          goals: [velocity]
          phase: generate-sources
          configuration:
            version: 4.2.0
            models: ['target/dependency/maven-api-model-${maven.version}.mdo']
            templates: ['src/mdo/map-model-reader.vm']
            params: ["packageModelV4=org.apache.maven.api.model"]
```

### JSON Example (pom.json)

```json
{
  "modelVersion": "4.0.0",
  "parent": "org.apache.maven.extensions:maven-extensions:43",
  "id": "org.apache.maven.extensions:maven-yaml-extension:1.0.0-SNAPSHOT",
  "packaging": "jar",
  "dependencyManagement": {
    "dependencies": [
      "org.junit:junit-bom:5.12.0@import"
    ]
  },
  "dependencies": [
    "org.apache.maven:maven-api-spi:${maven.version}@provided",
    "org.apache.maven:maven-api-core:${maven.version}@provided",
    "org.junit.jupiter:junit-jupiter-api@test"
  ],
  "build": {
    "plugins": [
      {
        "id": "org.codehaus.modello:modello-maven-plugin:2.1.1",
        "executions": [
          {
            "id": "generate-yaml-reader",
            "goals": [
              "velocity"
            ],
            "phase": "generate-sources",
            "configuration": {
              "version": "4.2.0",
              "models": [
                "target/dependency/maven-api-model-${maven.version}.mdo"
              ],
              "templates": [
                "src/mdo/map-model-reader.vm"
              ],
              "params": [
                "packageModelV4=org.apache.maven.api.model"
              ]
            }
          }
        ]
      }
    ]
  }
}
```

### HOCON Example (pom.conf or pom.hocon)

> ⚠️ **Warning**: Advanced HOCON features like substitutions (${...}), includes, and object merging are not supported.

```hocon
modelVersion: 4.0.0
parent: "org.apache.maven.extensions:maven-extensions:43"
id: "org.apache.maven.extensions:maven-yaml-extension:1.0.0-SNAPSHOT"
packaging: jar
dependencyManagement: {
  dependencies: [
    "org.junit:junit-bom:5.12.0@import"
  ]
}
dependencies: [
  "org.apache.maven:maven-api-spi:${maven.version}@provided",
  "org.apache.maven:maven-api-core:${maven.version}@provided",
  "org.junit.jupiter:junit-jupiter-api@test"
]
build {
  plugins: [
    {
      id: "org.codehaus.modello:modello-maven-plugin:2.1.1"
      executions: [
        {
          id: generate-yaml-reader
          goals: [
            velocity
          ]
          phase: generate-sources
          configuration {
            version: 4.2.0
            models: [
              "target/dependency/maven-api-model-${maven.version}.mdo"
            ]
            templates: [
              src/mdo/map-model-reader.vm
            ]
            params: [
              "packageModelV4=org.apache.maven.api.model"
            ]
          }
        }
      ]
    }
  ]
}
```

### TOML Example (pom.toml)

> ⚠️ **Warning**: Location tracking (line numbers in error messages) is not supported for TOML files.

```toml
modelVersion = "4.0.0"
parent = "org.apache.maven.extensions:maven-extensions:43"
id = "org.apache.maven.extensions:maven-yaml-extension:1.0.0-SNAPSHOT"
packaging = "jar"
dependencyManagement.dependencies = [
  "org.junit:junit-bom:5.12.0@import"
]
dependencies = [
  "org.apache.maven:maven-api-spi:${maven.version}@provided",
  "org.apache.maven:maven-api-core:${maven.version}@provided",
  "org.junit.jupiter:junit-jupiter-api@test"
]
[build]
  [[build.plugins]]
    id = "org.codehaus.modello:modello-maven-plugin:2.1.1"
    [[build.plugins.executions]]
      id = "generate-yaml-reader"
      goals = ["velocity"]
      phase = "generate-sources"
      [build.plugins.executions.configuration]
        version = "4.2.0"
        models = ["target/dependency/maven-api-model-${maven.version}.mdo"]
        templates = ["src/mdo/map-model-reader.vm"]
        params = ["packageModelV4=org.apache.maven.api.model"]
```

## Compact String Syntax

Mason lets you write Maven coordinates as compact strings instead of verbose XML-style objects.

### GAV Strings

Used for `parent`, project `id`, plugins, and extensions:

```
groupId:artifactId[:version]
```

All parts are optional with Maven 4's inference mechanism (they can be inferred from parent or dependency management).

| Example | Description |
|---------|-------------|
| `org.apache:apache:28` | Full G:A:V |
| `org.apache.maven.plugins:maven-compiler-plugin` | G:A only (version inferred) |

### Dependency Strings

Dependencies support an extended syntax with type, classifier, scope, and optional marker:

```
groupId:artifactId[:version][:type][:classifier][@scope][?]
```

| Element | Delimiter | Description |
|---------|-----------|-------------|
| `groupId` | first `:` segment | Maven group ID |
| `artifactId` | second `:` segment | Maven artifact ID |
| `version` | third `:` segment | Version (optional) |
| `type` | fourth `:` segment | Packaging type, e.g. `jar`, `test-jar`, `pom` |
| `classifier` | fifth `:` segment | Classifier, e.g. `tests`, `sources` |
| `@scope` | after `@` | Scope: `compile`, `provided`, `runtime`, `test`, `system`, `import` |
| `?` | trailing suffix | Marks the dependency as optional |

**Examples:**

| String | Description |
|--------|-------------|
| `org.apache.maven:maven-core:3.9.0` | Basic G:A:V |
| `org.junit.jupiter:junit-jupiter-api@test` | G:A with scope (version inferred) |
| `org.apache.maven:maven-api-spi:${maven.version}@provided` | G:A:V with scope |
| `org.apache.maven:maven-model:3.9.0:jar` | G:A:V with type |
| `org.apache.maven:maven-core:3.9.0:jar@provided` | G:A:V:type with scope |
| `org.apache.maven:maven-core:3.9.0:test-jar:tests@test` | G:A:V:type:classifier with scope |
| `org.junit:junit-bom:5.12.0@import` | BOM import |
| `commons-io:commons-io:2.11.0?` | Optional dependency |

### The `id` Field

Instead of a bare string, you can use the `id` field inside an object. This is useful when you need to specify additional properties alongside the compact coordinates:

```yaml
dependencies:
  - id: org.apache.maven:maven-core:3.9.0@provided
  - id: org.junit.jupiter:junit-jupiter-api@test
    exclusions:
      - org.junit.jupiter:junit-jupiter-engine

parent:
  id: org.apache:apache:28
  relativePath: ../pom.xml

build:
  plugins:
    - id: org.apache.maven.plugins:maven-compiler-plugin:3.11.0
      configuration:
        release: 17
```

### Direct String Values

Any object that supports compact syntax can be written as a plain string. These are equivalent:

```yaml
# Compact string
parent: org.apache:apache:28

# Expanded object
parent:
  groupId: org.apache
  artifactId: apache
  version: "28"
```

```yaml
# Compact string
dependencies:
  - org.apache.maven:maven-core:3.9.0@provided

# Expanded object
dependencies:
  - groupId: org.apache.maven
    artifactId: maven-core
    version: 3.9.0
    scope: provided
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
