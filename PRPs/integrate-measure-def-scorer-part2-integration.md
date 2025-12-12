# PRP: Integrate MeasureDefScorer - Part 2: Integration & Activation

## Metadata
- **Title**: MeasureDefScorer Integration - Activate Version-Agnostic Scoring in Evaluation Workflow
- **Status**: Planning Complete - Blocked on Part 1
- **Priority**: High (Architecture Consolidation)
- **Estimated Effort**: 2-3 days
- **Target Branch**: NEW branch from ld-20251208-integrate-new-measure-scorer (after Part 1 merge)
- **Planning Date**: 2025-12-09
- **Dependencies**: **REQUIRED - Part 1 (integrate-measure-def-scorer-part1-foundation.md) MUST be completed and merged first**
- **Dependents**: None (final integration)

## Executive Summary

Part 2 activates the version-agnostic MeasureDefScorer in the regular measure evaluation workflow by:
1. Calling MeasureDefScorer in MeasureEvaluationResultHandler (populates Def scores)
2. Removing old scorer calls from R4/DSTU3 builders (copyScoresFromDef becomes active)
3. Adding Fhir2Def integration tests to validate end-to-end scoring
4. Iteratively fixing any gaps in MeasureDefScorer discovered during testing

**Key Change:** MeasureDefScorer becomes the single source of truth for scoring in regular measure evaluation.

**Part 1 Dependency:** This PRP cannot begin until Part 1 is complete and merged. Part 1 provides:
- copyScoresFromDef() methods in builders (currently inactive)
- MeasureDefScorer with COHORT/COMPOSITE support
- StratumDef.getMeasureScore() method
- RatioMeasureObservationCache for performance
- Deprecated old scorers (ready for removal)

### Strategy
Activate new scoring path, validate with integration tests, remove old scorer calls.

## Problem Statement

### Current State (After Part 1)
- MeasureDefScorer exists and is tested
- copyScoresFromDef() exists but does nothing (null scores)
- Old R4/DSTU3 scorers still produce scores in regular evaluation
- MeasureReportScoringFhirAdapter uses MeasureDefScorer (cdr-cr only)

### Part 2 Goals
1. **Activate MeasureDefScorer** - Call in MeasureEvaluationResultHandler
2. **Remove Old Scorers** - Delete calls from R4/DSTU3 builders
3. **Integration Testing** - Add Fhir2Def tests for all scoring types
4. **Iterative Refinement** - Fix gaps discovered during testing
5. **Cleanup** - (Future) Remove deprecated scorer implementations

### Out of Scope
- Removing deprecated scorer class files (future PR after cdr-cr migration)
- R5 implementation (already planned)

## Architecture Overview

### Current Flow (Part 1)
```
MeasureEvaluationResultHandler.processResults()
  ↓
MeasureMultiSubjectEvaluator.postEvaluationMultiSubject()
  → Builds GroupDef, StratumDef objects
  → Def objects have NULL scores
  ↓
R4MeasureReportBuilder.build()
  ↓
copyScoresFromDef(bc)
  → Finds null scores, does nothing
  ↓
measureReportScorer.score()  ← OLD: Still active, produces scores
  ↓
MeasureReport with scores ✅
```

### New Flow (Part 2)
```
MeasureEvaluationResultHandler.processResults()
  ↓
MeasureMultiSubjectEvaluator.postEvaluationMultiSubject()
  → Builds GroupDef, StratumDef objects
  → Def objects have NULL scores
  ↓
MeasureDefScorer.score(measureDef)  ← NEW: Populates Def scores
  ↓
R4MeasureReportBuilder.build()
  ↓
copyScoresFromDef(bc)  ← NOW ACTIVE: Copies scores from Def
  ↓
(old scorer call removed)
  ↓
MeasureReport with scores ✅
```

## Implementation Plan

### Phase 1: Activate MeasureDefScorer in Workflow

**Objective:** Call MeasureDefScorer in MeasureEvaluationResultHandler to populate Def scores.

**Tasks:**
1. Add MeasureDefScorer instantiation and call in `MeasureEvaluationResultHandler.processResults()`
2. Place call after `postEvaluationMultiSubject()` (line 77)
3. Guard with `if (applyScoring)` condition
4. Verify measureDef.url() is available

**Implementation:**

