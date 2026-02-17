# Measure & MultiMeasure Integration Testing Framework

## Overview

This project has a fluent Given/When/Then testing framework for measure evaluation integration tests. There are two entry points:

- **`Measure`** (`cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/Measure.java`): Single-measure evaluation tests
- **`MultiMeasure`** (`cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/MultiMeasure.java`): Multi-measure evaluation tests

Both use a **Given/When/Then** BDD pattern with fluent **Selected*** assertion classes organized in two parallel hierarchies:
- **`selected/report/`**: MeasureReport (FHIR resource) assertions - post-scoring state
- **`selected/def/`**: MeasureDef (internal model) assertions - pre-scoring state

## Architecture

### Entry Points

```
Measure.given()          -> Measure.Given -> Measure.When -> Measure.Then
MultiMeasure.given()     -> MultiMeasure.Given -> MultiMeasure.When -> MultiMeasure.Then
```

### Core Base Types (defined in Measure.java)

```java
// Generic parent interface for navigating back up the fluent chain
public interface ChildOf<T> { T up(); }

// Generic interface for getting the wrapped value
public interface SelectedOf<T> { T value(); }

// Base class all Selected* classes extend - wraps a value with parent navigation
public static class Selected<T, P> implements SelectedOf<T>, ChildOf<P> {
    protected final P parent;
    protected final T value;
}

// Functional interfaces for custom validation/selection
public interface Validator<T> { void validate(T value); }
public interface Selector<T, S> { T select(S from); }
```

### Return Records (test infrastructure)

- **`MeasureDefAndR4MeasureReport`**: Pairs a single `MeasureDef` with a single `MeasureReport` (used by `Measure`)
- **`MeasureDefAndR4ParametersWithMeasureReports`**: Pairs `List<MeasureDef>` with `Parameters` containing bundled `MeasureReport`s (used by `MultiMeasure`)

## Given Phase - Test Setup

### Measure.Given

```java
Measure.given()                              // Default: applyScoringSetMembership=true
Measure.given(false)                         // Override applyScoringSetMembership
    .repositoryFor("MinimalMeasureEvaluation")  // Load IgRepository from test resources path
    .repository(customRepo)                     // Or supply a custom IRepository
    .evaluationOptions(options)                 // Override MeasureEvaluationOptions
```

- Default options: in-memory filtering, naive valueset expansion
- Repository path resolves to: `src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/{repositoryPath}`

### MultiMeasure.Given

```java
MultiMeasure.given()
    .repositoryFor("MinimalMeasureEvaluation")
    .repository(customRepo)
    .evaluationOptions(options)
    .serverBase("http://custom-server")         // MultiMeasure-only: custom server base
```

## When Phase - Configure & Execute Evaluation

### Measure.When

```java
.when()
    .measureId("ProportionBooleanAllPopulations")   // Measure resource ID
    .periodStart("2024-01-01")                       // ISO date string
    .periodEnd("2024-12-31")                         // ISO date string
    .subject("Patient/patient-9")                    // Optional: individual subject
    .reportType("population")                        // Optional: population|individual|subject-list
    .additionalData(bundle)                          // Optional: additional Bundle data
    .parameters(params)                              // Optional: Parameters resource
    .practitioner("Practitioner/1")                  // Optional: practitioner reference
    .productLine("HMO")                              // Optional: product line
    .evaluate()                                      // Execute the evaluation (returns When)
```

### MultiMeasure.When

```java
.when()
    .measureId("MeasureA")                           // Add measure by ID (can call multiple times)
    .measureId("MeasureB")
    .measureUrl("http://example.com/Measure/X")      // Or by canonical URL
    .measureIdentifier("test123")                    // Or by identifier
    .periodStart("2024-01-01")
    .periodEnd("2024-12-31")
    .subject("Patient/patient-1")
    .reportType("population")
    .additionalData(bundle)
    .parameters(params)
    .productLine("HMO")
    .reporter("Organization/1")                      // MultiMeasure-only: reporter reference
    .evaluate()
```

## Then Phase - Assertions

### Measure.Then

The `Then` class provides two assertion pathways:

