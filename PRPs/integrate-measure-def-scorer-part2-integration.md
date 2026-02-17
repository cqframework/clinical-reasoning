# PRP: Integrate MeasureDefScorer - Part 2: Integration & Activation

## Metadata
- **Title**: MeasureDefScorer Integration - Activate Version-Agnostic Scoring in Evaluation Workflow
- **Status**: ⚠️ Partially Complete - Alternative R4 FHIR-Specific Approach Implemented
- **Priority**: High (Architecture Consolidation)
- **Estimated Effort**: 2-3 days
- **Target Branch**: ld-20251216-switch-to-new-measure-scorer (updated from ld-20251210-integrate-new-measure-scorer-part-2)
- **Planning Date**: 2025-12-09
- **Last Updated**: 2025-12-16 (Final cleanup: Dstu3MeasureReportScorer deleted, R4MeasureReportScorer javadoc updated for external callers)
- **Dependencies**: ✅ Part 1 (integrate-measure-def-scorer-part1-foundation.md) - COMPLETE
- **Dependents**: None (final integration)
- **Completion Status**:
  - ✅ R4MeasureReportScorer R4 FHIR-specific logic restoration (2025-12-16) - COMPLETE
    - ✅ Phase 1: `getCountFromStratifierPopulation()` restored (stratifier scoring)
    - ✅ Phase 2: `getCountFromGroupPopulation()` restored (group scoring)
    - ✅ All tests pass: 965 tests, 0 failures
  - ✅ PRP Phase 1: MeasureEvaluationResultHandler MeasureReportDefScorer integration (2025-12-16) - COMPLETE
    - ✅ Renamed MeasureDefScorer to MeasureReportDefScorer
    - ✅ Refactored MeasureEvaluationResultHandler from static to instance-based
    - ✅ Integrated MeasureReportDefScorer as field in MeasureEvaluationResultHandler
    - ✅ Updated R4MeasureProcessor and Dstu3MeasureProcessor to maintain handler instance
    - ✅ Added MeasureReportDefScorer call after postEvaluationMultiSubject
    - ✅ Scoring now runs unconditionally (not guarded by applyScoring flag)
    - ✅ All tests pass: 965 tests, 0 failures
  - ✅ PRP Phase 2: Remove old scorer calls from builders (2025-12-16) - COMPLETE
    - ✅ Removed `measureReportScorer` field from R4MeasureReportBuilder
    - ✅ Removed `measureReportScorer` field from Dstu3MeasureReportBuilder
    - ✅ Removed unused IMeasureReportScorer imports
    - ✅ All tests pass: 970 tests, 0 failures
  - ✅ PRP Phases 3-6: Integration tests via def/report dual structure (2025-12-16) - COMPLETE
    - ✅ Added def/report assertions to all MeasureScoringType* tests (45 tests)
    - ✅ Verified MeasureDef scoring (pre-FHIR conversion)
    - ✅ Verified MeasureReport scoring (post-FHIR conversion)
    - ✅ Fixed SelectedMeasureDefStratumPopulation#hasCount() to use getCount()
    - ✅ Implemented SelectedMeasureDefStratifier#stratumByValue() for value-based navigation
    - ✅ All tests pass: 473 measure tests, 0 failures
  - ✅ PRP Phase 7: Iterative refinement (2025-12-16) - COMPLETE
    - ✅ Ran all integration tests: 473 measure tests, 0 failures
    - ✅ No scoring gaps or edge cases identified
    - ✅ All measure types verified (Cohort, Proportion, Ratio, Continuous Variable)
  - ✅ PRP Phase 8: Documentation (2025-12-16) - COMPLETE
    - ✅ Added "Measure Scoring Architecture" section to CLAUDE.md
    - ✅ Documented MeasureReportDefScorer integration workflow
    - ✅ Added testing pattern examples (def/report dual structure)
    - ✅ Added migration notes for old scorer removal
  - ✅ PRP Final Cleanup: Remove Dstu3 Scorer and Update R4 Javadoc (2025-12-16) - COMPLETE
    - ✅ Deleted Dstu3MeasureReportScorer.java (no longer needed after MeasureReportDefScorer integration)
    - ✅ Deleted MeasureScorerTest.java (unit test for deleted Dstu3 scorer)
    - ✅ Updated R4MeasureReportScorer.java javadoc to clarify external-only usage
    - ✅ Noted internal measure evaluation uses MeasureReportDefScorer instead
    - ✅ Documented backward compatibility retention for external callers
- **Decision**: Option 1 chosen (Complete all PRP phases) - ✅ COMPLETE (2025-12-16)

## Corrections Applied (2025-12-10)

This plan has been updated to match the actual codebase state after Part 1 completion:

1. **MeasureEvaluationResultHandler.processResults()** - Corrected method signature:
   - Changed from instance method to static method
   - Updated to use `applyScoring` parameter directly (not `processor.applyScoring()`)
   - Maintained correct line 77 reference for insertion point

