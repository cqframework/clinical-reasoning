# Measure Validation Architecture

## Overview

Measure validation ensures that prerequisites for CQL evaluation are met **before** evaluation begins, avoiding wasted compute and providing structured, actionable error feedback. Validation runs in two layers:

1. **First-pass structural validation** (`R4MeasureDefBuilder.triggerFirstPassValidation`) — existing checks that throw `InvalidRequestException` for structural problems in the FHIR Measure resource itself.
2. **Pre-evaluation validation** (`CompositeMeasureDefValidator`) — a composable validator framework that probes the repository for library resolution, ValueSet availability, parameter configuration, and expression reference integrity.

Both layers execute before CQL evaluation in `R4MeasureProcessor`.

## Pipeline Position

```
R4MeasureProcessor.evaluateMultiMeasuresWithCqlEngine():

  checkMeasureLibrary(measure)                           ← Quick library presence check
  R4MeasureDefBuilder.triggerFirstPassValidation(measures) ← Structural checks (throws)
  ──── NEW VALIDATION LAYER ────
  for each measure:
    MeasureDef = R4MeasureDefBuilder.build(measure)
    CompositeMeasureDefValidator.validate(context)        ← Repository-probing checks
  ──── END VALIDATION ────
  getMultiLibraryIdMeasureEngineDetails(measures)         ← Full library resolution
  resolveParameterMap(parameters)
  MeasureEvaluationResultHandler.getEvaluationResults()   ← CQL evaluation
```

## Validation Result Model (domain core)

All classes in `org.opencds.cqf.fhir.cr.measure.common`:

**`ValidationSeverity`** — Enum: `ERROR`, `WARNING`, `INFO`

**`ValidationIssue`** — Record carrying a single issue:
- `severity` — blocking errors vs informational warnings
- `code` — machine-readable identifier (e.g. `"LIBRARY_NOT_FOUND"`)
- `description` — human-readable problem statement
- `remediation` — actionable fix guidance
- `location` — optional path within the Measure (e.g. `"Measure.library"`)

**`ValidationResult`** — Mutable accumulator for issues:
- `addIssue(ValidationIssue)`, `merge(ValidationResult)`
- `hasErrors()`, `hasWarnings()`, `isEmpty()`
- `getBlockingErrors()` — filters for ERROR severity

**`MeasureValidationException`** — `extends RuntimeException`, thrown when validation produces blocking errors. Carries the full `ValidationResult` for programmatic inspection via `getValidationResult()`.

## Validator Interface

**`MeasureDefValidator`** — Strategy interface:
```java
public interface MeasureDefValidator {
    ValidationResult validate(MeasureDefValidationContext context);
}
```

**`MeasureDefValidationContext`** — Record providing:
- `measureDef()` — the built domain MeasureDef
- `measure()` — the raw FHIR Measure resource (`IBaseResource`)
- `repository()` — `IRepository` for probing resource availability
- `parameters()` — user-supplied parameters map (empty if none)

**`CompositeMeasureDefValidator`** — Runs an ordered list of validators, merges all results into one `ValidationResult`. All validators execute regardless of earlier failures.

## Individual Validators

### R4CqlLibraryValidator (`r4/`)
- **Checks**: Library canonical URLs resolve in the repository
- **Follows**: Transitive `relatedArtifact` (type=depends-on) dependencies
- **Produces**: `LIBRARY_NOT_FOUND` (ERROR)
- **How**: `repository.search(Bundle.class, Library.class, Searches.byCanonical(url), null)`

### R4ValueSetAvailabilityValidator (`r4/`)
- **Checks**: ValueSets referenced in Library `dataRequirement.codeFilter.valueSet` exist
- **Produces**: `VALUESET_UNAVAILABLE` (WARNING) — external terminology services may resolve at runtime
- **Does NOT** trigger expansion; existence check only

### R4ParameterConfigurationValidator (`r4/`, extends `ParameterConfigurationValidator`)
- **Checks**: Required Library parameters (min > 0) are present; flags unknown parameters
- **Reads**: `Library.parameter` definitions (name, type, use, min)
- **Produces**: `MISSING_REQUIRED_PARAMETER` (ERROR), `UNKNOWN_PARAMETER` (WARNING)
- **Skips**: Well-known operation parameters like `"Measurement Period"`

### R4ExpressionReferenceValidator (`r4/`)
- **Checks**: CQL expression names in populations, stratifiers, SDEs exist in the primary library
- **Parses**: Library ELM JSON content (`application/elm+json`) via Jackson, falls back to CQL text
- **Produces**: `EXPRESSION_NOT_FOUND` (WARNING) — expressions may exist in included libraries
- **ELM path**: `library.statements.def[].name`

## First-Pass Structural Validation (existing)

`R4MeasureDefBuilder.triggerFirstPassValidation(List<Measure>)` checks:

