# Measure Report Scoring Architecture

## Overview

Measure scoring computes numeric results (scores, aggregation values) from evaluated measure populations. The architecture follows a **Def-first** pattern: scoring operates on version-agnostic Def objects, then version-specific builders copy those scores into FHIR MeasureReport resources.

## Scoring Pipeline

```
MeasureEvaluationResultHandler
  └── MeasureReportDefScorer.score(measureDef)   ← Computes scores on Def objects
       └── For each GroupDef:
           ├── scoreGroup() → sets GroupDef.score
           └── scoreStratifiers() → sets StratumDef.score
                                    sets StratumPopulationDef.aggregationResult (CV/RCV)

R4MeasureReportBuilder.build()
  ├── buildGroups() → creates report structure from Measure resource
  │    └── R4StratifierBuilder.buildStratum() → creates StratifierGroupPopulationComponent from StratumPopulationDef
  └── copyScoresFromDef() → copies from Def objects to FHIR report
       ├── GroupDef.score → reportGroup.measureScore
       ├── PopulationDef.aggregationResult → group population extensions
       ├── StratumDef.score → reportStratum.measureScore
       └── StratumPopulationDef.aggregationResult → stratum population extensions
```

## Key Classes

### Scoring Engine

**`MeasureReportDefScorer`** (`cqf-fhir-cr/.../measure/common/MeasureReportDefScorer.java`)
- Version-agnostic scorer operating on Def objects
- Entry point: `score(MeasureDef)` → iterates groups, calls `scoreGroup()`
- `scoreGroup()` dispatches to scoring methods based on `MeasureScoring` type
- Sets `GroupDef.setScore()`, `StratumDef.setScore()`, `PopulationDef.setAggregationResult()`, and `StratumPopulationDef.setAggregationResult()`
- Uses `MeasureScoreCalculator` for pure math (proportion, ratio calculations)

**Scoring dispatch in `getStratumScoreOrNull()`:**
| MeasureScoring | Method | Notes |
|---|---|---|
| PROPORTION | `scoreProportionRatioStratum()` | Count-based |
| RATIO (no observations) | `scoreProportionRatioStratum()` | Count-based |
| RATIO (with observations) | `scoreRatioMeasureObservationStratum()` → `scoreRatioContVariableStratum()` | Aggregate-based, stores per-stratum results |
| CONTINUOUSVARIABLE | `scoreContinuousVariableStratum()` | Aggregate-based, stores per-stratum results |
| COHORT | returns null | No scoring |

### Def Objects (Data Model)

**`GroupDef`** (`cqf-fhir-cr/.../measure/common/GroupDef.java`)
- Contains populations, stratifiers, scoring type, population basis
- Mutable `score` field (set by scorer)
- `findPopulationById(id)`, `getSingle(type)`, `findPopulationByType(type)`

**`PopulationDef`** (`cqf-fhir-cr/.../measure/common/PopulationDef.java`)
- Represents a group-level population (NUMERATOR, DENOMINATOR, MEASUREOBSERVATION, etc.)
- Mutable `aggregationResult` field (set by scorer for MEASUREOBSERVATION populations only)
- Has `aggregateMethod` (SUM, AVG, MIN, MAX, MEDIAN, COUNT, N_A)
- Has `criteriaReference` (links MEASUREOBSERVATION to NUMERATOR or DENOMINATOR for RCV)
- Stores subject→resource mappings via `addResource()`

**`StratifierDef`** (`cqf-fhir-cr/.../measure/common/StratifierDef.java`)
- Contains a list of `StratumDef` objects (one per stratum value, e.g., "male", "female")
- Has `id`, `code`, `expression`, `stratifierType` (CRITERIA, VALUE, NON_SUBJECT_VALUE)

**`StratumDef`** (`cqf-fhir-cr/.../measure/common/StratumDef.java`)
- Converted from record to class (2025-12-03) for mutable `score` field
- Contains `List<StratumPopulationDef>` (one per population in the group, filtered to this stratum)
- Contains `MeasureObservationStratumCache` for RCV measures (pre-computed num/den observation lookups)
- `getStratumPopulation(PopulationDef)` finds matching stratum population by ID

**`StratumPopulationDef`** (`cqf-fhir-cr/.../measure/common/StratumPopulationDef.java`)
- Converted from record to class for mutable `aggregationResult` field
- Wraps a `PopulationDef` reference plus stratum-specific subject/resource sets
- `populationDef()` returns the parent PopulationDef (for aggregate method, criteria reference)
- `id()` delegates to `populationDef.id()`
- `getCount()` calculates stratum-specific count based on stratifier type and population basis
- `getAggregationResult()` / `setAggregationResult()` for per-stratum observation aggregates

**`MeasureObservationStratumCache`** (`cqf-fhir-cr/.../measure/common/MeasureObservationStratumCache.java`)
- Pre-computed cache for RCV measures linking numerator/denominator observation StratumPopulationDefs
- Built during evaluation in `MeasureMultiSubjectEvaluator`
- Used by `scoreRatioMeasureObservationStratum()` to avoid redundant stream lookups

### Report Builders (R4)

**`R4MeasureReportBuilder`** (`cqf-fhir-cr/.../measure/r4/R4MeasureReportBuilder.java`)
- Implements `MeasureReportBuilder<Measure, MeasureReport, DomainResource>`
- `build()`: creates report structure, then calls `copyScoresFromDef()` to transfer computed data
- `copyScoresFromDef()`: copies group scores, population aggregation results, stratifier scores, and stratum population aggregation results
- `copyStratumPopulationAggregationResults()`: iterates `StratumPopulationDef` objects with non-null `aggregationResult`, finds matching report stratum populations by ID, and applies extensions via `R4MeasureReportUtils`