#### MeasureEvaluationResultHandler.java
```java
package org.opencds.cqf.fhir.cr.measure.common;

// ... existing imports ...
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasureEvaluationResultHandler {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluationResultHandler.class);

    public <MeasureT extends IBaseResource, MeasureReportT extends IBaseResource>
            MeasureReportT processResults(
                    MeasureProcessor<MeasureT, ?, ?> processor,
                    MeasureDef measureDef,
                    List<String> subjectIds,
                    MeasureReportType measureReportType,
                    boolean isSingleSubject) {

        // ... existing code ...

        // Line 77: Existing call
        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(fhirContext, measureDef);

        // NEW: Score all groups and stratifiers using version-agnostic scorer
        // Added in Part 2 (integrate-measure-def-scorer-part2-integration)
        if (processor.applyScoring()) {
            logger.debug("Scoring MeasureDef using MeasureDefScorer for measure: {}", measureDef.url());
            MeasureDefScorer measureDefScorer = new MeasureDefScorer();
            measureDefScorer.score(measureDef.url(), measureDef);
        }

        // ... existing code ...

        return processor.buildMeasureReport(
                processor.getResource(),
                measureDef,
                measureReportType,
                processor.getMeasurementPeriod(),
                subjectIds);
    }
}
```

**Key Points:**
- Placed **after** postEvaluationMultiSubject() (Def objects built)
- Placed **before** buildMeasureReport() (scores needed for copying)
- Guarded with `applyScoring()` condition (respects processor settings)
- Logs for debugging

**Files Modified:**
- `cqf-fhir-cr/.../common/MeasureEvaluationResultHandler.java`

---

### Phase 2: Remove Old Scorer Calls from Builders

**Objective:** Remove R4/DSTU3 scorer calls, making copyScoresFromDef() the sole scoring path.

**Tasks:**
1. Remove `measureReportScorer.score()` call from R4MeasureReportBuilder (line 96+1 from Part 1)
2. Remove `measureReportScorer.score()` call from Dstu3MeasureReportBuilder (line 98+1 from Part 1)
3. Update comments to reflect new behavior
4. Optionally remove `measureReportScorer` field and constructor initialization

**Implementation:**

#### R4MeasureReportBuilder.java (Part 2)
```java
@Override
public MeasureReport build(
        Measure measure,
        MeasureDef measureDef,
        MeasureReportType measureReportType,
        Interval measurementPeriod,
        List<String> subjectIds) {

    var report = this.createMeasureReport(measure, measureDef, measureReportType, subjectIds, measurementPeriod);

    var bc = new R4MeasureReportBuilderContext(measure, measureDef, report);

    // buildGroups must be run first to set up the builder context to be able to use
    // the evaluatedResource references for SDE processing
    buildGroups(bc);

    buildSDEs(bc);

    addEvaluatedResource(bc);
    addSupplementalData(bc);
    bc.addOperationOutcomes();

    for (var r : bc.contained().values()) {
        bc.report().addContained(r);
    }

    // Copy scores from Def objects (populated by MeasureDefScorer in MeasureEvaluationResultHandler)
    // Updated in Part 2 (integrate-measure-def-scorer-part2-integration)
    copyScoresFromDef(bc);

    // REMOVED in Part 2: Old scorer no longer needed
    // this.measureReportScorer.score(measure.getUrl(), measureDef, bc.report());

    setReportStatus(bc);
    return bc.report();
}
```

**Optional Cleanup:**
```java
// Can remove field and constructor initialization if desired
// protected static final IMeasureReportScorer<MeasureReport> measureReportScorer;
//
// public R4MeasureReportBuilder() {
//     this.measureReportScorer = new R4MeasureReportScorer();
// }
```

