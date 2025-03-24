# Mason
[![License](https://img.shields.io/badge/License-EPL--2.0-blue.svg)](https://opensource.org/licenses/EPL-2.0) [![Maven Central](https://img.shields.io/maven-central/v/eu.maveniverse.maven.mason/mason.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.jline/jline)

Mason is a Maven extension that provides alternative model readers for Maven POM files, supporting multiple formats including:

* YAML
* JSON5
* TOML (limited support)
* HOCON  (limited support)

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

## Building

```bash
mvn clean install
```

## License

[Eclipse Public License, Version 2.0](https://opensource.org/licenses/EPL-2.0)

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.