| Check | Method | Error |
|---|---|---|
| Measure has ID | `checkId(measure)` | `InvalidRequestException` |
| Population IDs unique per group | `validateUniquePopulationIds()` | `InvalidRequestException` |
| SDE usage codes present | `checkSDEUsage()` | `InvalidRequestException` |
| Improvement notation valid | `validateMeasureImprovementNotation()` | `InvalidRequestException` |
| Ratio CV structure (2 observations, criteria refs) | `validateRatioContinuousVariableIfApplicable()` | `InvalidRequestException` |

Additional checks during `R4MeasureDefBuilder.build()`:
- All Elements have IDs (`checkId()`)
- Stratifiers have either criteria OR components, not both
- Criteria references for MEASUREOBSERVATION populations resolve to group population IDs
- Population basis and improvement notation coalescing (group-level overrides measure-level)

## OperationOutcome Integration

`R4MeasureReportBuilderContext` surfaces validation issues as contained `OperationOutcome` resources:

**Existing**: `addOperationOutcomes()` — converts `MeasureDef.errors()` (runtime evaluation errors) to `OperationOutcome` with `IssueSeverity.ERROR` and `IssueType.EXCEPTION`.

**New**: `addValidationOutcomes(ValidationResult)` — converts `ValidationIssue` objects with:

| ValidationSeverity | OperationOutcome.IssueSeverity |
|---|---|
| ERROR | ERROR |
| WARNING | WARNING |
| INFO | INFORMATION |

| Validation Code | IssueType |
|---|---|
| `LIBRARY_NOT_FOUND` | NOTFOUND |
| `VALUESET_UNAVAILABLE` | NOTFOUND |
| `EXPRESSION_NOT_FOUND` | NOTFOUND |
| `MISSING_REQUIRED_PARAMETER` | REQUIRED |
| `UNKNOWN_PARAMETER` | VALUE |

Remediation text goes in `issue.diagnostics`. Error code goes in `issue.details.coding` with system `http://opencds.org/fhir/measure-validation`.

## Key Files

### Domain Core (`cqf-fhir-cr/.../measure/common/`)
| File | Type |
|---|---|
| `ValidationSeverity.java` | Enum |
| `ValidationIssue.java` | Record |
| `ValidationResult.java` | Class (mutable accumulator) |
| `MeasureDefValidator.java` | Interface |
| `MeasureDefValidationContext.java` | Record |
| `CompositeMeasureDefValidator.java` | Class |
| `ParameterConfigurationValidator.java` | Base class |
| `MeasureValidationException.java` | Exception |

### R4 Validators (`cqf-fhir-cr/.../measure/r4/`)
| File | Codes Produced |
|---|---|
| `R4CqlLibraryValidator.java` | `LIBRARY_NOT_FOUND` |
| `R4ValueSetAvailabilityValidator.java` | `VALUESET_UNAVAILABLE` |
| `R4ParameterConfigurationValidator.java` | `MISSING_REQUIRED_PARAMETER`, `UNKNOWN_PARAMETER` |
| `R4ExpressionReferenceValidator.java` | `EXPRESSION_NOT_FOUND` |

### Integration Points
| File | Role |
|---|---|
| `R4MeasureProcessor.java` | Constructs composite validator, invokes `runPreEvaluationValidation()` |
| `R4MeasureReportBuilderContext.java` | `addValidationOutcomes()` for OperationOutcome surfacing |
| `R4MeasureDefBuilder.java` | `triggerFirstPassValidation()` for structural checks |

### Tests
| File | Coverage |
|---|---|
| `MeasureDefValidatorTest.java` | Unit tests for all validators, composite, ValidationResult |
| `InvalidMeasureTest.java` | Integration test: `evaluateThrowsErrorWhenLibraryUnavailable()` |

## Adding a New Validator

1. Create a class implementing `MeasureDefValidator` in `r4/` (or `common/` if version-agnostic)
2. Define a `public static final String` error code constant
3. Implement `validate(MeasureDefValidationContext)` — probe the repository, return `ValidationResult`
4. Add the validator to the `CompositeMeasureDefValidator` list in `R4MeasureProcessor` constructor
5. Add the error code to `R4MeasureReportBuilderContext.mapIssueType()` switch
6. Add unit tests in `MeasureDefValidatorTest`

## Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Separate from `triggerFirstPassValidation` | New `MeasureDefValidator` interface | Clean separation; existing method is static, throws directly |
| Structured model vs raw strings | `ValidationIssue` record | Jira DQM-570 requires error code, description, remediation |
| Always-on | No opt-in flag | Validation checks are cheap repository lookups |
| ValueSet not found = WARNING | Not ERROR | External terminology services may resolve at runtime |
| Expression not found = WARNING | Not ERROR | Expressions may exist in included libraries |
| Custom exception | `MeasureValidationException` | Avoids coupling domain code to HAPI `InvalidRequestException` |
