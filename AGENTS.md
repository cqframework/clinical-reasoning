# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the CQF Clinical Reasoning on FHIR for Java repository, which provides Java-based, FHIR-enabled clinical reasoning components. The project implements FHIR Clinical Reasoning operations like Measure evaluation (`$evaluate-measure`), PlanDefinition application (`$apply`), and CQL evaluation capabilities.

## Build and Test Commands

### Building the Project
```bash
# Full build with tests
./mvnw clean install

# Build without tests
./mvnw install -DskipTests

# Package only (default goal)
./mvnw package

# Parallel build (faster)
./mvnw install -T 4
```

### Running Tests
```bash
# Run all tests
./mvnw test

# Run tests in a specific module
./mvnw test -pl cqf-fhir-cr

# Run a single test class
./mvnw test -Dtest=ClassName

# Run a single test method
./mvnw test -Dtest=ClassName#methodName

# Run integration tests
./mvnw verify

# Skip tests
./mvnw install -DskipTests
```

### Code Formatting and Quality
```bash
# Check code formatting (runs in CI on PRs)
./mvnw spotless:check

# Apply code formatting fixes
./mvnw spotless:apply

# This project uses Palantir Java Format (configured in pom.xml)
# Checkstyle runs automatically during validate phase
```

### Other Useful Commands
```bash
# Generate Javadocs
./mvnw javadoc:javadoc

# Run benchmarks (cqf-fhir-benchmark module)
cd cqf-fhir-benchmark && mvn clean install && java -jar target/benchmarks.jar

# Build documentation locally (requires Python 3 and MkDocs)
cd docs/src/doc
pip3 install -r requirements.txt
mkdocs serve  # Browse at http://127.0.0.1:8000/
```

## Architecture and Module Structure

This is a multi-module Maven project with the following key modules:

### Core Modules

- **cqf-fhir-utility**: Foundation utilities for FHIR clinical reasoning operations. Contains:
  - Adapter pattern implementations for version-agnostic FHIR resource handling (dstu3, r4, r5)
  - Repository API implementations (REST, filesystem, in-memory)
  - Common helpers (BundleHelper, Parameters, SearchHelper, ValueSets, etc.)
  - Builder utilities for constructing FHIR resources

- **cqf-fhir-cql**: Core CQL (Clinical Quality Language) integration with FHIR
  - LibraryEngine for CQL library evaluation
  - CQL-to-ELM translation utilities
  - EvaluationSettings and engine configuration
  - Bridges FHIR data with CQL evaluation engine

- **cqf-fhir-cr**: Clinical Reasoning operation implementations
  - Implementation of FHIR Clinical Reasoning operations (Measure, PlanDefinition, ActivityDefinition, Questionnaire, etc.)
  - Visitor pattern for knowledge artifact lifecycle operations (approve, draft, release, retire, package)
  - Operation-specific processors organized by resource type subdirectories

- **cqf-fhir-test**: Testing utilities for clinical reasoning operations
  - Shared test resources and helpers
  - Used by other modules for unit testing

### Integration Modules

- **cqf-fhir-cr-hapi**: Integration with HAPI FHIR server (replaces hapi-fhir-storage-cr)
- **cqf-fhir-cr-spring**: Spring Framework integration
- **cqf-fhir-cr-cli**: Command-line interface for running operations

### Support Modules

- **cqf-fhir-bom**: Bill of Materials for dependency management
- **cqf-fhir-benchmark**: JMH benchmarking suite
- **docs**: MkDocs-based documentation website (www.cqframework.org/clinical-reasoning)

## Key Architectural Patterns

### Adapter Pattern
The project uses adapters extensively to provide version-agnostic access to FHIR resources across DSTU3, R4, and R5. The adapter interfaces are in `cqf-fhir-utility/src/main/java/org/opencds/cqf/fhir/utility/adapter/` with version-specific implementations in subdirectories (dstu3/, r4/, r5/).

Example interfaces: `IKnowledgeArtifactAdapter`, `ILibraryAdapter`, `IResourceAdapter`, `IParametersAdapter`

### Visitor Pattern
Knowledge artifact lifecycle operations (draft, approve, release, retire, package) are implemented using visitors. Base implementation is in `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/visitor/` with version-specific implementations.

Key visitors: `DraftVisitor`, `ApproveVisitor`, `ReleaseVisitor`, `PackageVisitor`, `DataRequirementsVisitor`

### Repository API
The Repository API abstracts data access, allowing clinical reasoning operations to work across different platforms (HAPI JPA, Android SQLite, filesystem, remote FHIR servers, in-memory).

## Code Style and Standards

- **Java Version**: Java 17 (source and target)
- **Style Guide**: Google Java Style with Palantir Java Format
- **Line Length**: 100 characters (configured in .editorconfig)
- **Indentation**: 4 spaces
- **Naming**: camelCase without prefixes (m, s, my, our, the are forbidden)
- **Abstract Classes**: Must be named with "Base" or "Abstract" prefix
- **Error Prone**: Enabled as annotation processor with warnings disabled (-XepDisableAllWarnings)
- **Checkstyle**: Runs during validate phase with custom config (config/checkstyle.xml)
- **Animal Sniffer**: Enforces Android API 34 compliance

### Formatting Before Committing
Always run `./mvnw spotless:apply` before committing to ensure code formatting compliance. The CI will fail PRs that don't pass `spotless:check`.

## FHIR Version Support

This project supports multiple FHIR versions (DSTU3, R4, R5) through the adapter pattern. When implementing features:
- Add version-specific code in the appropriate subdirectory (dstu3/, r4/, r5/)
- Use adapters to write version-agnostic business logic where possible
- Test against all supported FHIR versions

