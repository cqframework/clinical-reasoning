# CQF Clinical Reasoning on FHIR for Java

[![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://chat.fhir.org/#narrow/stream/179220-cql)
[![Release](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf.fhir/cqf-fhir/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf.fhir/cqf-fhir)
[![Build](https://github.com/cqframework/clinical-reasoning/actions/workflows/build.yml/badge.svg)](https://github.com/cqframework/clinical-reasoning/actions/workflows/build.yml)

This project provides a Java-based, FHIR-enabled clinical reasoning components.

See the [docs](https://www.cqframework.org/clinical-reasoning/) for more information.

## Building

This project uses [Gradle](https://gradle.org/) with the Kotlin DSL. Java 17 is required.

```bash
# Build the project (tests included)
./gradlew build

# Build without running tests
./gradlew assemble

# Run tests for a specific module
./gradlew :cqf-fhir-cr:test

# Run a single test class
./gradlew :cqf-fhir-cr:test --tests "ClassName"

# Check code formatting (CI enforces this)
./gradlew spotlessCheck

# Auto-fix code formatting
./gradlew spotlessApply
```

### Dependency Versions

All dependency versions are managed in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) (the Gradle [version catalog](https://docs.gradle.org/current/userguide/version_catalogs.html)). To upgrade a dependency, edit the version in that file and rebuild. Build infrastructure plugin versions are in `build-logic/gradle/libs.versions.toml`.

### Upgrading the Gradle Wrapper

The `gradlew` script and `gradle/wrapper/` files are the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html), which ensures everyone uses the same Gradle version. To upgrade:

```bash
./gradlew wrapper --gradle-version <new-version>
```

This updates `gradlew`, `gradlew.bat`, and the wrapper JAR/properties. Commit all changed files.

## SonarCloud

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=cqframework_clinical-reasoning&metric=coverage)]
(https://sonarcloud.io/dashboard?id=cqframework_clinical-reasoning)

## Getting Help

Bugs and feature requests can be filed with [Github Issues](https://github.com/cqframework/clinical-reasoning/issues).

The implementers are active on the official FHIR [Zulip chat for CQL](https://chat.fhir.org/#narrow/stream/179220-cql).

Inquires for commercial support can be directed to [info@alphora.com](info@alphora.com).

## Developer Guide

See [AGENTS.md](AGENTS.md) for build commands, linked builds setup, and project architecture.

## Related Projects

[Clinical Quality Language](https://github.com/cqframework/clinical_quality_language) - Tooling in support of the CQL specification, including the CQL compiler and runtime used in this project.

[CQL Support for VS Code](https://marketplace.visualstudio.com/items?itemName=cqframework.cql) - Open source CQL IDE with syntax highlighting, linting, and local CQL evaluation provided by this project.

[HAPI FHIR](https://github.com/hapifhir/hapi-fhir) - This project provides CQL and Measure evaluation for the HAPI FHIR server, among other functionality.

## License

Copyright 2019+ Smile Digital Health

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