**`R4StratifierBuilder`** (`cqf-fhir-cr/.../measure/r4/R4StratifierBuilder.java`)
- Creates `StratifierGroupPopulationComponent` from `StratumPopulationDef` objects
- Matches report populations to StratumPopulationDef by `populationDef.id()`
- Sets count, code, ID, and subject results on each stratum population

**`R4MeasureReportUtils`** (`cqf-fhir-cr/.../measure/r4/utils/R4MeasureReportUtils.java`)
- `addAggregationResultMethodAndCriteriaRef()`: Adds cqfm-aggregateMethod, cqfm-aggregationResult, and cqfm-criteriaReference extensions
- Has overloads for both `MeasureReportGroupPopulationComponent` (group-level) and `StratifierGroupPopulationComponent` (stratum-level)
- Private helpers accept `Element` (common base type) for shared extension logic

## FHIR Extensions on Populations

These extensions are added to both group-level and stratum-level populations:

| Extension URL | Type | Purpose |
|---|---|---|
| `cqfm-aggregateMethod` | StringType | Aggregation method (sum, avg, min, max, median, count) |
| `cqfm-aggregationResult` | DecimalType | Computed aggregate value |
| `cqfm-criteriaReference` | StringType | Links MEASUREOBSERVATION to NUMERATOR or DENOMINATOR |

**Extension constants** are in `MeasureConstants`:
- `EXT_CQFM_AGGREGATE_METHOD_URL`
- `EXT_AGGREGATION_METHOD_RESULT`
- `EXT_CQFM_CRITERIA_REFERENCE`

## Aggregation Result Flow

### Group-Level (already existed)
1. `MeasureReportDefScorer.scoreGroup()` computes aggregate for MEASUREOBSERVATION populations
2. Sets `PopulationDef.setAggregationResult(value)`
3. `R4MeasureReportBuilder.copyPopulationAggregationResults()` reads from PopulationDef and writes extensions to `MeasureReportGroupPopulationComponent`

### Stratum-Level (added for per-stratum aggregation)
1. `MeasureReportDefScorer.scoreContinuousVariableStratum()` computes CV aggregate for the stratum
2. Sets `StratumPopulationDef.setAggregationResult(value)` on the MEASUREOBSERVATION stratum population
3. `MeasureReportDefScorer.scoreRatioContVariableStratum()` computes RCV numerator/denominator aggregates
4. Sets `StratumPopulationDef.setAggregationResult(num)` and `StratumPopulationDef.setAggregationResult(den)` on both stratum populations
5. `R4MeasureReportBuilder.copyStratumPopulationAggregationResults()` reads from StratumPopulationDef and writes extensions to `StratifierGroupPopulationComponent` using `R4MeasureReportUtils`

## Testing

### Test Files
- `MeasureReportDefScorerTest` (`cqf-fhir-cr/src/test/.../common/`) - Comprehensive scorer tests
- `R4MeasureReportBuilderTest` (`cqf-fhir-cr/src/test/.../r4/`) - Builder + extension verification
- `R4MeasureReportUtilsTest` (`cqf-fhir-cr/src/test/.../r4/utils/`) - Extension helper tests

### Test Patterns
- **No mocks** - All tests use direct constructor invocation for Def objects
- **Helper methods** for common Def object creation (`createPopulationDef`, `createBooleanBasisCode`, etc.)
- **Pre/post assertions**: verify null before scoring, verify set after scoring
- **Dual verification**: check both Def state (aggregationResult) and FHIR output (extensions)

### Key Test Scenarios
- CV stratum scoring: verify `StratumPopulationDef.getAggregationResult()` is set for MEASUREOBSERVATION
- RCV stratum scoring: verify both numerator and denominator StratumPopulationDef objects get aggregation results
- Proportion stratum: verify NO aggregation results (only applicable to CV/RCV)
- Extension copying: verify `StratifierGroupPopulationComponent` has cqfm-aggregateMethod, cqfm-aggregationResult, cqfm-criteriaReference

## Common Patterns

### Creating Test Data for Stratified Measures
```java
// 1. Create PopulationDefs at group level
PopulationDef measureObsPop = new PopulationDef(
    "obs-1", obsCode, MeasurePopulationType.MEASUREOBSERVATION,
    "expr", booleanBasis, "num-1",  // criteriaReference
    ContinuousVariableObservationAggregateMethod.SUM, null);

// 2. Add observation data
Map<String, QuantityDef> obs = new HashMap<>();
obs.put("obs-key", new QuantityDef(10.0));
measureObsPop.addResource("subjectId", obs);

// 3. Create StratumPopulationDef wrapping the PopulationDef
StratumPopulationDef stratumObs = new StratumPopulationDef(
    measureObsPop, Set.of("subject1", "subject2"),
    Set.of(), List.of(), MeasureStratifierType.VALUE, booleanBasis);

// 4. Create MeasureObservationStratumCache for RCV
MeasureObservationStratumCache cache = new MeasureObservationStratumCache(numObs, denObs);

// 5. Create StratumDef
StratumDef stratum = new StratumDef(
    List.of(stratumObs), valueDefSet, subjectIds, cache);
```

### ID Matching Rules
- `StratumPopulationDef.id()` → `PopulationDef.id()` → matches Measure `MeasureGroupPopulationComponent.getId()`
- Report stratifier ID comes from Measure stratifier (`measureStratifier.getId()`), must match `StratifierDef.id()` for score copying
- Stratum matching uses text comparison via `R4MeasureReportUtils.matchesStratumValue()`