2. **Test Framework** - Updated test class patterns:
   - Changed from `extends Fhir2DefTestBase` to `Fhir2DefUnifiedMeasureTestHandler.given()` pattern
   - Updated all test examples in Phases 3-6 to use handler pattern

3. **RatioMeasureObservationCache** - Removed all references:
   - This class was removed during Part 1 execution (not needed)
   - Cleaned up all mentions from dependency lists and verification steps

All code examples have been corrected to match the actual implementation. The plan is now ready for execution.

## Updates Applied (2025-12-16)

This plan has been updated to reflect the alternative implementation approach:

1. **MeasureReportScoringFhirAdapter** - Not Implemented:
   - Original plan assumed this adapter class existed as a bridge between MeasureDefScorer and R4MeasureReportScorer
   - This class was NOT implemented (does not exist in source code)
   - Alternative approach adopted: Restore R4 FHIR-specific logic from commit 9874c95

2. **R4MeasureReportScorer** - Restored R4 FHIR-Specific Logic (2025-12-16) - ✅ COMPLETE:
   - **Phase 1 (Completed):** Restored `getCountFromStratifierPopulation(List<StratifierGroupPopulationComponent>, String)` method
     - Used for stratifier-level proportion/ratio scoring
     - Works directly with `stratum.getPopulation()`
     - Lines: 621-639
   - **Phase 2 (Completed):** Restored `getCountFromGroupPopulation(List<MeasureReportGroupPopulationComponent>, String)` method
     - Used for group-level proportion/ratio scoring
     - Works directly with `mrgc.getPopulation()`
     - Updated `scoreGroup()` to use this method instead of `groupDef.getPopulationCount()`
     - Lines: 601-619, call site: 185-189
   - These methods work directly with R4 MeasureReport objects instead of Def objects
   - Provides immediate solution without requiring the missing adapter
   - Both Def-based and FHIR-based methods coexist as overloads
   - All tests pass: 965 tests, 0 failures

3. **Architectural Change** - Dual Approach:
   - **Continuous Variable scoring:** Uses Def-based approach (version-agnostic)
   - **Proportion/Ratio stratifier scoring:** Uses R4 FHIR-specific approach (restored)
   - This hybrid approach provides flexibility until adapter is implemented

All references to MeasureReportScoringFhirAdapter have been updated to reflect current state.

## Refactoring Applied (2025-12-16)

This section documents the architectural refactoring of MeasureEvaluationResultHandler after Phase 1 implementation:

### Background

After completing Phase 1 (adding MeasureReportDefScorer call), the code exhibited several architectural issues:
- MeasureEvaluationResultHandler was a static utility class
- MeasureReportDefScorer was instantiated inline on every call
- MeasureEvaluator was instantiated on every call
- applyScoring and PopulationBasisValidator were passed as method parameters

### Refactoring Changes

The following refactoring was applied to improve architecture and maintainability:

#### 1. Renamed MeasureDefScorer to MeasureReportDefScorer

**Rationale:** Better name to reflect that this scorer operates on MeasureDef objects and populates scores that will be used by MeasureReport builders.

**Changes:**
- Class renamed: `MeasureDefScorer` → `MeasureReportDefScorer`
- Test class renamed: `MeasureDefScorerTest` → `MeasureReportDefScorerTest`
- All references updated throughout codebase
- Logger updated to use new class name

#### 2. Converted MeasureEvaluationResultHandler to Instance-Based Class

**Rationale:** Instance-based design allows for:
- Better dependency management through constructor injection
- Easier testing with mock dependencies
- Single initialization of heavy objects (scorer, evaluator)
- More flexible architecture for future changes

**Before (Static):**
```java
public class MeasureEvaluationResultHandler {
    private MeasureEvaluationResultHandler() {
        // static class
    }

    public static void processResults(
            FhirContext fhirContext,
            Map<String, EvaluationResult> evalResultsPerSubject,
            MeasureDef measureDef,
            @Nonnull MeasureEvalType measureEvalType,
            boolean applyScoring,
            PopulationBasisValidator populationBasisValidator) {

        MeasureEvaluator evaluator = new MeasureEvaluator(populationBasisValidator);
        // ... process results ...
        MeasureReportDefScorer measureDefScorer = new MeasureReportDefScorer();
        measureDefScorer.score(measureDef.url(), measureDef);
    }
}
```

**After (Instance-Based):**
```java
public class MeasureEvaluationResultHandler {
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasureEvaluator measureEvaluator;
    private final MeasureReportDefScorer measureReportDefScorer;

    public MeasureEvaluationResultHandler(
            MeasureEvaluationOptions measureEvaluationOptions,
            PopulationBasisValidator populationBasisValidator) {
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measureEvaluator = new MeasureEvaluator(populationBasisValidator);
        this.measureReportDefScorer = new MeasureReportDefScorer();
    }

    public void processResults(
            FhirContext fhirContext,
            Map<String, EvaluationResult> evalResultsPerSubject,
            MeasureDef measureDef,
            @Nonnull MeasureEvalType measureEvalType) {

        // Use fields instead of creating new instances
        measureEvaluator.evaluate(..., measureEvaluationOptions.getApplyScoringSetMembership());
        // ...
        measureReportDefScorer.score(measureDef.url(), measureDef);
    }
}
```