```java
.then()
    // Pre-scoring assertions on internal MeasureDef model
    .def()          // -> SelectedMeasureDef<Then>
        ...
    .up()           // Back to Then

    // Post-scoring assertions on FHIR MeasureReport
    .report()       // -> SelectedMeasureReport
        ...
    .up()           // Back to Then

    // Backward-compatible shortcuts (delegate to report()):
    .firstGroup()
    .group("id")
    .hasGroupCount(1)
    .hasMeasureVersion("1.0")
    .hasEvaluatedResourceCount(5)
    // ... many more delegating methods
```

### MultiMeasure.Then

```java
.then()
    // Pre-scoring assertions on List<MeasureDef>
    .defs()         // -> SelectedMeasureDefCollection<Then>
        .hasCount(3)
        .first()    // -> SelectedMeasureDef
        .byMeasureUrl("http://...").first()
        .byMeasureId("MyMeasure").first()
        ...
    .up()

    // Post-scoring assertions on Parameters with bundled MeasureReports
    .report()       // -> MultiMeasure.SelectedReport
        .hasBundleCount(1)
        .hasMeasureReportCount(3)
        .hasMeasureReportCountPerUrl(2, "http://...")
        ...

    // Backward-compatible shortcuts:
    .hasBundleCount(1)
    .hasMeasureReportCount(3)
    .measureReport("http://example.com/Measure/X")    // -> MultiMeasure.SelectedMeasureReport
    .measureReport("http://...", "Patient/1")          // By URL + subject
    .getFirstMeasureReport()                           // -> MultiMeasure.SelectedMeasureReport
```

## R4 Selected Report Hierarchy (post-scoring FHIR assertions)

All classes in `selected/report/` extend `Measure.Selected<T, P>`.

### SelectedMeasureReport
**Package**: `org.opencds.cqf.fhir.cr.measure.r4.selected.report`
**Wraps**: `MeasureReport`
**Parent**: `Measure.Then`

Key methods:
```java
// Navigation
.firstGroup()                              -> SelectedMeasureReportGroup
.group("groupId")                          -> SelectedMeasureReportGroup
.group(0)                                  -> SelectedMeasureReportGroup (by index)
.evaluatedResource("Patient/1")            -> SelectedMeasureReportReference
.extension("supplementalDataId")           -> SelectedMeasureReportExtension
.extensionByValueReference("Encounter/1")  -> SelectedMeasureReportExtension
.containedByValue("M")                     -> SelectedMeasureReportContained
.containedByCoding("M")                    -> SelectedMeasureReportContained

// Assertions (all return self for chaining)
.hasGroupCount(1)
.hasStatus(MeasureReportStatus.COMPLETE)
.hasMeasureUrl("http://...")
.hasMeasureVersion("1.0")
.hasEvaluatedResourceCount(5)
.evaluatedResourceHasNoDuplicateReferences()
.hasContainedResourceCount(3)
.hasContainedResource(predicate)
.hasContainedOperationOutcome()
.hasContainedOperationOutcomeMsg("expected error")
.hasReportType("Summary")
.hasSubjectReference("Patient/1")
.hasPatientReference("Patient/1")
.hasPeriodStart(date)
.hasPeriodEnd(date)
.hasEmptySubject()
.hasMeasureReportDate()
.hasMeasureReportPeriod()
.hasNoReportLevelImprovementNotation()
.hasReportLevelImprovementNotation()
.improvementNotationCode("increase")
.hasImprovementNotation("increase")
.hasExtension("url", count)
.containedObservationsHaveMatchingExtension()
.subjectResultsValidation()
.subjectResultsHaveResourceType("Patient")
.containedListHasCorrectResourceType("Patient")
.passes(validator)
.logReportJson()                           // Logs JSON to SLF4J logger
```

### SelectedMeasureReportGroup
**Wraps**: `MeasureReportGroupComponent`
**Parent**: `SelectedMeasureReport`

```java
// Navigation
.population("numerator")                   -> SelectedMeasureReportPopulation
.populationId("pop-id")                    -> SelectedMeasureReportPopulation
.firstPopulation()                         -> SelectedMeasureReportPopulation
.firstStratifier()                         -> SelectedMeasureReportStratifier
.stratifierById("strat-id")               -> SelectedMeasureReportStratifier

// Assertions
.hasScore("0.75")
.hasMeasureScore(true)
.hasPopulationCount(4)
.hasStratifierCount(2)
.hasImprovementNotationExt("increase")
.hasNoImprovementNotationExt()
.hasDateOfCompliance()
```