#### Dstu3MeasureReportBuilder.java (Part 2)
```java
@Override
public MeasureReport build(
        Measure measure,
        MeasureDef measureDef,
        MeasureReportType measureReportType,
        Interval measurementPeriod,
        List<String> subjectIds) {
    this.reset();

    this.measure = measure;
    this.report = this.createMeasureReport(measure, measureReportType, subjectIds, measurementPeriod);

    buildGroups(measure, measureDef);
    processSdes(measure, measureDef, subjectIds);

    // Copy scores from Def objects (populated by MeasureDefScorer in MeasureEvaluationResultHandler)
    // Updated in Part 2 (integrate-measure-def-scorer-part2-integration)
    copyScoresFromDef(measure, measureDef, this.report);

    // REMOVED in Part 2: Old scorer no longer needed
    // this.measureReportScorer.score(measure.getUrl(), measureDef, this.report);

    // Only add evaluated resources to individual reports
    if (measureReportType == MeasureReportType.INDIVIDUAL) {
        ListResource references = this.createReferenceList(
                "evaluated-resources-references",
                this.getEvaluatedResourceReferences().values());
        this.report.addContained(references);
        this.report.setEvaluatedResources(new Reference("#" + references.getId()));
    }

    return this.report;
}
```

**Files Modified:**
- `cqf-fhir-cr/.../r4/R4MeasureReportBuilder.java`
- `cqf-fhir-cr/.../dstu3/Dstu3MeasureReportBuilder.java`

---

### Phase 3: Fhir2Def Integration Tests - Continuous Variable

**Objective:** Add score assertions to existing ContinuousVariableResourceMeasureObservationFhir2DefTest.

**Tasks:**
1. Add `.hasScore()` assertions to existing test methods
2. Verify scores match expected values (compare to baseline)
3. Add assertions for improvement notation behavior

**Implementation:**

#### ContinuousVariableResourceMeasureObservationFhir2DefTest.java
```java
@Test
void continuousVariableWithMeasureObservation() {
    given().repositoryFor("ContinuousVariableMeasure")
        .when().measureId("CVExample").captureDef().evaluate()
        .then().def()
            .firstGroup()
                .hasScore(expectedScore)  // NEW: Verify group score
                .population(MeasurePopulationType.MEASUREOBSERVATION)
                    .hasCount(expectedCount);
}

@Test
void continuousVariableWithStratifiers() {
    given().repositoryFor("ContinuousVariableMeasure")
        .when().measureId("CVStratifierExample").captureDef().evaluate()
        .then().def()
            .firstGroup()
                .hasScore(expectedGroupScore)
                .stratifierById("gender")
                    .stratumByValue("male").hasScore(expectedMaleScore).up()
                    .stratumByValue("female").hasScore(expectedFemaleScore);
}
```

**Files Modified:**
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/ContinuousVariableResourceMeasureObservationFhir2DefTest.java`

---

### Phase 4: Fhir2Def Integration Tests - Proportion

**Objective:** Create comprehensive Proportion measure Fhir2Def tests.

**Tasks:**
1. Create `MeasureScoringTypeProportionFhir2DefTest.java`
2. Test basic proportion scoring (n/d)
3. Test with exclusions (n-nx)/(d-dx-de)
4. Test with stratifiers
5. Test with improvement notation

**Implementation:**

#### MeasureScoringTypeProportionFhir2DefTest.java
```java
package org.opencds.cqf.fhir.cr.measure.fhir2deftest.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.*;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.Fhir2DefTestBase;

/**
 * Integration tests for Proportion measure scoring with Fhir2Def framework.
 * Tests end-to-end scoring including MeasureDefScorer integration.
 *
 * Created for Part 2 (integrate-measure-def-scorer-part2-integration).
 */
class MeasureScoringTypeProportionFhir2DefTest extends Fhir2DefTestBase {

    @Test
    void proportionBasic_SimpleScore() {
        // Test: Basic proportion (n=75, d=100 → score=0.75)
        given().repositoryFor("ProportionMeasure")
            .when().measureId("ProportionBasic").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasScore(0.75)
                    .population(NUMERATOR).hasCount(75).up()
                    .population(DENOMINATOR).hasCount(100);
    }

    @Test
    void proportionWithExclusions_AdjustedScore() {
        // Test: Proportion with exclusions (n=4, nx=1, d=6, dx=1, de=1 → score=0.75)
        // Formula: (n-nx)/(d-dx-de) = (4-1)/(6-1-1) = 3/4 = 0.75
        given().repositoryFor("ProportionMeasure")
            .when().measureId("ProportionWithExclusions").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasScore(0.75)
                    .population(NUMERATOR).hasCount(4).up()
                    .population(NUMERATOREXCLUSION).hasCount(1).up()
                    .population(DENOMINATOR).hasCount(6).up()
                    .population(DENOMINATOREXCLUSION).hasCount(1).up()
                    .population(DENOMINATOREXCEPTION).hasCount(1);
    }