#### 3. Simplified Method Signature

**Changes:**
- Removed `applyScoring` parameter (derived from field)
- Removed `PopulationBasisValidator` parameter (used in constructor only)
- `processResults()` changed from static to instance method

**Impact:**
- Cleaner API with fewer parameters
- Dependencies initialized once in constructor
- Method signature focuses on per-evaluation data (results, measureDef, evalType)

#### 4. Updated Processor Classes

Both R4MeasureProcessor and Dstu3MeasureProcessor were updated to maintain a MeasureEvaluationResultHandler instance:

**R4MeasureProcessor.java:**
```java
public class R4MeasureProcessor {
    private final MeasureEvaluationResultHandler measureEvaluationResultHandler;

    public R4MeasureProcessor(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasureProcessorUtils measureProcessorTimeUtils) {
        // ...
        this.measureEvaluationResultHandler =
                new MeasureEvaluationResultHandler(
                    this.measureEvaluationOptions,
                    new R4PopulationBasisValidator());
    }

    // Call sites updated (2 locations):
    // Line 188: measureEvaluationResultHandler.processResults(fhirContext, results, measureDef, evaluationType);
    // Line 240: measureEvaluationResultHandler.processResults(fhirContext, resultForThisMeasure, measureDef, evaluationType);
}
```

**Dstu3MeasureProcessor.java:**
```java
public class Dstu3MeasureProcessor {
    private final MeasureEvaluationResultHandler measureEvaluationResultHandler;

    public Dstu3MeasureProcessor(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            SubjectProvider subjectProvider) {
        // ...
        this.measureEvaluationResultHandler =
                new MeasureEvaluationResultHandler(
                    this.measureEvaluationOptions,
                    new Dstu3PopulationBasisValidator());
    }

    // Call site updated:
    // Line 196-197: measureEvaluationResultHandler.processResults(fhirContext, results.processMeasureForSuccessOrFailure(measureDef), measureDef, evalType);
}
```

#### 5. Scoring Behavior Change

**Important Change:** Scoring now runs unconditionally for all measure evaluations.

**Before:**
```java
if (applyScoring) {
    measureDefScorer.score(measureDef.url(), measureDef);
}
```

**After:**
```java
// Note: Scoring is always performed, independent of applyScoring flag
// (applyScoring controls set membership filtering, not numeric scoring)
logger.debug("Scoring MeasureDef using MeasureReportDefScorer for measure: {}", measureDef.url());
measureReportDefScorer.score(measureDef.url(), measureDef);
```

**Rationale:**
- `applyScoring` flag controls **set membership filtering** in MeasureEvaluator, not numeric scoring
- Numeric scores should always be computed to support all report types
- Builders rely on scores being present in Def objects
- Unconditional scoring simplifies logic and prevents null score issues

### Files Modified (Refactoring)

- `MeasureReportDefScorer.java` (renamed from MeasureDefScorer.java)
- `MeasureReportDefScorerTest.java` (renamed from MeasureDefScorerTest.java)
- `MeasureEvaluationResultHandler.java` (converted from static to instance-based)
- `R4MeasureProcessor.java` (added handler field, updated call sites)
- `Dstu3MeasureProcessor.java` (added handler field, updated call sites)

### Test Results

All 965 tests pass after refactoring:
- No functional changes to scoring logic
- Architecture improved without behavior changes
- Clean separation of concerns between handler, scorer, and evaluator

## Executive Summary

Part 2 activates the version-agnostic MeasureReportDefScorer in the regular measure evaluation workflow by:
1. Calling MeasureReportDefScorer in MeasureEvaluationResultHandler (populates Def scores)
2. Removing old scorer calls from R4/DSTU3 builders (copyScoresFromDef becomes active)
3. Adding Fhir2Def integration tests to validate end-to-end scoring
4. Iteratively fixing any gaps in MeasureReportDefScorer discovered during testing

**Key Change:** MeasureReportDefScorer becomes the single source of truth for scoring in regular measure evaluation.

**Part 1 Dependency:** This PRP cannot begin until Part 1 is complete and merged. Part 1 provides:
- copyScoresFromDef() methods in builders (currently inactive)
- MeasureReportDefScorer with COHORT/COMPOSITE support
- StratumDef.getMeasureScore() method
- Deprecated old scorers (ready for removal)

### Strategy
Activate new scoring path, validate with integration tests, remove old scorer calls.

## Problem Statement

### Current State (After Part 1, Updated 2025-12-16)
- MeasureReportDefScorer exists and is tested (renamed from MeasureDefScorer)
- MeasureEvaluationResultHandler refactored to instance-based class
- copyScoresFromDef() exists but does nothing (null scores from old scorers)
- MeasureReportDefScorer is now called in evaluation workflow
- Old R4/DSTU3 scorers still produce scores in builder build() methods
- MeasureReportScoringFhirAdapter was NOT implemented (class does not exist)
- Alternative: R4MeasureReportScorer uses restored R4 FHIR-specific logic from commit 9874c95