### SelectedMeasureReportPopulation
**Wraps**: `MeasureReportGroupPopulationComponent`
**Parent**: `SelectedMeasureReportGroup`

```java
.hasCount(10)
.hasCode(MeasurePopulationType.NUMERATOR)
.hasSubjectResults()
.hasAggregationResultsExtensionValue(42.5)
.hasNoAggregationResultsExtensionValue()
.hasAggregateMethodExtension(ContinuousVariableObservationAggregateMethod.SUM)
.hasNoAggregateMethodExtension()
.hasCriteriaReferenceExtension("numerator-pop-id")  // With cross-validation
.hasNoCriteriaReferenceExtension()
.assertNoSupportingEvidence()
.getPopulationExtension("EvidenceName")    -> SelectedMeasureReportPopulationExt
.passes(validator)
```

### SelectedMeasureReportPopulationExt
**Wraps**: `Extension` (supporting evidence)
**Parent**: `SelectedMeasureReportPopulation`

Supports scalar, list, tuple, null, and empty list value assertions:
```java
// Metadata
.hasName("MyEvidence")
.hasDescription("Description text")
.hasCode(codeableConcept)

// Scalar values
.hasBooleanValue(true)
.hasIntegerValue(42)
.hasStringValue("hello")
.hasDecimalValue(3.14)
.hasResourceIdValue("Patient/1")
.hasPeriodValue(start, end)

// List values (repeated "value" slices)
.hasListBooleanItem(true)
.hasListIntegerItem(42)
.hasListStringItem("hello")
.hasListDecimalItem(3.14)
.hasListResourceIdItem("Patient/1")
.hasListPeriodItem(start, end)

// Tuple assertions (nested field extensions)
.hasTupleBoolean("fieldName", true)
.hasTupleInteger("fieldName", 42)
.hasTupleString("fieldName", "value")
.hasTupleDecimal("fieldName", 3.14)
.hasTupleResourceId("fieldName", "Patient/1")
.hasTuplePeriod("fieldName", period)
.hasTupleListStringItem("fieldName", "item")
.hasTupleListIntegerItem("fieldName", 42)
.hasTupleListBooleanItem("fieldName", true)
.hasTupleListResourceIdItem("fieldName", "id")

// Special result types
.hasNullResult()       // data-absent-reason=unknown
.hasEmptyListResult()  // cqf-isEmptyList=true
```

### SelectedMeasureReportStratifier
**Wraps**: `MeasureReportGroupStratifierComponent`
**Parent**: `SelectedMeasureReportGroup`

```java
// Navigation
.firstStratum()                            -> SelectedMeasureReportStratum
.stratum("male")                           -> SelectedMeasureReportStratum (by text value)
.stratum(codeableConcept)                  -> SelectedMeasureReportStratum (by CodeableConcept)
.stratumByText("male")                     -> SelectedMeasureReportStratum
.stratumByPosition(1)                      -> SelectedMeasureReportStratum (1-based)
.stratumByComponentValueText("value")      -> SelectedMeasureReportStratum
.stratumByComponentCodeText("code")        -> SelectedMeasureReportStratum

// Assertions
.hasStratumCount(4)
.hasStratum("male")
.hasCodeText("Gender")
```

### SelectedMeasureReportStratum
**Wraps**: `StratifierGroupComponent`
**Parent**: `SelectedMeasureReportStratifier`

```java
// Navigation
.firstPopulation()                         -> SelectedMeasureReportStratumPopulation
.population("numerator")                   -> SelectedMeasureReportStratumPopulation
.populationId("pop-id")                    -> SelectedMeasureReportStratumPopulation
.firstStratumComponent()                   -> SelectedMeasureReportStratumComponent
.stratumComponent(0)                       -> SelectedMeasureReportStratumComponent (by index)
.stratumComponentWithCodeText("Age")       -> SelectedMeasureReportStratumComponent

// Assertions
.hasScore("0.5")
.hasValue("male")
.hasPopulationCount(4)
.hasComponentStratifierCount(2)
```

