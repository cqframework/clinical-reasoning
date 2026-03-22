# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## Project Overview

This is the CQF Clinical Reasoning on FHIR repository, which provides JVM-based, FHIR-enabled clinical reasoning components. The project implements FHIR Clinical Reasoning operations like Measure evaluation (`$evaluate-measure`), PlanDefinition application (`$apply`), and CQL evaluation capabilities.

## Build and Test Commands

### Building the Project
```bash
# Full build with tests
./gradlew build

# Build without tests
./gradlew assemble

# Compile only
./gradlew compileJava

# Parallel build (enabled by default via gradle.properties)
./gradlew build
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run tests in a specific module
./gradlew :cqf-fhir-cr:test

# Run a single test class
./gradlew :MODULE:test --tests ClassName

# Run a single test method
./gradlew :MODULE:test --tests "ClassName.methodName"

# Run integration tests
./gradlew integrationTest

# Skip tests
./gradlew assemble
```

### Code Formatting and Quality
```bash
# Check code formatting (runs in CI on PRs)
./gradlew spotlessCheck

# Apply code formatting fixes
./gradlew spotlessApply

# This project uses Palantir Java Format (configured in buildSrc convention plugins)
# Checkstyle runs automatically as part of the build
```

### Linked Builds (Cross-Repo Development)

To build against a local checkout of a dependency (e.g., CQL Engine) instead of Maven Central:

```bash
# Copy the template and set paths to your local checkouts
cp local.properties.example local.properties

# Edit local.properties — uncomment and set the path:
# cql.engine.path=../clinical_quality_language/Src/java

# Build as normal — the settings plugin detects local.properties and includes the linked build
./gradlew build
```

Available linked builds are listed in `local.properties.example`. The registry is defined in `build-logic/src/main/kotlin/cqf/LinkedBuild.kt`.

In CI, the `resolveLinkedBuilds` task parses `Depends-On: org/repo#branch` directives from PR descriptions to automatically clone and link upstream repos.

### Other Useful Commands
```bash
# Generate Javadocs
./gradlew javadoc

# Build CLI fat JAR
./gradlew :cqf-fhir-cr-cli:bootJar

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Build documentation locally (requires Python 3 and MkDocs)
cd docs/src/doc
pip3 install -r requirements.txt
mkdocs serve  # Browse at http://127.0.0.1:8000/
```

## Architecture, Design Patterns, and Code Style

See the [Developer Guide](docs/src/doc/docs/developer-guide.md) for a detailed overview of the architecture, design patterns, and coding standards used in this project.

## Branch and PR Workflow

- **Main Branch**: `master` (target branch for PRs)
- **Feature Branches**: Create from `master` with descriptive names
- **PR Requirements**:
  - Must pass CI build
  - Must pass spotlessCheck formatting
  - Must pass checkstyle validation
  - Delete feature branches after merging
- **Commits**: Treated as snapshot builds, versions use -SNAPSHOT suffix

## Release and Publishing

- **Publishing**: Uses `maven-publish` plugin to Sonatype Central
- **Excluded Artifacts**: cqf-fhir-benchmark and docs are not published
- **Signing**: GPG signing required for releases (only when version is not SNAPSHOT)
- **BOM**: `cqf-fhir-bom` provides dependency management for all published modules

## Documentation

- Main documentation site: www.cqframework.org/clinical-reasoning
- Documentation source: docs/src/doc (MkDocs Material)
- API documentation: Generated via Javadoc task
- Key docs: operations.md, developer-guide.md, cql.md

## Related Projects

- Clinical Quality Language: https://github.com/cqframework/clinical_quality_language
- HAPI FHIR: https://github.com/hapifhir/hapi-fhir
- CQL VS Code Extension: https://marketplace.visualstudio.com/items?itemName=cqframework.cql

## License

Apache License 2.0 - All contributions licensed under Apache 2.0