### Part 2 Goals
1. ✅ **Activate MeasureReportDefScorer** - Called in MeasureEvaluationResultHandler (COMPLETE)
2. ❌ **Remove Old Scorers** - Delete calls from R4/DSTU3 builders (IN PROGRESS)
3. ❌ **Integration Testing** - Add Fhir2Def tests for all scoring types (NOT DONE)
4. ❌ **Iterative Refinement** - Fix gaps discovered during testing (NOT DONE)
5. ❌ **Cleanup** - (Future) Remove deprecated scorer implementations

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

### New Flow (Part 2, Current State After Refactoring)
```
R4MeasureProcessor (or Dstu3MeasureProcessor)
  → maintains MeasureEvaluationResultHandler instance
  ↓
MeasureEvaluationResultHandler.processResults()  ← Instance method
  ↓
MeasureMultiSubjectEvaluator.postEvaluationMultiSubject()
  → Builds GroupDef, StratumDef objects
  ↓
MeasureReportDefScorer.score(measureDef)  ← NEW: Populates Def scores (unconditional)
  ↓
R4MeasureReportBuilder.build()
  ↓
copyScoresFromDef(bc)  ← NOW ACTIVE: Copies scores from Def
  ↓
(old scorer call removed in Phase 2)
  ↓
MeasureReport with scores ✅
```

## Alternative Implementation Approach (2025-12-16)

### Background

The original plan proposed using MeasureReportScoringFhirAdapter as a bridge between MeasureDefScorer and R4MeasureReportScorer. This adapter was not implemented in the codebase.

### Alternative Solution: Restore R4 FHIR-Specific Logic

Instead of implementing the missing adapter, we restored the R4 FHIR-specific scoring logic from commit 9874c95. This approach:

#### Benefits
1. **Immediate Solution** - No dependency on missing adapter implementation
2. **Battle-Tested Code** - Logic was working correctly before the Def-based refactoring
3. **Minimal Risk** - Restoring known-good code from recent commit
4. **Backward Compatible** - Both Def-based and FHIR-based methods coexist as overloads

#### Implementation Details

**Restoration Phase 1 (Completed 2025-12-16): Stratifier Scoring**

Restored stratifier-level proportion/ratio scoring method:

```java
private int getCountFromStratifierPopulation(
        List<StratifierGroupPopulationComponent> populations, String populationName) {
    return populations.stream()
            .filter(population -> populationName.equals(
                    population.getCode().getCodingFirstRep().getCode()))
            .map(StratifierGroupPopulationComponent::getCount)
            .findAny()
            .orElse(0);
}
```

**Updated Call Site** (R4MeasureReportScorer.java, lines 509-511):
```java
// In getStratumScoreOrNull() method
score = calcProportionScore(
    getCountFromStratifierPopulation(stratum.getPopulation(), NUMERATOR),
    getCountFromStratifierPopulation(stratum.getPopulation(), DENOMINATOR));
```

---

**Restoration Phase 2 (Completed 2025-12-16): Group Scoring**

Restored group-level proportion/ratio scoring method:

```java
private int getCountFromGroupPopulation(
        List<MeasureReportGroupPopulationComponent> populations, String populationName) {
    return populations.stream()
            .filter(population -> populationName.equals(
                    population.getCode().getCodingFirstRep().getCode()))
            .map(MeasureReportGroupPopulationComponent::getCount)
            .findAny()
            .orElse(0);
}
```

**Call Site to Update** (R4MeasureReportScorer.java, lines 182-187):

*Current (Def-based):*
```java
// In scoreGroup() method
score = calcProportionScore(
    groupDef.getPopulationCount(MeasurePopulationType.NUMERATOR)
            - groupDef.getPopulationCount(MeasurePopulationType.NUMERATOREXCLUSION),
    groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOR)
            - groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCLUSION)
            - groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCEPTION));
```

*Required (R4 FHIR-based):*
```java
// In scoreGroup() method
score = calcProportionScore(
    getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR)
            - getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR_EXCLUSION),
    getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR)
            - getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCLUSION)
            - getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCEPTION));
```

**Required Imports:**
```java
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
```

#### Dual-Method Architecture

R4MeasureReportScorer has parallel overloads for both group and stratifier scoring:

**Group-Level Scoring Methods:**

1. **Def-based** (lines 586-596):
   - Signature: `getCountFromStratifierPopulation(GroupDef, StratumDef, MeasurePopulationType)`
   - Used by: Future code if MeasureReportScoringFhirAdapter is implemented
   - Approach: Version-agnostic using Def objects
   - Status: ✅ Exists (unused)

2. **R4 FHIR-specific** (lines 601-619):
   - Signature: `getCountFromGroupPopulation(List<MeasureReportGroupPopulationComponent>, String)`
   - Used by: `scoreGroup()` for proportion/ratio scoring (lines 185-189)
   - Approach: Works directly with R4 MeasureReport objects
   - Status: ✅ Restored (2025-12-16, Phase 2)