### SelectedMeasureReportStratumPopulation
**Wraps**: `StratifierGroupPopulationComponent`
**Parent**: `SelectedMeasureReportStratum`

```java
.hasCount(5)
.hasCode(MeasurePopulationType.NUMERATOR)
.hasStratumPopulationSubjectResults()
.hasNoStratumPopulationSubjectResults()
```

### SelectedMeasureReportStratumComponent
**Wraps**: `StratifierGroupComponentComponent`
**Parent**: `SelectedMeasureReportStratum`

```java
.hasCodeText("Gender")
.hasValueText("male")
```

### SelectedMeasureReportReference
**Wraps**: `Reference`
**Parent**: `SelectedMeasureReport`

```java
.hasPopulations("numerator", "denominator")
.referenceHasExtension("numerator")
.hasEvaluatedResourceReferenceCount(3)
.hasNoDuplicateExtensions()
```

### SelectedMeasureReportContained
**Wraps**: `Resource`
**Parent**: `SelectedMeasureReport`

```java
.observationHasExtensionUrl()
.observationHasSDECoding()
.observationHasCode("gender")
.observationCount(5)
```

### SelectedMeasureReportExtension
**Wraps**: `Extension`
**Parent**: `SelectedMeasureReport`

```java
.extensionHasSDEUrl()
.extensionHasSDEId("sde-race")
```

## R4 Selected Def Hierarchy (pre-scoring internal model assertions)

All classes in `selected/def/` extend `Measure.Selected<T, P>`.

### SelectedMeasureDef
**Wraps**: `MeasureDef`
**Parent**: generic `<P>` (typically `Measure.Then` or `MultiMeasure.Then`)

```java
// Navigation
.firstGroup()                              -> SelectedMeasureDefGroup
.group("groupId")                          -> SelectedMeasureDefGroup (by ID)
.group(0)                                  -> SelectedMeasureDefGroup (by index)

// Assertions
.hasNoErrors()
.hasErrors(2)
.hasError("substring of error message")
.hasGroupCount(1)
.hasMeasureId("MyMeasure")
.hasMeasureUrl("http://...")
.hasMeasureVersion("1.0")
.measureDef()                              // Raw MeasureDef access
```

### SelectedMeasureDefCollection
**Wraps**: `List<MeasureDef>`
**Parent**: generic `<P>` (used by `MultiMeasure.Then`)

```java
.hasCount(3)
.first()                                   -> SelectedMeasureDef
.get(0)                                    -> SelectedMeasureDef (by index)
.byMeasureUrl("http://...")                -> SelectedMeasureDefCollection (filtered)
.byMeasureId("MyMeasure")                 -> SelectedMeasureDefCollection (filtered)
.allSatisfy(measureDef -> { ... })         // Consumer-based assertion
.list()                                    // Raw List<MeasureDef> access
```

### SelectedMeasureDefGroup
**Wraps**: `GroupDef`
**Parent**: `SelectedMeasureDef`

```java
// Navigation
.population("numerator")                   -> SelectedMeasureDefPopulation (by code)
.populationById("pop-id")                 -> SelectedMeasureDefPopulation
.firstPopulation()                         -> SelectedMeasureDefPopulation
.stratifier("strat-id")                   -> SelectedMeasureDefStratifier
.firstStratifier()                         -> SelectedMeasureDefStratifier

// Assertions
.hasPopulationCount(6)
.hasStratifierCount(1)
.hasScore(0.75)                            // Double (numeric, pre-scoring is null)
.hasNullScore()                            // Pre-scoring assertion
.hasMeasureScoring(MeasureScoring.PROPORTION)
.hasPopulationBasis("boolean")
.isBooleanBasis()                          // Shortcut for hasPopulationBasis("boolean")
.hasGroupId("group-1")
.groupDef()                                // Raw GroupDef access
```

### SelectedMeasureDefPopulation
**Wraps**: `PopulationDef`
**Parent**: `SelectedMeasureDefGroup`