    @Test
    void proportionWithStratifiers_StratifierScores() {
        // Test: Stratifier scores calculated correctly
        given().repositoryFor("ProportionMeasure")
            .when().measureId("ProportionStratified").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasScore(0.75)
                    .stratifierById("gender")
                        .stratumByValue("male").hasScore(0.80).up()
                        .stratumByValue("female").hasScore(0.70);
    }

    @Test
    void proportionWithDecreaseImprovement_InvertedScore() {
        // Test: Improvement notation "decrease" inverts score
        // Raw score 0.25 → inverted 0.75 (1 - 0.25)
        given().repositoryFor("ProportionMeasure")
            .when().measureId("ProportionDecrease").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasScore(0.75)  // Inverted from raw 0.25
                    .population(NUMERATOR).hasCount(25).up()
                    .population(DENOMINATOR).hasCount(100);
    }

    @Test
    void proportionZeroDenominator_NullScore() {
        // Test: Zero denominator → null score
        given().repositoryFor("ProportionMeasure")
            .when().measureId("ProportionZeroDenom").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasNullScore()
                    .population(NUMERATOR).hasCount(0).up()
                    .population(DENOMINATOR).hasCount(0);
    }
}
```

**Files Created:**
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/MeasureScoringTypeProportionFhir2DefTest.java`

---

### Phase 5: Fhir2Def Integration Tests - Cohort

**Objective:** Create COHORT measure Fhir2Def tests (verify null scores).

**Tasks:**
1. Create `MeasureScoringTypeCohortFhir2DefTest.java`
2. Test that COHORT measures have null scores
3. Test with stratifiers (stratifiers also have null scores)

**Implementation:**

#### MeasureScoringTypeCohortFhir2DefTest.java
```java
package org.opencds.cqf.fhir.cr.measure.fhir2deftest.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.*;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.Fhir2DefTestBase;

/**
 * Integration tests for COHORT measure scoring with Fhir2Def framework.
 * COHORT measures do not have scores - tests verify null scores.
 *
 * Created for Part 2 (integrate-measure-def-scorer-part2-integration).
 */
class MeasureScoringTypeCohortFhir2DefTest extends Fhir2DefTestBase {

    @Test
    void cohortBasic_NullScore() {
        // Test: COHORT measures have null scores
        given().repositoryFor("CohortMeasure")
            .when().measureId("CohortBasic").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasNullScore()  // COHORT measures don't have scores
                    .population(INITIALPOPULATION).hasCount(50);
    }

    @Test
    void cohortWithStratifiers_NullStratifierScores() {
        // Test: COHORT stratifiers also have null scores
        given().repositoryFor("CohortMeasure")
            .when().measureId("CohortStratified").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasNullScore()
                    .stratifierById("gender")
                        .stratumByValue("male").hasNullScore().up()
                        .stratumByValue("female").hasNullScore();
    }
}
```

**Files Created:**
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/MeasureScoringTypeCohortFhir2DefTest.java`

---

### Phase 6: Fhir2Def Integration Tests - Ratio

**Objective:** Create Ratio measure Fhir2Def tests (standard and continuous variable).

**Tasks:**
1. Create `MeasureScoringTypeRatioFhir2DefTest.java`
2. Test standard ratio scoring (n/d)
3. Test ratio with measure observations (continuous variable)
4. Test with stratifiers

**Implementation:**

#### MeasureScoringTypeRatioFhir2DefTest.java
```java
package org.opencds.cqf.fhir.cr.measure.fhir2deftest.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.*;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.Fhir2DefTestBase;

/**
 * Integration tests for Ratio measure scoring with Fhir2Def framework.
 * Tests both standard ratio and ratio with measure observations (continuous variable).
 *
 * Created for Part 2 (integrate-measure-def-scorer-part2-integration).
 */
class MeasureScoringTypeRatioFhir2DefTest extends Fhir2DefTestBase {

    @Test
    void ratioBasic_SimpleScore() {
        // Test: Basic ratio (n=80, d=100 → score=0.80)
        given().repositoryFor("RatioMeasure")
            .when().measureId("RatioBasic").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasScore(0.80)
                    .population(NUMERATOR).hasCount(80).up()
                    .population(DENOMINATOR).hasCount(100);
    }