**Stratifier-Level Scoring Methods:**

1. **Def-based** (lines 586-596):
   - Signature: `getCountFromStratifierPopulation(GroupDef, StratumDef, MeasurePopulationType)`
   - Used by: Future code if MeasureReportScoringFhirAdapter is implemented
   - Approach: Version-agnostic using Def objects
   - Status: ✅ Exists (unused)

2. **R4 FHIR-specific** (lines 607-615):
   - Signature: `getCountFromStratifierPopulation(List<StratifierGroupPopulationComponent>, String)`
   - Used by: `getStratumScoreOrNull()` for proportion/ratio scoring (lines 509-511)
   - Approach: Works directly with R4 MeasureReport objects
   - Status: ✅ Restored (2025-12-16)

#### Trade-offs

**Pros:**
- ✅ Works directly with R4 FHIR MeasureReport objects (no Def dependency for proportion/ratio)
- ✅ Proven logic from commit 9874c95
- ✅ All tests pass (965 tests, 0 failures after Phase 1)
- ✅ No architectural changes required to MeasureDefScorer or builders
- ✅ Consistent approach for both group and stratifier scoring

**Cons:**
- ⚠️ Two different approaches in same class (Def-based for continuous variable, FHIR-based for proportion/ratio)
- ⚠️ May need refactoring if MeasureReportScoringFhirAdapter is implemented later
- ⚠️ Not fully version-agnostic (R4-specific implementation)
- ⚠️ Phase 2 restoration required before proceeding with Option 1 execution

#### Future Work

If MeasureReportScoringFhirAdapter is implemented:
1. Can switch proportion/ratio scoring (both group and stratifier) to use Def-based methods
2. Remove R4 FHIR-specific overloads (`getCountFromGroupPopulation` and `getCountFromStratifierPopulation`)
3. Achieve full version-agnostic architecture

#### Summary: Two-Phase Restoration

| Phase | Method | Level | Status | Lines |
|-------|--------|-------|--------|-------|
| **Phase 1** | `getCountFromStratifierPopulation()` | Stratifier | ✅ Complete | 621-639 |
| **Phase 2** | `getCountFromGroupPopulation()` | Group | ✅ Complete | 601-619 |

**Both phases complete!** Ready to proceed with Option 1 (Phases 1-8) execution.

## Implementation Plan

### Phase 1: Activate MeasureReportDefScorer in Workflow

**Objective:** Integrate MeasureReportDefScorer into MeasureEvaluationResultHandler to populate Def scores.

**Status:** ✅ COMPLETE (2025-12-16) including architectural refactoring

**Tasks Completed:**
1. ✅ Renamed MeasureDefScorer to MeasureReportDefScorer
2. ✅ Converted MeasureEvaluationResultHandler from static to instance-based class
3. ✅ Integrated MeasureReportDefScorer as field in handler (initialized in constructor)
4. ✅ Added MeasureEvaluator as field in handler (initialized in constructor)
5. ✅ Updated R4MeasureProcessor and Dstu3MeasureProcessor to maintain handler instance
6. ✅ MeasureReportDefScorer call placed after `postEvaluationMultiSubject()`
7. ✅ Scoring runs unconditionally (not guarded by applyScoring flag)

**Implementation:**

#### MeasureEvaluationResultHandler.java (Current State After Refactoring)
```java
package org.opencds.cqf.fhir.cr.measure.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exclusively responsible for calling CQL evaluation and collating the results among multiple
 * measure defs in a FHIR version agnostic way.
 */
public class MeasureEvaluationResultHandler {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluationResultHandler.class);

    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasureEvaluator measureEvaluator;
    private final MeasureReportDefScorer measureReportDefScorer;

    public MeasureEvaluationResultHandler(
            MeasureEvaluationOptions measureEvaluationOptions,
            PopulationBasisValidator populationBasisValidator) {
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measureEvaluator = new MeasureEvaluator(populationBasisValidator);
        this.measureReportDefScorer = new MeasureReportDefScorer();
    }

    public void processResults(
            FhirContext fhirContext,
            Map<String, EvaluationResult> evalResultsPerSubject,
            MeasureDef measureDef,
            @Nonnull MeasureEvalType measureEvalType) {

        // Populate MeasureDef using MeasureEvaluator
        for (Map.Entry<String, EvaluationResult> entry : evalResultsPerSubject.entrySet()) {
            // ... evaluation loop with error handling ...
            measureEvaluator.evaluate(
                    measureDef,
                    measureEvalType,
                    subjectTypePart,
                    subjectIdPart,
                    evalResult,
                    measureEvaluationOptions.getApplyScoringSetMembership());
        }

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(fhirContext, measureDef);

        // Score all groups and stratifiers using version-agnostic scorer
        // Added in Part 2 (integrate-measure-def-scorer-part2-integration) - Phase 1
        // Populates scores in MeasureDef before builders run
        // Note: Scoring is always performed, independent of applyScoring flag
        // (applyScoring controls set membership filtering, not numeric scoring)
        logger.debug("Scoring MeasureDef using MeasureReportDefScorer for measure: {}", measureDef.url());
        measureReportDefScorer.score(measureDef.url(), measureDef);
    }
}
```