```java
// Subject & count assertions
.hasSubjectCount(7)                        // Number of unique subjects
.hasCount(7)                               // Uses PopulationDef.getCount() - single source of truth
.hasSubjects("Patient/1", "Patient/2")     // Specific subjects present
.doesNotHaveSubject("Patient/excluded")
.hasEvaluatedResourceCount(15)

// Resource-per-subject
.subjectHasResourceCount("Patient/1", 2)

// Aggregation
.hasAggregationResult(42.5)
.hasNoAggregationResult()
.hasAggregateMethod(ContinuousVariableObservationAggregateMethod.SUM)
.hasAggregateMethodNA()
.hasNoAggregateMethod()

// Metadata
.hasType(MeasurePopulationType.NUMERATOR)
.hasExpression("Numerator")
.hasCriteriaReference("numerator-pop-id")  // With cross-validation against parent group
.hasNoCriteriaReference()
.hasPopulationId("pop-1")
.isBooleanBasis()

// Supporting evidence
.getExtDef("EvidenceName")                -> SelectedMeasureDefPopulationExtension
.assertNoSupportingEvidenceResults()

.populationDef()                           // Raw PopulationDef access
```

### SelectedMeasureDefPopulationExtension
**Wraps**: `SupportingEvidenceDef`
**Parent**: `SelectedMeasureDefPopulation`

```java
.extensionDefHasResults()
.extensionDef()                            // Raw SupportingEvidenceDef access
```

### SelectedMeasureDefStratifier
**Wraps**: `StratifierDef`
**Parent**: `SelectedMeasureDefGroup`

```java
// Navigation
.firstStratum()                            -> SelectedMeasureDefStratum
.stratum(0)                                -> SelectedMeasureDefStratum (by index)
.stratumByValue("male")                    -> SelectedMeasureDefStratum (by value text)

// Assertions
.hasStratumCount(4)
.hasComponentCount(2)
.hasExpression("Gender")
.isComponentStratifier()
.hasResultForSubject("Patient/1")
.hasResultCount(10)
.hasStratifierId("strat-1")
.stratifierDef()                           // Raw StratifierDef access
```

### SelectedMeasureDefStratum
**Wraps**: `StratumDef`
**Parent**: `SelectedMeasureDefStratifier`

```java
// Navigation
.population("numerator")                   -> SelectedMeasureDefStratumPopulation
.populationById("pop-id")                 -> SelectedMeasureDefStratumPopulation
.firstPopulation()                         -> SelectedMeasureDefStratumPopulation

// Assertions
.hasPopulationCount(4)
.hasSubjectCount(3)
.hasScore(0.5)                             // Double score
.hasNullScore()                            // Pre-scoring
.hasValueDef("male")
.hasNoPopulationOfType(MeasurePopulationType.NUMERATOR)
.isComponentStratum()
.stratumDef()                              // Raw StratumDef access
```

### SelectedMeasureDefStratumPopulation
**Wraps**: `StratumPopulationDef`
**Parent**: `SelectedMeasureDefStratum`

```java
.hasCount(5)
.hasSubjectCount(5)
.hasSubjects("Patient/1", "Patient/2")
.hasResourceCount(8)
.hasPopulationId("pop-1")
.hasType(MeasurePopulationType.NUMERATOR)
.hasAggregateMethod(ContinuousVariableObservationAggregateMethod.SUM)
.hasAggregateMethodNA()
.hasNoAggregateMethod()
.isBooleanBasis()
.isResourceBasis()
.stratumPopulationDef()                    // Raw StratumPopulationDef access
```

## MultiMeasure-Specific Selected Classes (inline in MultiMeasure.java)

MultiMeasure has its own Selected classes for MeasureReport-level assertions that differ from the standalone `selected/report/` classes:

### MultiMeasure.SelectedReport
**Wraps**: `Parameters` (containing bundled MeasureReports)

```java
.hasBundleCount(1)
.hasMeasureReportCount(3)
.hasMeasureReportCountPerUrl(2, "http://...")
.measureReport("http://example.com/Measure/X")     -> MultiMeasure.SelectedMeasureReport
.measureReport("http://...", "Patient/1")           -> MultiMeasure.SelectedMeasureReport
.getFirstMeasureReport()                            -> MultiMeasure.SelectedMeasureReport
.getSecondMeasureReport()                           -> MultiMeasure.SelectedMeasureReport
```

