# Measure Scoring Architecture

## Overview

This skill provides comprehensive knowledge about how measure scoring works in the CQF Clinical Reasoning FHIR library, including the architectural patterns, scoring flow, and the distinction between measure-level and group-level scoring.

## Core Concepts

### Measure Scoring Types

Measures can have different scoring methodologies defined by the `MeasureScoring` enum:

- **PROPORTION**: Ratio of patients meeting numerator criteria to those in denominator
  - Formula: `(numerator - numeratorExclusion) / (denominator - denominatorExclusion - denominatorException)`
  - Example: 2/6 = 0.3333 (33.33% of patients achieved the quality measure)

- **RATIO**: Ratio of two measure observations
  - Used when comparing two different measurements (e.g., medication events per patient)
  - Can include continuous variable observations for numerator and denominator

- **CONTINUOUS_VARIABLE**: Aggregated observation values
  - Uses aggregate functions (SUM, AVG, MIN, MAX, MEDIAN, COUNT)
  - Example: Average patient age, total hospital days

- **COHORT**: Simple patient count, no scoring
  - Just counts patients meeting criteria
  - No score is calculated

- **COMPOSITE**: Combines multiple component measures
  - Not a directly evaluated scoring type (validation error if used)

## Measure-Level vs Group-Level Scoring

**CRITICAL**: A measure can have EITHER measure-level OR group-level scoring, but NEVER both.

### Measure-Level Scoring (Standard Case)

The most common pattern where scoring is defined at the measure level:

```
Measure
├─ measureScoring: PROPORTION ✓
└─ Group
   ├─ measureScoring: null ✓
   ├─ populations (initial-population, denominator, numerator, etc.)
   └─ score: 0.3333 (calculated)
```

**Characteristics:**
- `MeasureDef.measureScoring` is set (e.g., PROPORTION)
- `GroupDef.measureScoring` is null for all groups
- All groups use the same scoring methodology
- No `cqfm-scoring` extension on MeasureReport groups

**Code validation** (R4MeasureReportBuilder.java:192-193):
```java
if (groupMeasureScoring != null) {
    if (bc.measureDef().hasMeasureScoring()) {
        throw new InternalErrorException("Cannot have both measure and group scoring");
    }
}
```

### Group-Level Scoring (Override Case)

Less common pattern where each group defines its own scoring:

```
Measure
├─ measureScoring: null ✓
└─ Group 1
   ├─ measureScoring: PROPORTION ✓
   ├─ populations
   └─ score: 0.25 (calculated)
└─ Group 2
   ├─ measureScoring: RATIO ✓
   ├─ populations
   └─ score: 1.5 (calculated)
```

**Characteristics:**
- `MeasureDef.measureScoring` is null
- Each `GroupDef.measureScoring` is set
- Groups can have different scoring methodologies
- `cqfm-scoring` extension IS added to each MeasureReport group

**Extension details**:
- URL: `http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-scoring`
- Value: CodeableConcept with Coding (system: `http://terminology.hl7.org/ValueSet/measure-scoring`)
- Set in: `R4MeasureReportBuilder.buildGroup()` via `R4MeasureReportUtils.createGroupScoringExtension()`

## Scoring Architecture

### Key Classes

**Version-Agnostic (cqf-fhir-cr/measure/common/):**