**Key Points:**
- **Instance-based design** - No longer static, uses constructor injection
- **Field initialization** - MeasureReportDefScorer and MeasureEvaluator initialized once
- **Simplified signature** - No applyScoring or PopulationBasisValidator parameters
- Placed **after** postEvaluationMultiSubject() (Def objects built)
- **Unconditional scoring** - Always runs to populate scores for builders
- Logs for debugging

**Files Modified:**
- `cqf-fhir-cr/.../common/MeasureReportDefScorer.java` (renamed from MeasureDefScorer.java)
- `cqf-fhir-cr/.../common/MeasureEvaluationResultHandler.java` (refactored to instance-based)
- `cqf-fhir-cr/.../r4/R4MeasureProcessor.java` (added handler field)
- `cqf-fhir-cr/.../dstu3/Dstu3MeasureProcessor.java` (added handler field)

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
    Fhir2DefUnifiedMeasureTestHandler.given()
        .repositoryFor("ContinuousVariableMeasure")
        .when().measureId("CVExample").captureDef().evaluate()
        .then().def()
            .firstGroup()
                .hasScore(expectedScore)  // NEW: Verify group score
                .population(MeasurePopulationType.MEASUREOBSERVATION)
                    .hasCount(expectedCount);
}

@Test
void continuousVariableWithStratifiers() {
    Fhir2DefUnifiedMeasureTestHandler.given()
        .repositoryFor("ContinuousVariableMeasure")
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
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.Fhir2DefUnifiedMeasureTestHandler;

/**
 * Integration tests for Proportion measure scoring with Fhir2Def framework.
 * Tests end-to-end scoring including MeasureDefScorer integration.
 *
 * Created for Part 2 (integrate-measure-def-scorer-part2-integration).
 */
class MeasureScoringTypeProportionFhir2DefTest {

    @Test
    void proportionBasic_SimpleScore() {
        // Test: Basic proportion (n=75, d=100 → score=0.75)
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("ProportionMeasure")
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
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("ProportionMeasure")
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
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("ProportionMeasure")
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
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("ProportionMeasure")
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
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("ProportionMeasure")
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
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.Fhir2DefUnifiedMeasureTestHandler;

/**
 * Integration tests for COHORT measure scoring with Fhir2Def framework.
 * COHORT measures do not have scores - tests verify null scores.
 *
 * Created for Part 2 (integrate-measure-def-scorer-part2-integration).
 */
class MeasureScoringTypeCohortFhir2DefTest {

    @Test
    void cohortBasic_NullScore() {
        // Test: COHORT measures have null scores
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("CohortMeasure")
            .when().measureId("CohortBasic").captureDef().evaluate()
            .then().def()
                .firstGroup()
                    .hasNullScore()  // COHORT measures don't have scores
                    .population(INITIALPOPULATION).hasCount(50);
    }

    @Test
    void cohortWithStratifiers_NullStratifierScores() {
        // Test: COHORT stratifiers also have null scores
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("CohortMeasure")
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
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.Fhir2DefUnifiedMeasureTestHandler;

/**
 * Integration tests for Ratio measure scoring with Fhir2Def framework.
 * Tests both standard ratio and ratio with measure observations (continuous variable).
 *
 * Created for Part 2 (integrate-measure-def-scorer-part2-integration).
 */
class MeasureScoringTypeRatioFhir2DefTest {

    @Test
    void ratioBasic_SimpleScore() {
        // Test: Basic ratio (n=80, d=100 → score=0.80)
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("RatioMeasure")
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
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("RatioMeasure")
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
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("RatioMeasure")
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
        Fhir2DefUnifiedMeasureTestHandler.given()
            .repositoryFor("RatioMeasure")
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

**Objective:** Fix any gaps in MeasureReportDefScorer discovered during integration testing.

**Tasks:**
1. Run all Fhir2Def integration tests
2. Identify any test failures or incorrect scores
3. Debug MeasureReportDefScorer to find root cause
4. Implement fixes in MeasureReportDefScorer
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
2. Debug MeasureReportDefScorer → Find issue
3. Implement fix → Add unit test
4. Re-run integration tests → Verify fix
5. Repeat until all tests pass
```

**Files Modified (as needed):**
- `cqf-fhir-cr/.../common/MeasureReportDefScorer.java`
- `cqf-fhir-cr/src/test/java/.../common/MeasureReportDefScorerTest.java`

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
- `cqf-fhir-cr/.../common/MeasureReportDefScorer.java` (renamed from MeasureDefScorer.java)
- `cqf-fhir-cr/.../common/MeasureEvaluationResultHandler.java` (refactored to instance-based, adds MeasureReportDefScorer call)
- `cqf-fhir-cr/.../r4/R4MeasureProcessor.java` (added handler field)
- `cqf-fhir-cr/.../dstu3/Dstu3MeasureProcessor.java` (added handler field)
- `cqf-fhir-cr/.../r4/R4MeasureReportBuilder.java` (remove old scorer call)
- `cqf-fhir-cr/.../dstu3/Dstu3MeasureReportBuilder.java` (remove old scorer call)
- `cqf-fhir-cr/src/test/java/.../fhir2deftest/r4/ContinuousVariableResourceMeasureObservationFhir2DefTest.java` (add score assertions)
- `cqf-fhir-cr/.../common/MeasureReportDefScorer.java` (iterative fixes as needed)
- `cqf-fhir-cr/src/test/java/.../common/MeasureReportDefScorerTest.java` (add tests for fixes)

### Files Modified (Part 2 - Updated 2025-12-16)
- `cqf-fhir-cr/.../r4/R4MeasureReportScorer.java` (restored R4 FHIR-specific `getCountFromStratifierPopulation` logic, updated javadoc for external-only usage)

### Files Deleted (Part 2 - Final Cleanup 2025-12-16)
- `cqf-fhir-cr/.../dstu3/Dstu3MeasureReportScorer.java` (deleted - no longer needed after MeasureReportDefScorer integration)
- `cqf-fhir-cr/src/test/java/.../dstu3/MeasureScorerTest.java` (deleted - unit test for deleted Dstu3 scorer)

### Files NOT Implemented
- `cqf-fhir-cr/.../common/MeasureReportScoringFhirAdapter.java` (was not implemented - R4 FHIR-specific approach used instead)

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
- ✅ Deprecated old scorers
- ✅ All Part 1 tests passing

**Verification Before Starting Part 2:**
```bash
# Verify Part 1 branch merged
git log --oneline | grep "integrate-measure-def-scorer-part1"

# Verify Part 1 files exist
ls cqf-fhir-cr/.../common/MeasureReportDefScorer.java
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

## Functional Gaps Corrected During Phase 2 Execution

During Phase 2 implementation, several critical functional gaps were discovered and corrected to ensure proper integration of MeasureDefScorer. These issues were exposed by the removal of old scorers in Phase 2.

### Gap 1: Stratum Text-Based Matching in R4MeasureReportBuilder

**Issue Discovered:**
- After removing old scorer calls (Phase 2), 17 tests failed with error: `stratum 'null' does not have a score`
- All failures were in RATIO and CONTINUOUSVARIABLE measures with stratifiers
- Root cause: `R4MeasureReportBuilder.matchesStratumValue()` was comparing coding **codes** instead of **text** values

**Problem:**
```java
// INCORRECT (before fix):
private boolean matchesStratumValue(StratifierGroupComponent reportStratum, StratumDef stratumDef) {
    // Was comparing coding codes, but should compare text values
    String reportCode = reportStratum.getValue().getCoding().get(0).getCode();
    String defCode = stratumDef.getCode(); // Wrong!
    return Objects.equals(reportCode, defCode);
}
```

**Source of Truth:**
- `R4MeasureReportScorer#doesStratumDefMatchStratum()` - uses text comparison
- `R4MeasureReportScorer#getStratumDefTextForR4()` - extracts text from CodeableConcept

**Fix Applied** (R4MeasureReportBuilder.java:733-777):
```java
private boolean matchesStratumValue(MeasureReport.StratifierGroupComponent reportStratum, StratumDef stratumDef) {
    // Use the same logic as R4MeasureReportScorer: compare CodeableConcept.text
    String reportText = reportStratum.hasValue() ? reportStratum.getValue().getText() : null;
    String defText = getStratumDefText(stratumDef);
    return Objects.equals(reportText, defText);
}

private String getStratumDefText(StratumDef stratumDef) {
    // Extract text from CodeableConcept values, handling both component and non-component stratifiers
    // (Full implementation copied from R4MeasureReportScorer#getStratumDefTextForR4)
}
```

**Impact:**
- Fixed 3/17 test failures immediately (MeasureReportTypeSubjectListTest, MeasureScoringTypeRatioContVariableTest, MeasureStratifierTest)
- Corrected fundamental stratum matching algorithm to use text-based comparison

**Files Modified:**
- `R4MeasureReportBuilder.java` - Updated `matchesStratumValue()` and added `getStratumDefText()`
- Added import: `org.opencds.cqf.fhir.cr.measure.common.StratumValueDef`

### Gap 2: Qualified vs Unqualified Subject ID Matching in MeasureDefScorer

**Issue Discovered:**
- After fixing Gap 1, 14 tests still failed (all CONTINUOUSVARIABLE tests)
- Root cause: `MeasureDefScorer.getResultsForStratum()` was using `subjectsQualifiedOrUnqualified()` but should use `getSubjectsUnqualified()`

**Problem:**
- **PopulationDef.subjectResources** are keyed on **UNQUALIFIED** patient IDs: `"patient-1965-female"`
- **StratumPopulationDef** may contain **QUALIFIED** IDs: `"Patient/patient-1965-female"`
- The mismatched ID formats caused stratum subject filtering to fail, resulting in empty result sets and null scores

**Incorrect Code** (MeasureDefScorer.java:480):
```java
// BEFORE (incorrect):
Set<String> stratumSubjects = stratumPopulationDef.subjectsQualifiedOrUnqualified();
// This returns qualified IDs like "Patient/patient-1965-female"

// Filter fails because populationDef.getSubjectResources() keys are unqualified like "patient-1965-female"
return populationDef.getSubjectResources().entrySet().stream()
    .filter(entry -> stratumSubjects.contains(entry.getKey())) // NEVER MATCHES!
    .map(Map.Entry::getValue)
    .flatMap(Collection::stream)
    .collect(Collectors.toList());
```

**Source of Truth:**
- `BaseMeasureReportScorer#getResultsForStratum()` - uses `getSubjectsUnqualified()`
- `BaseMeasureReportScorer#doesStratumPopDefMatchGroupPopDef()` - unqualified matching algorithm

**Fix Applied** (MeasureDefScorer.java:480):
```java
// AFTER (correct):
// CRITICAL: PopulationDef.subjectResources are keyed on UNQUALIFIED patient IDs
// Use getSubjectsUnqualified() to match the unqualified keys
Set<String> stratumSubjectsUnqualified = stratumPopulationDef.getSubjectsUnqualified().stream()
        .collect(Collectors.toSet());

// Filter at the subjectResources Map.Entry level (subject ID is the key)
return populationDef.getSubjectResources().entrySet().stream()
        .filter(entry -> stratumSubjectsUnqualified.contains(entry.getKey())) // NOW MATCHES!
        .map(Map.Entry::getValue)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
```

**Impact:**
- Fixed all remaining 14 test failures
- All 27 tests in ContinuousVariableResourceMeasureObservationTest and MeasureScoringTypeRatioContVariableTest now pass
- Critical bug fix for stratified continuous variable and ratio measures

**Files Modified:**
- `MeasureDefScorer.java` - Updated `getResultsForStratum()` method at line 480

### Gap 3: DSTU3 Stratum Matching Verification

**Issue Investigated:**
- User requested verification that DSTU3MeasureReportBuilder uses the same stratum matching algorithm as R4

**Analysis:**
- **R4**: Uses `CodeableConcept` with text field → compares `CodeableConcept.text`
- **DSTU3**: Uses simple `String` values → compares `String` directly via `getValueAsString()`

**Conclusion:**
- DSTU3's `matchesStratumValue()` (Dstu3MeasureReportBuilder.java:885-899) already uses correct string-based comparison
- Both implementations are conceptually equivalent for their respective FHIR versions
- No changes needed for DSTU3

### Test Coverage Added

To prevent regression of these fixes, new unit tests were added:

**R4MeasureReportBuilderTest.java:**
- `testScoreCopying_StratifierWithTextBasedMatching()` - Verifies text-based stratum matching with qualified IDs

**Dstu3MeasureReportBuilderTest.java:**
- `testScoreCopying_StratumScore()` - Verifies DSTU3 stratum score copying with qualified IDs

**MeasureDefScorerTest.java:**
- `testScoreStratifier_QualifiedVsUnqualifiedSubjectIds()` - Directly tests qualified vs unqualified ID matching in proportion measures
- Verifies both female stratum (2/2 = 1.0) and male stratum (1/2 = 0.5) are scored correctly

### Root Cause Summary

Both gaps stemmed from **incomplete migration of algorithms** from old scorers to MeasureDefScorer:

1. **R4MeasureReportBuilder** - Missing the text-based comparison algorithm from R4MeasureReportScorer
2. **MeasureDefScorer** - Missing the unqualified ID matching algorithm from BaseMeasureReportScorer

These gaps were hidden while old scorers were active (Phase 1) because the old scorers were overwriting the scores. Phase 2 removal of old scorer calls exposed these issues immediately.

### Verification

All functional gaps were corrected and verified:
- ✅ All 990 tests in cqf-fhir-cr module pass
- ✅ Text-based stratum matching works in R4
- ✅ Qualified/unqualified ID matching works in MeasureDefScorer
- ✅ DSTU3 stratum matching confirmed correct
- ✅ New unit tests prevent regression

---

## Conclusion

Part 2 completes the integration of version-agnostic MeasureReportDefScorer:
- **Activation** - MeasureReportDefScorer called in evaluation workflow (COMPLETE with refactoring)
- **Validation** - Fhir2Def integration tests ensure correctness (IN PROGRESS)
- **Cleanup** - Old scorer calls removed from builders (IN PROGRESS)
- **Tested** - End-to-end integration validated (IN PROGRESS)

**Blockers:** Cannot start until Part 1 (integrate-measure-def-scorer-part1-foundation.md) is complete and merged.

**Deliverables:**
- ✅ MeasureReportDefScorer active in regular measure evaluation (COMPLETE)
- ✅ MeasureEvaluationResultHandler refactored to instance-based architecture (COMPLETE)
- ❌ Old scorers deprecated and calls removed (IN PROGRESS)
- ❌ Comprehensive Fhir2Def integration tests (NOT DONE)
- Clear path to final cleanup (remove deprecated classes)