### MultiMeasure.SelectedMeasureReport
**Wraps**: `MeasureReport`
**Parent**: `MultiMeasure.SelectedReport`

```java
// Navigation
.firstGroup()                              -> MultiMeasure.SelectedGroup
.group("id")                               -> MultiMeasure.SelectedGroup
.evaluatedResource("Patient/1")            -> MultiMeasure.SelectedReference

// Assertions
.measureReportMatches("http://...")
.measureReportSubjectMatches("Patient/1")
.measureReportTypeIndividual()
.hasMeasure("http://...")
.hasEvaluatedResourceCount(5)
.hasMeasureReportStatus(MeasureReportStatus.COMPLETE)
.hasContainedOperationOutcome()
.hasContainedOperationOutcomeMsg("error msg")
.hasMeasureVersion("1.0")
.hasContainedResourceCount(3)
.hasContainedResource(predicate)
.hasExtension("url", count)
.hasSubjectReference("Patient/1")
.hasReportType("Summary")
.hasReporter("Organization/1")
.hasPeriodStart(date)
.hasPeriodEnd(date)
```

### MultiMeasure.SelectedGroup / SelectedPopulation / SelectedStratifier / SelectedStratum / SelectedStratumPopulation

These mirror the `selected/report/` hierarchy but are defined inline in `MultiMeasure.java` with simpler implementations.

## DSTU3 Support

DSTU3 has a parallel but smaller set of `selected/def/` classes:

- `dstu3/selected/def/SelectedMeasureDef<P>`
- `dstu3/selected/def/SelectedMeasureDefGroup<P>`
- `dstu3/selected/def/SelectedMeasureDefPopulation<P>`

These do NOT extend `Measure.Selected` (they are standalone). They provide similar assertions (hasNoErrors, hasCount, hasSubjectCount, etc.) but with simpler APIs.

## MeasureValidationUtils

**Package**: `org.opencds.cqf.fhir.cr.measure.r4`

Static helper methods used by both `selected/report/` classes and `MultiMeasure` inline classes:

```java
MeasureValidationUtils.validateGroupScore(group, "0.75")      // BigDecimal or String
MeasureValidationUtils.validatePopulation(population, 10)
MeasureValidationUtils.validateStratumScore(stratum, "0.5")
MeasureValidationUtils.validateStratifier(stratifier, "male", "numerator", 5)
MeasureValidationUtils.validateEvaluatedResourceExtension(refs, "Patient/1")
```

## Typical Test Pattern

### Single Measure Test

```java
private static final Given GIVEN = Measure.given().repositoryFor("MeasureTest");

@Test
void proportionBooleanPopulation() {
    GIVEN.when()
        .measureId("ProportionBooleanAllPopulations")
        .evaluate()
    .then()
        // Pre-scoring internal state
        .def()
            .hasNoErrors()
            .firstGroup()
                .population("initial-population").hasCount(10).up()
                .population("denominator").hasCount(10).up()
                .population("numerator").hasCount(2).up()
                .hasScore(0.3333333333333333)
            .up()
        .up()
        // Post-scoring FHIR MeasureReport
        .report()
            .firstGroup()
                .population("initial-population").hasCount(10).up()
                .population("denominator").hasCount(10).up()
                .population("numerator").hasCount(2).up()
                .hasScore("0.3333333333333333")  // String for FHIR BigDecimal
            .up()
        .report();
}
```

### Multi-Measure Test

```java
private static final MultiMeasure.Given GIVEN = MultiMeasure.given().repositoryFor("MinimalMeasureEvaluation");

@Test
void multiMeasurePopulation() {
    GIVEN.when()
        .measureId("MeasureA")
        .measureId("MeasureB")
        .periodStart("2024-01-01")
        .periodEnd("2024-12-31")
        .reportType("population")
        .evaluate()
    .then()
        .hasBundleCount(1)
        .hasMeasureReportCount(2)
        .defs()
            .hasCount(2)
            .first().hasNoErrors().up()
        .up()
        .measureReport("http://example.com/Measure/MeasureA")
            .hasReportType("Summary")
            .firstGroup()
                .population("numerator").hasCount(7).up()
                .hasScore("0.75")
            .up()
        .up();
}
```