- **MeasureDef**: Container for measure-level metadata
  - Fields: `measureScoring` (nullable), `url`, `version`, `groups`, `sdes`
  - No score field (measures don't get scored, only groups do)
  - Methods: `hasMeasureScoring()`, `measureScoring()`

- **GroupDef**: Container for group-level data and scoring
  - Fields: `measureScoring` (nullable), `score` (nullable), `populations`, `stratifiers`
  - Added by: Claude Sonnet 4.5 on 2025-12-03
  - Methods:
    - `getScore()`: Returns calculated score
    - `setScoreAndAdaptToImprovementNotation(Double, MeasureScoring)`: Sets score with improvement notation adjustment
    - `getMeasureOrGroupScoring(MeasureDef)`: Resolves effective scoring type

- **MeasureReportDefScorer**: Primary scorer (version-agnostic)
  - Location: `cqf-fhir-cr/measure/common/MeasureReportDefScorer.java`
  - Purpose: Calculates and sets scores on Def objects
  - Key method: `score(String measureUrl, MeasureDef measureDef)`
  - **Integration**: As of 2025-12-16, this is the ONLY scorer used internally
  - Operates on Def objects before FHIR report generation

**Version-Specific (cqf-fhir-cr/measure/r4/):**

- **R4MeasureReportBuilder**: FHIR R4 report builder
  - Method: `copyScoresFromDef()` copies pre-computed scores from Def to FHIR MeasureReport
  - Replaced old pattern of scoring in builders

- **R4MeasureReportScorer**: **DEPRECATED for internal use** as of 2025-12-16
  - Retained ONLY for external callers
  - Provides R4-specific helpers for stratifier population counts
  - Internal usage replaced by MeasureReportDefScorer

## Scoring Resolution Logic

The `getMeasureOrGroupScoring(MeasureDef)` method determines which scoring type to use:

```java
public MeasureScoring getMeasureOrGroupScoring(MeasureDef measureDef) {
    // Check measure first (most common case)
    if (measureDef.hasMeasureScoring()) {
        return measureDef.measureScoring();
    }

    // If no measure scoring, check group
    if (hasMeasureScoring()) {
        return measureScoring();
    }

    // Error if neither
    throw new InternalErrorException("Must have scoring at measure or group level");
}
```

**Resolution order:**
1. If `MeasureDef.measureScoring` is set → use measure's scoring
2. Else if `GroupDef.measureScoring` is set → use group's scoring
3. Else → throw error (scoring must be defined somewhere)

## Scoring Workflow

### End-to-End Flow

```
1. Measure Evaluation
   ↓
2. Create MeasureDef + GroupDef objects with population results
   ↓
3. MeasureReportDefScorer.score(measureUrl, measureDef)
   - Iterates through measureDef.groups()
   - For each GroupDef:
     a. Resolve scoring type via getMeasureOrGroupScoring()
     b. Calculate score based on scoring type
     c. MUTATE GroupDef via setScoreAndAdaptToImprovementNotation()
   ↓
4. R4MeasureReportBuilder.buildGroups()
   - Creates FHIR MeasureReport structure
   - If group has own scoring → add cqfm-scoring extension
   ↓
5. R4MeasureReportBuilder.copyScoresFromDef()
   - Copies pre-computed scores from GroupDef to MeasureReport
   - Sets reportGroup.getMeasureScore().setValue(groupDef.getScore())
   ↓
6. Return MeasureReport with scores
```

### Orchestration Point

**MeasureEvaluationResultHandler.java** (lines 88-89):
```java
logger.debug("Scoring MeasureDef using MeasureReportDefScorer for measure: {}", measureDef.url());
measureReportDefScorer.score(measureDef.url(), measureDef);
```

- Scoring happens ONCE after evaluation, before report building
- Scores are stored on Def objects as mutable state
- Builders copy (not recalculate) scores to FHIR resources

## Score Calculation Examples

### Proportion Scoring

```java
// MeasureScoreCalculator.calculateProportionScore()
return (numerator - numeratorExclusion) /
       (denominator - denominatorExclusion - denominatorException);

// Example:
// Numerator: 2, Denominator: 6
// Score: 2/6 = 0.3333333333333333
```

### Ratio Scoring

```java
// Similar to proportion but with different population semantics
// Can include continuous variable observations
// numeratorObservation / denominatorObservation
```

### Continuous Variable Scoring

```java
// Uses aggregation result from measure observation population
final QuantityDef quantityDef = scoreContinuousVariable(measureObsPop);
measureObsPop.setAggregationResult(quantityDef);
return quantityDef != null ? quantityDef.value() : null;
```

### Cohort Scoring

```java
// No score - cohort measures just count patients
return null;
```

## Testing Patterns

### Dual Assertion Structure

Tests verify both internal Def state and FHIR Report output:

```java
.then()
    // MeasureDef assertions (pre-scoring) - verify internal state
    .def()
        .hasNoErrors()
        .hasMeasureScoring(MeasureScoring.PROPORTION)  // Measure-level
        .firstGroup()
            .hasNoGroupLevelScoring()                   // No group override
            .hasEffectiveScoring(MeasureScoring.PROPORTION)  // Resolved scoring
            .population("numerator").hasCount(2).up()
            .hasScore(0.3333333333333333)               // Double score
        .up()
    .up()
    // MeasureReport assertions (post-scoring) - verify FHIR resource output
    .report()
        .firstGroup()
            .hasNoGroupScoringExt()                     // No extension
            .population("numerator").hasCount(2).up()
            .hasScore("0.3333333333333333")             // String score
        .up()
    .report();
```

### Test Assertion Methods (as of 2025-12-16)

**SelectedMeasureDef (measure-level):**
- `hasMeasureScoring(MeasureScoring)`: Assert measure-level scoring is set
- `hasNoMeasureScoring()`: Assert no measure-level scoring (group-level expected)

**SelectedMeasureDefGroup (group-level, internal state):**
- `hasScore(Double)`: Assert calculated score value (existing)
- `hasNullScore()`: Assert score is null (pre-scoring) (existing)
- `hasMeasureScoring(MeasureScoring)`: Assert GroupDef.measureScoring field (existing)
- `hasGroupLevelScoring(MeasureScoring)`: Assert group has its own scoring
- `hasNoGroupLevelScoring()`: Assert group has no override (uses measure scoring)
- `hasEffectiveScoring(MeasureScoring)`: Assert the resolved scoring type (measure or group)

**SelectedMeasureReportGroup (FHIR report output):**
- `hasScore(String)`: Assert FHIR report score (existing)
- `hasGroupScoringExt(MeasureScoring)`: Assert cqfm-scoring extension present with specific code
- `hasGroupScoringExt(String)`: Assert extension present with code string
- `hasNoGroupScoringExt()`: Assert no extension (standard measure-level scoring case)

### Error Message Format

All assertion messages use `.formatted()` with expected/actual values:

```java
// Good - uses .formatted()
"Expected measure-level scoring: %s, actual: %s".formatted(expected, actual)

// Bad - uses concatenation (avoid)
"Expected measure-level scoring: " + expected + ", actual: " + actual
```

## Common Patterns

### Pattern 1: Measure-Level Scoring (90% of measures)

```java
// Measure resource
{
  "resourceType": "Measure",
  "scoring": {
    "coding": [{
      "code": "proportion"
    }]
  },
  "group": [{
    // No group-level scoring
    "population": [...]
  }]
}

// Test assertions
.def()
    .hasMeasureScoring(MeasureScoring.PROPORTION)
    .firstGroup()
        .hasNoGroupLevelScoring()
        .hasEffectiveScoring(MeasureScoring.PROPORTION)

.report()
    .firstGroup()
        .hasNoGroupScoringExt()
```

### Pattern 2: Group-Level Scoring (10% of measures)

```java
// Measure resource
{
  "resourceType": "Measure",
  // No measure-level scoring
  "group": [{
    "extension": [{
      "url": "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-scoring",
      "valueCodeableConcept": {
        "coding": [{
          "code": "proportion"
        }]
      }
    }],
    "population": [...]
  }]
}

// Test assertions
.def()
    .hasNoMeasureScoring()
    .firstGroup()
        .hasGroupLevelScoring(MeasureScoring.PROPORTION)
        .hasEffectiveScoring(MeasureScoring.PROPORTION)

.report()
    .firstGroup()
        .hasGroupScoringExt(MeasureScoring.PROPORTION)
```

## Migration Notes (Historical Context)

### Phase 1: Initial MeasureDef/GroupDef Implementation (2025-12-03)
- Added `score` field to `GroupDef`
- Added `setScoreAndAdaptToImprovementNotation()` method

### Phase 2: MeasureReportDefScorer Integration (2025-12-16)
- Created version-agnostic `MeasureReportDefScorer`
- Removed old scoring logic from builders
- Builders now use `copyScoresFromDef()` instead of recalculating
- Deleted `Dstu3MeasureReportScorer` (no longer needed)
- Retained `R4MeasureReportScorer` for external callers only

### Phase 3: Test Assertion Enhancement (2026-02-04)
- Added measure-level scoring assertions to `SelectedMeasureDef`
- Added group-level scoring assertions to `SelectedMeasureDefGroup`
- Added extension assertions to `SelectedMeasureReportGroup`
- Updated all assertion messages to use `.formatted()` with expected/actual values
- Updated 37+ tests across multiple test files

## File Locations

### Core Implementation
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDef.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/GroupDef.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureReportDefScorer.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureEvaluationResultHandler.java`

### R4 Implementation
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilder.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java` (deprecated for internal use)
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/utils/R4MeasureReportUtils.java`

### Test Assertions
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/selected/def/SelectedMeasureDef.java`
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/selected/def/SelectedMeasureDefGroup.java`
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/selected/report/SelectedMeasureReportGroup.java`

### Example Tests
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/MeasureScoringTypeProportionTest.java`
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/MeasureScoringTypeRatioContVariableTest.java`
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/ContinuousVariableResourceMeasureObservationTest.java`

## Key Takeaways

1. **Mutually Exclusive**: Measures have EITHER measure-level OR group-level scoring, never both
2. **Single Scorer**: `MeasureReportDefScorer` is the only internal scorer (as of 2025-12-16)
3. **Mutation Pattern**: Scoring mutates `GroupDef` objects, then builders copy to FHIR
4. **Extension Marker**: `cqfm-scoring` extension on report groups indicates group-level scoring
5. **Resolution Logic**: Measure scoring takes precedence over group scoring in resolution
6. **Testing**: Always test both Def state (internal) and Report output (FHIR)
7. **Error Messages**: Use `.formatted()` with expected/actual values for clarity