    @Test
    void ratioContinuousVariable_MeasureObservations() {
        // Test: Ratio with measure observations (aggregate values)
        given().repositoryFor("RatioMeasure")
            .when().measureId("RatioContinuousVariable").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasScore(expectedScore)
                    .population(NUMERATOR).hasCount(expectedNumCount).up()
                    .population(DENOMINATOR).hasCount(expectedDenCount).up()
                    .population(MEASUREOBSERVATION).hasCount(expectedObsCount);
    }

    @Test
    void ratioWithStratifiers_StratifierScores() {
        // Test: Stratifier scores with observation caching optimization
        given().repositoryFor("RatioMeasure")
            .when().measureId("RatioStratified").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasScore(0.80)
                    .stratifierById("gender")
                        .stratumByValue("male").hasScore(0.85).up()
                        .stratumByValue("female").hasScore(0.75);
    }

    @Test
    void ratioZeroDenominator_NullScore() {
        // Test: Zero denominator → null score
        given().repositoryFor("RatioMeasure")
            .when().measureId("RatioZeroDenom").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasNullScore()
                    .population(NUMERATOR).hasCount(0).up()
                    .population(DENOMINATOR).hasCount(0);
    }
}
```

**Files Created:**
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/MeasureScoringTypeRatioFhir2DefTest.java`

---

### Phase 7: Iterative Refinement

**Objective:** Fix any gaps in MeasureDefScorer discovered during integration testing.

**Tasks:**
1. Run all Fhir2Def integration tests
2. Identify any test failures or incorrect scores
3. Debug MeasureDefScorer to find root cause
4. Implement fixes in MeasureDefScorer
5. Re-run tests until all pass
6. Compare scores to baseline (Part 1) to ensure no regression

**Expected Issues:**
- Edge cases in score calculation (zero denominators, null handling)
- Improvement notation not applied correctly
- Stratifier scoring gaps
- Observation aggregation issues

**Iterative Process:**
```
1. Run tests → Identify failures
2. Debug MeasureDefScorer → Find issue
3. Implement fix → Add unit test
4. Re-run integration tests → Verify fix
5. Repeat until all tests pass
```

**Files Modified (as needed):**
- `cqf-fhir-cr/.../common/MeasureDefScorer.java`
- `cqf-fhir-cr/src/test/java/.../common/MeasureDefScorerTest.java`

---

### Phase 8: Documentation

**Objective:** Update documentation to reflect new scoring architecture.

**Tasks:**
1. Update CLAUDE.md/AGENTS.md with new patterns
2. Document MeasureDefScorer integration in workflow
3. Add migration notes for old scorer removal
4. Update cdr-cr migration guide

**Files Modified:**
- `CLAUDE.md` / `AGENTS.md`

---

## Files Summary

### Files Created (Part 2)
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/MeasureScoringTypeProportionFhir2DefTest.java`
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/MeasureScoringTypeCohortFhir2DefTest.java`
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/MeasureScoringTypeRatioFhir2DefTest.java`

### Files Modified (Part 2)
- `cqf-fhir-cr/.../common/MeasureEvaluationResultHandler.java` (add MeasureDefScorer call)
- `cqf-fhir-cr/.../r4/R4MeasureReportBuilder.java` (remove old scorer call)
- `cqf-fhir-cr/.../dstu3/Dstu3MeasureReportBuilder.java` (remove old scorer call)
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/ContinuousVariableResourceMeasureObservationFhir2DefTest.java` (add score assertions)
- `cqf-fhir-cr/.../common/MeasureDefScorer.java` (iterative fixes as needed)
- `cqf-fhir-cr/src/test/java/.../common/MeasureDefScorerTest.java` (add tests for fixes)

### Files NOT Modified (Part 2)
- `cqf-fhir-cr/.../r4/R4MeasureReportScorer.java` (deprecated, keep for now)
- `cqf-fhir-cr/.../dstu3/Dstu3MeasureReportScorer.java` (deprecated, keep for now)
- `cqf-fhir-cr/.../common/MeasureReportScoringFhirAdapter.java` (already uses MeasureDefScorer)

---

## Success Criteria (Part 2)