## Key Differences: def() vs report()

| Aspect | `.def()` (MeasureDef) | `.report()` (MeasureReport) |
|--------|----------------------|---------------------------|
| **Timing** | Pre/post scoring internal state | Post-scoring FHIR resource |
| **Score type** | `Double` (`hasScore(0.75)`) | `String` (`hasScore("0.75")`) |
| **Counts** | Uses `PopulationDef.getCount()` | Uses `MeasureReportGroupPopulationComponent.getCount()` |
| **Subjects** | `hasSubjects("Patient/1")` | Not directly available |
| **Navigation** | `population("numerator")` by code | `population("numerator")` by coding |
| **Null score** | `hasNullScore()` | `hasMeasureScore(false)` |
| **Errors** | `hasNoErrors()`, `hasError("msg")` | `hasContainedOperationOutcome()` |

## File Locations

```
cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/
  r4/
    Measure.java                              # Single-measure Given/When/Then + base types
    MultiMeasure.java                         # Multi-measure Given/When/Then + inline Selected classes
    MeasureValidationUtils.java               # Static validation helpers
    selected/
      def/
        SelectedMeasureDef.java               # MeasureDef assertions
        SelectedMeasureDefCollection.java     # List<MeasureDef> assertions (MultiMeasure)
        SelectedMeasureDefGroup.java          # GroupDef assertions
        SelectedMeasureDefPopulation.java     # PopulationDef assertions
        SelectedMeasureDefPopulationExtension.java  # SupportingEvidenceDef assertions
        SelectedMeasureDefStratifier.java     # StratifierDef assertions
        SelectedMeasureDefStratum.java        # StratumDef assertions
        SelectedMeasureDefStratumPopulation.java  # StratumPopulationDef assertions
      report/
        SelectedMeasureReport.java            # MeasureReport assertions
        SelectedMeasureReportGroup.java       # Group assertions
        SelectedMeasureReportPopulation.java  # Population assertions
        SelectedMeasureReportPopulationExt.java   # Supporting evidence extension assertions
        SelectedMeasureReportStratifier.java  # Stratifier assertions
        SelectedMeasureReportStratum.java     # Stratum assertions
        SelectedMeasureReportStratumPopulation.java  # Stratum population assertions
        SelectedMeasureReportStratumComponent.java   # Stratum component assertions
        SelectedMeasureReportReference.java   # Evaluated resource reference assertions
        SelectedMeasureReportContained.java   # Contained resource assertions
        SelectedMeasureReportExtension.java   # SDE extension assertions
  dstu3/
    selected/
      def/
        SelectedMeasureDef.java               # DSTU3 MeasureDef assertions (standalone)
        SelectedMeasureDefGroup.java          # DSTU3 GroupDef assertions (standalone)
        SelectedMeasureDefPopulation.java     # DSTU3 PopulationDef assertions (standalone)

cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/
  MeasureDefAndR4MeasureReport.java           # Record pairing MeasureDef + MeasureReport
  MeasureDefAndR4ParametersWithMeasureReports.java  # Record for multi-measure results
```

## Conventions When Writing New Tests

1. **Use `Measure.given()` for single-measure tests**, `MultiMeasure.given()` for multi-measure tests
2. **Always assert on both `def()` and `report()`** to verify internal state and FHIR output
3. **Use `up()` to navigate back** to the parent in the fluent chain
4. **Score types differ**: `Double` for def (e.g., `hasScore(0.75)`), `String` for report (e.g., `hasScore("0.75")`)
5. **Pre-scoring assertions** use `hasNullScore()` on def, `hasMeasureScore(false)` on report
6. **Share Given instances** as `private static final Given` fields in test classes
7. **Repository path** is relative to `src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/`
8. **Test class naming**: `Measure*Test.java` or `MultiMeasureServiceTest.java`
9. **Suppress warnings**: `@SuppressWarnings({"squid:S2699", "squid:S5960", "squid:S1135"})` is standard
10. **JUnit 5 only**: Use `@Test` from `org.junit.jupiter.api.Test`
