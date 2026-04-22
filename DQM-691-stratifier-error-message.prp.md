# Plan: Fix misleading stratifier error message (DQM-691)

## Context

**Jira:** DQM-691 - "Need better detailed error message for $evaluate-measure operation failure"

When a VALUE stratifier expression returns resource types (e.g. `Encounter`) instead of categorical/scalar types (e.g. `CodeableConcept`, `Boolean`, `String`), the error message is misleading:

```
stratifier expression criteria results for expression: [All Encounters] must fall within accepted types
for population-basis: [Encounter] ... mismatch between total eval result classes: [Encounter, Encounter]
and matching result classes: []
```

The user sees `population-basis: [Encounter]` and `result classes: [Encounter, Encounter]` and thinks they should match. But the actual issue is that VALUE stratifiers must return categorical types for stratification (Boolean, CodeableConcept, Coding, String, Integer, etc.), not resource types like Encounter. The mention of "population-basis" is irrelevant and misleading in this context.

## Changes

### 1. Improve error message in `R4PopulationBasisValidator.java`

**File:** `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4PopulationBasisValidator.java`

**Lines 162-171:** Replace the current error message format string with one that:
- Clearly states VALUE stratifier expressions must return allowed categorical types
- Lists the allowed types (`ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES`) so the user knows what to use
- Removes the misleading "population-basis" reference
- Still shows the actual result types that were returned

New message format (approximate):
```
"value stratifier expression for [%s] returned invalid result type(s): %s for Measure: [%s]. 
VALUE stratifier expressions must return categorical types for stratification, such as: %s. 
Resource types like %s are not valid for VALUE stratifiers. 
If you intend to stratify by resource membership, use a CRITERIA-based stratifier instead."
```

### 2. Add helper to format allowed types

Add a small helper or inline the `ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES` set into the error message so the user can see exactly which types are accepted.

### 3. Update test expectations in `R4PopulationBasisValidatorTest.java`

**File:** `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4PopulationBasisValidatorTest.java`

Update the `expectedExceptionMessage` strings in these test methods to match the new error message format:
- `mismatchBooleanBasisSingleEncounterResult` (line 456-457)
- `mismatchBooleanBasisMultipleEncounterResults` (line 482-483)
- `mismatchBooleanBasisMixedMultipleBooleanAndEncounterResults` (line 507-508)

## Verification

1. Run the unit tests:
   ```
   ./gradlew :cqf-fhir-cr:test --tests "R4PopulationBasisValidatorTest"
   ```
2. Run spotless formatting:
   ```
   ./gradlew :cqf-fhir-cr:spotlessApply
   ```
3. Run the full module tests to check for regressions:
   ```
   ./gradlew :cqf-fhir-cr:test
   ```