## Testing Patterns

- **Unit Tests**: Use JUnit 5 (Jupiter) - JUnit 4 is banned
- **Mocking**: Mockito with mockito-junit-jupiter
- **Assertions**: Hamcrest matchers
- **Test Resources**: Located in `src/test/resources` for each module
- **Test Utilities**: Use `cqf-fhir-test` module helpers
- **JSON Assertions**: Use `jsonassert` for comparing JSON structures
- **Equality Tests**: Use `equalsverifier` for testing equals/hashCode contracts

## Measure Scoring Architecture

### MeasureReportDefScorer Integration (2025-12-16)

The measure evaluation workflow uses a version-agnostic scorer that operates on Def objects:

**Architecture:**
- **MeasureReportDefScorer**: Version-agnostic scorer in `cqf-fhir-cr/measure/common/`
- **MeasureEvaluationResultHandler**: Orchestrates scoring by calling MeasureReportDefScorer
- **Builders**: R4/Dstu3MeasureReportBuilder copy scores from Def to FHIR MeasureReport via `copyScoresFromDef()`

**Workflow:**
1. Measure evaluation creates MeasureDef with populations
2. MeasureEvaluationResultHandler calls `MeasureReportDefScorer.score()`
3. Scorer iterates Def objects (GroupDef, StratifierDef, StratumDef) and sets scores
4. Builder copies scores from Def objects to FHIR MeasureReport

**Key Classes:**
- `MeasureReportDefScorer` (common): Version-agnostic scoring logic (primary scorer)
- `R4MeasureReportScorer` (r4): **Retained for external callers only** - internal usage deprecated as of 2025-12-16. Provides R4-specific helpers for stratifier population counts. A proper external API will be implemented in a future release.

**Testing Pattern:**
Tests use def/report dual structure to verify both internal state and FHIR output:
```java
.then()
    // MeasureDef assertions (pre-scoring) - verify internal state
    .def()
        .hasNoErrors()
        .firstGroup()
            .population("numerator").hasCount(2).up()
            .hasScore(0.25)  // numeric score
        .up()
    .up()
    // MeasureReport assertions (post-scoring) - verify FHIR resource output
    .report()
        .firstGroup()
            .population("numerator").hasCount(2).up()
            .hasScore("0.25")  // string score
        .up()
    .report();
```

**Migration Notes (Old Scorer Removal):**
- ❌ Old pattern: `measureReportScorer.score()` called in builders
- ✅ New pattern: `copyScoresFromDef()` copies pre-computed scores from Def objects
- The old R4/Dstu3 scorer fields and calls were removed from builders in Phase 2 (2025-12-16)
- `Dstu3MeasureReportScorer` class deleted (2025-12-16) - no longer needed after MeasureReportDefScorer integration
- `R4MeasureReportScorer` retained for external callers only - internal usage deprecated
- Scoring now happens once in MeasureEvaluationResultHandler, not per-builder

## Dependencies

Key dependencies (versions in parent pom.xml properties):
- HAPI FHIR: ${hapi.version} (currently 8.6.0)
- CQL Engine: ${cql.version} (currently 4.1.0)
- Spring: ${spring.version} (currently 6.2.12)
- JUnit: ${junit.version} (currently 5.10.2)
- Guava: ${guava.version} (currently 33.2.1-jre)

Snapshots are pulled from: https://central.sonatype.com/repository/maven-snapshots/

## Branch and PR Workflow

- **Main Branch**: `master` (target branch for PRs)
- **Feature Branches**: Create from `master` with descriptive names
- **PR Requirements**:
  - Must pass CI build
  - Must pass spotless:check formatting
  - Must pass checkstyle validation
  - Delete feature branches after merging
- **Commits**: Treated as snapshot builds, versions use -SNAPSHOT suffix

## Release and Publishing

- **Profiles**: `package` profile for snapshot publishing, `release` profile for releases
- **Publishing**: Uses central-publishing-maven-plugin to Maven Central
- **Excluded Artifacts**: cqf-fhir-benchmark and docs are not published
- **Signing**: GPG signing required for releases (maven-gpg-plugin)
- **Versioning**: Release enforcer plugin prevents SNAPSHOT dependencies in releases

## Documentation

- Main documentation site: www.cqframework.org/clinical-reasoning
- Documentation source: docs/src/doc (MkDocs Material)
- API documentation: Generated via maven-javadoc-plugin
- Key docs: operations.md, developer-guide.md, cql.md

## Related Projects

- Clinical Quality Language: https://github.com/cqframework/clinical_quality_language
- HAPI FHIR: https://github.com/hapifhir/hapi-fhir
- CQL VS Code Extension: https://marketplace.visualstudio.com/items?itemName=cqframework.cql

## Known Issues and Design Notes

### External Terminology Versioning (Active Issue - 2026-02-13)

When running `ImplementationGuide/$data-requirements`, external CodeSystems (SNOMED, ICD-10-CM, etc.) are being resolved from dependency packages (e.g., hl7.terminology) and their package versions are included in expansion parameters. This may not be the desired behavior, as external terminologies should typically be resolved by the terminology server without version constraints.

**See:** [EXTERNAL_TERMINOLOGY_VERSIONING.md](EXTERNAL_TERMINOLOGY_VERSIONING.md) for detailed analysis, architecture flow, and potential solutions.

**Status:** Awaiting feedback on solution approach. The issue affects ImplementationGuide packaging, ValueSet expansion, and terminology server integration.

**Key files involved:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/visitor/DataRequirementsVisitor.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/visitor/InferManifestParametersVisitor.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/implementationguide/KeyElementFilter.java`

## License

Apache License 2.0 - All contributions licensed under Apache 2.0