### Functional Requirements
- ✅ All existing tests pass (no regressions from Part 1 baseline)
- ✅ All new Fhir2Def integration tests pass
- ✅ Scores match Part 1 baseline exactly
- ✅ Def objects have scores populated before builders run
- ✅ copyScoresFromDef() successfully copies scores to MeasureReport
- ✅ Old scorer calls removed from builders

### Non-Functional Requirements
- ✅ Version-agnostic scoring active in regular evaluation
- ✅ Performance maintained or improved (observation caching)
- ✅ Clear path to remove deprecated scorers (future PR)
- ✅ End-to-end integration validated

### Code Quality
- ✅ JavaDoc updated for modified methods
- ✅ Consistent naming conventions
- ✅ Proper null handling
- ✅ Code formatted with spotless

---

## Testing Strategy

### Test Phases
1. **Baseline Comparison** - Compare scores to Part 1 baseline
2. **Integration Tests** - Run all Fhir2Def tests
3. **Regression Tests** - Run full existing test suite
4. **Performance Tests** - Verify no regression

### Expected Results
- All tests pass
- Scores identical to Part 1 baseline
- Def objects have scores populated
- No performance regression

---

## Risk Mitigation

### Risk 1: Score Mismatch with Baseline
**Likelihood:** Medium
**Impact:** High
**Mitigation:**
- Compare scores to Part 1 baseline before and after
- Iterative debugging process
- Unit tests for edge cases
- Can revert to Part 1 if needed

### Risk 2: Test Failures
**Likelihood:** Medium
**Impact:** Medium
**Mitigation:**
- Iterative fix-and-test approach
- Comprehensive Fhir2Def test coverage
- Debug with Def assertions framework

### Risk 3: Performance Regression
**Likelihood:** Low
**Impact:** Medium
**Mitigation:**
- Observation caching from Part 1 improves performance
- No additional computation (old scorers removed)
- Performance tests verify no regression

---

## Part 1 Dependency Requirements

**Part 2 CANNOT start until Part 1 provides:**
- ✅ copyScoresFromDef() in R4MeasureReportBuilder
- ✅ copyScoresFromDef() in Dstu3MeasureReportBuilder
- ✅ MeasureDefScorer.score() with COHORT/COMPOSITE support
- ✅ StratumDef.getMeasureScore()
- ✅ RatioMeasureObservationCache
- ✅ Deprecated old scorers
- ✅ All Part 1 tests passing

**Verification Before Starting Part 2:**
```bash
# Verify Part 1 branch merged
git log --oneline | grep "integrate-measure-def-scorer-part1"

# Verify Part 1 files exist
ls cqf-fhir-cr/.../common/RatioMeasureObservationCache.java
ls cqf-fhir-cr/.../common/MeasureReportScoringFhirAdapter.java

# Run Part 1 tests
./mvnw test -pl cqf-fhir-cr

# Verify no regressions
git diff baseline..part1-branch -- "*Test.java" | grep "score"
```

---

## Future Work (Not Part 2)

### Cleanup (Future PR - After cdr-cr Migration)
1. Remove deprecated scorer class files:
   - `R4MeasureReportScorer.java`
   - `Dstu3MeasureReportScorer.java`
   - `IMeasureReportScorer.java`
   - `BaseMeasureReportScorer.java`
2. Remove scorer field and constructor from builders
3. Update cdr-cr project to use MeasureReportScoringFhirAdapter

### Enhancements (Future PRs)
1. R5 support in MeasureReportScoringFhirAdapter
2. Complete COMPOSITE measure scoring
3. Enhanced stratifier population logic in adapter
4. Additional Fhir2Def test coverage

---

## Conclusion

Part 2 completes the integration of version-agnostic MeasureDefScorer:
- **Activation** - MeasureDefScorer called in evaluation workflow
- **Validation** - Fhir2Def integration tests ensure correctness
- **Cleanup** - Old scorer calls removed from builders
- **Tested** - End-to-end integration validated

**Blockers:** Cannot start until Part 1 (integrate-measure-def-scorer-part1-foundation.md) is complete and merged.

**Deliverables:**
- MeasureDefScorer active in regular measure evaluation
- Old scorers deprecated and calls removed
- Comprehensive Fhir2Def integration tests
- Clear path to final cleanup (remove deprecated classes)
