# PRP: Integrate MeasureDefScorer - Part 1: Foundation, Refactoring & External API

## Metadata
- **Title**: MeasureDefScorer Foundation - Infrastructure, Bug Fixes, and cdr-cr API (No Behavioral Changes)
- **Status**: Planning Complete - Ready for Implementation
- **Priority**: High (Architecture Consolidation & External API Delivery)
- **Estimated Effort**: 3-4 days
- **Target Branch**: ld-20251212-measure-def-scorer-integrator-part-1
- **Planning Date**: 2025-12-12
- **Dependencies**: ✅ PRs #851, #852 (version-agnostic scorer, observation cache)
- **Dependents**: Part 2 (Final MeasureDefScorer Integration & Enhanced Testing)

## Executive Summary

Part 1 completes the foundation for version-agnostic measure scoring without changing existing behavior. This PRP:

1. **Completes MeasureDefScorer** - Add COHORT/COMPOSITE support, fix critical bugs
2. **Creates External API** - MeasureReportScoringFhirAdapter for cdr-cr project
3. **Adds Score Copying Infrastructure** - copyScoresFromDef() in R4/DSTU3 builders (inactive in Part 1)
4. **Fixes Critical Bugs** - Text-based stratum matching and qualified/unqualified ID handling
5. **Deprecates Old Scorers** - Clear migration path with documentation
6. **Comprehensive Testing** - 35+ new unit tests including gap prevention tests

**Key Constraint:** Old R4/DSTU3 scorers remain the source of truth for regular measure evaluation. New scoring paths only active in MeasureReportScoringFhirAdapter (cdr-cr API).

**Part 2 Dependency:** Part 2 will:
- Activate MeasureDefScorer in MeasureEvaluationResultHandler (makes Def scores the source of truth)
- Remove old scorer calls from builders (copyScoresFromDef becomes active)
- Enhance Def integration testing to capture and verify scoring behavior
- Add comprehensive end-to-end tests for all measure types

**⚠️ CRITICAL**: This PRP includes functional gap fixes discovered during Part 2 execution in the cr1 codebase. These MUST be fixed in Part 1 to prevent 31 test failures when old scorers are removed in Part 2.

### Strategy
Safe, incremental refactoring with zero behavior change for existing workflows. New API provides immediate value to cdr-cr project while preparing infrastructure for Part 2.

## Problem Statement

### Current State (After PRs #851, #852)
- MeasureDefScorer exists with PROPORTION, RATIO, CONTINUOUSVARIABLE support
- MeasureObservationStratumCache optimizes ratio observation scoring
- StratumDef and GroupDef have score fields with getters/setters
- GroupDef.getMeasureScore() handles improvement notation
- Old version-specific scorers still produce all scores

### Part 1 Goals
1. **Complete MeasureDefScorer** - Add COHORT/COMPOSITE cases, fix critical bugs
2. **Create External API** - MeasureReportScoringFhirAdapter for cdr-cr project
3. **Prepare Builders** - Add copyScoresFromDef() methods (inactive, null scores)
4. **Fix Critical Bugs** - Text-based stratum matching, qualified/unqualified ID handling
5. **Deprecate Old Scorers** - Mark for future removal, keep functional
6. **Comprehensive Testing** - Unit tests for all new components and bug fixes

### Non-Goals (Part 2)
- Integration with MeasureEvaluationResultHandler (Part 2)
- Removing old scorer calls from builders (Part 2)
- Enhanced Def integration testing framework (Part 2)
- End-to-end scoring validation tests (Part 2)

## Architecture Overview

### Current Flow (After PRs #851, #852)
```
MeasureEvaluationResultHandler
  ↓
MeasureMultiSubjectEvaluator (builds Def objects, NO scores)
  ↓
R4MeasureReportBuilder.build()
  ↓
measureReportScorer.score() ← OLD: Produces scores
  ↓
MeasureReport with scores ✅
```

### Part 1 Flow - Two Independent Workflows

#### Workflow 1: Regular clinical-reasoning Evaluation (Unchanged)
```
MeasureEvaluationResultHandler
  ↓
MeasureMultiSubjectEvaluator (builds Def objects, NO scores)
  ↓
R4MeasureReportBuilder.build()
  ↓
copyScoresFromDef(bc) ← NEW: Does nothing (null scores in Def objects)
  ↓
measureReportScorer.score() ← EXISTING: Produces scores (unchanged)
  ↓
MeasureReport with scores ✅
```

#### Workflow 2: cdr-cr Post-hoc Scoring (NEW)
```
cdr-cr retrieves MeasureReport from database (has counts, no scores)
  ↓
MeasureReportScoringFhirAdapter.score(measure, measureReport)
  ↓
Build MeasureDef from Measure structure
  ↓
Populate MeasureDef with counts from MeasureReport
  ↓
MeasureDefScorer.score(measureDef) ← NEW: Populates Def scores
  ↓
copyScoresToReport(measureDef, measureReport) ← NEW: Copies to report
  ↓
MeasureReport with scores ✅
```

## Critical Functional Gaps from Part 2

Based on lessons learned from Part 2 execution in the cr1 codebase, the following critical functional gaps MUST be addressed in Part 1 to prevent test failures when old scorers are removed in Part 2.

### Gap 1: Text-Based Stratum Matching in R4MeasureReportBuilder

**Issue:**
The `matchesStratumValue()` method uses coding **codes** for comparison, but should use **text** values to match how the old R4MeasureReportScorer works.

**Impact if Not Fixed:**
When old scorers are removed in Part 2, 17 tests will fail with "stratum 'null' does not have a score" errors in RATIO and CONTINUOUSVARIABLE measures with stratifiers.

**Root Cause:**
Incomplete migration of the text-based comparison algorithm from R4MeasureReportScorer#doesStratumDefMatchStratum() and R4MeasureReportScorer#getStratumDefTextForR4().

**Fix Required:**
Update `R4MeasureReportBuilder.matchesStratumValue()` to use text-based comparison:

```java
private boolean matchesStratumValue(MeasureReport.StratifierGroupComponent reportStratum, StratumDef stratumDef) {
    // Use the same logic as R4MeasureReportScorer: compare CodeableConcept.text
    String reportText = reportStratum.hasValue() ? reportStratum.getValue().getText() : null;
    String defText = getStratumDefText(stratumDef);
    return Objects.equals(reportText, defText);
}

private String getStratumDefText(StratumDef stratumDef) {
    // Extract text from StratumValueDef, handling both component and non-component stratifiers
    // Full implementation based on R4MeasureReportScorer#getStratumDefTextForR4
    String stratumText = null;

    for (StratumValueDef valuePair : stratumDef.valueDefs()) {
        var value = valuePair.value();
        var componentDef = valuePair.def();

        // Handle CodeableConcept values
        if (value.getValueClass().equals(org.hl7.fhir.r4.model.CodeableConcept.class)) {
            if (stratumDef.isComponent()) {
                // component stratifier: use code text
                stratumText = componentDef != null && componentDef.code() != null
                        ? componentDef.code().text()
                        : null;
            } else {
                // non-component: extract text from CodeableConcept value
                if (value.getValue() instanceof org.hl7.fhir.r4.model.CodeableConcept codeableConcept) {
                    stratumText = codeableConcept.getText();
                }
            }
        } else if (stratumDef.isComponent()) {
            // Component with non-CodeableConcept value: convert to string
            stratumText = value != null ? value.getValueAsString() : null;
        } else {
            // Non-component with non-CodeableConcept value: convert to string
            stratumText = value != null ? value.getValueAsString() : null;
        }
    }

    return stratumText;
}
```

**Files to Update:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilder.java`
- Add import: `org.opencds.cqf.fhir.cr.measure.common.StratumValueDef`

**Verification Test:**
- `R4MeasureReportBuilderTest.testScoreCopying_StratifierWithTextBasedMatching()` - Verifies text-based matching with qualified IDs
- Reference: cr1 cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilderTest.java:400+

---

### Gap 2: Qualified vs Unqualified Subject ID Matching in MeasureDefScorer

**Issue:**
The existing `MeasureDefScorer.getResultsForStratum()` (line 475) uses `subjectsQualifiedOrUnqualified()` but should use `getSubjectsUnqualified()` because:
- `PopulationDef.subjectResources` are keyed on **UNQUALIFIED** IDs: `"patient-1965-female"`
- `StratumPopulationDef` may contain **QUALIFIED** IDs: `"Patient/patient-1965-female"`

**Impact if Not Fixed:**
After Gap 1 is fixed, 14 tests will still fail (all CONTINUOUSVARIABLE tests) because stratum subject filtering fails, resulting in empty result sets and null scores.

**Root Cause:**
Incomplete migration of the unqualified ID matching algorithm from BaseMeasureReportScorer#getResultsForStratum() and BaseMeasureReportScorer#doesStratumPopDefMatchGroupPopDef().

**Current Buggy Code (line 475):**
```java
// INCORRECT:
Set<String> stratumSubjects = stratumPopulationDef.subjectsQualifiedOrUnqualified();
// This returns qualified IDs like "Patient/patient-1965-female"

// Filter fails because populationDef.getSubjectResources() keys are unqualified
return populationDef.getSubjectResources().entrySet().stream()
    .filter(entry -> stratumSubjects.contains(entry.getKey())) // NEVER MATCHES!
    .map(Map.Entry::getValue)
    .flatMap(Collection::stream)
    .collect(Collectors.toList());
```

**Fix Required:**
Update `MeasureDefScorer.getResultsForStratum()` to use unqualified IDs:

```java
// CORRECT (line ~475-488):
// CRITICAL: PopulationDef.subjectResources are keyed on UNQUALIFIED patient IDs (e.g., "patient-1965-female")
// but StratumPopulationDef.subjectsQualifiedOrUnqualified() may contain QUALIFIED IDs (e.g., "Patient/patient-1965-female")
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

**Files to Update:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorer.java` (line 475)

**Verification Test:**
- `MeasureDefScorerTest.testScoreStratifier_QualifiedVsUnqualifiedSubjectIds()` - Tests qualified vs unqualified ID matching in proportion measures
- Verifies both female stratum (2/2 = 1.0) and male stratum (1/2 = 0.5) are scored correctly with mixed ID formats
- Reference: cr1 cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorerTest.java:1176+

---

### Gap 3: DSTU3 Stratum Matching Verification

**Status:** No changes needed - DSTU3's `matchesStratumValue()` already uses correct string-based comparison via `getValueAsString()`.

**Analysis:**
- **R4**: Uses `CodeableConcept` with text field → compares `CodeableConcept.text`
- **DSTU3**: Uses simple `String` values → compares `String` directly via `getValueAsString()`
- Both implementations are conceptually equivalent for their respective FHIR versions

**Verification Test:**
- `Dstu3MeasureReportBuilderTest.testScoreCopying_StratumScore()` - Verifies DSTU3 stratum score copying with qualified IDs works correctly

---

### Why Fix These Gaps in Part 1?

These bugs are **hidden during Part 1** because old scorers are still active and overwriting the scores. They become **immediately visible in Part 2** when old scorer calls are removed. By fixing them in Part 1:

- ✅ Part 2 can proceed smoothly without debugging
- ✅ Prevents 31 test failures (17 + 14) in Part 2
- ✅ Tests added in Part 1 serve as regression prevention
- ✅ Code is correct from the start

**Source of Fixes:**
All implementations verified in cr1 codebase at:
- `/Users/lukedegruchy/development/smilecdr/git/clinical-reasoning-repos/clinical-reasoning-only/cr1`

## Implementation Plan

### Phase 1: Complete COHORT/COMPOSITE Support and Fix Gap 2

**File:** `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorer.java`

**Changes:**

1. **Add COHORT/COMPOSITE cases** in `calculateGroupScore()` switch statement (around line 122-149):
```java
@Nullable
private Double calculateGroupScore(String measureUrl, GroupDef groupDef, MeasureScoring measureScoring) {
    switch (measureScoring) {
        case PROPORTION:
        case RATIO:
            // Existing implementation...

        case CONTINUOUSVARIABLE:
            // Existing implementation...

        case COHORT:
            // COHORT measures don't have scores by design
            return null;

        case COMPOSITE:
            // COMPOSITE measure scoring not yet implemented
            // TODO: Implement composite scoring in future enhancement
            return null;

        default:
            logger.warn("Unsupported measure scoring type: {} for measure: {}", measureScoring, measureUrl);
            return null;
    }
}
```

2. **Fix Gap 2** - In `getResultsForStratum()` method (line 475):
```java
// CRITICAL: PopulationDef.subjectResources are keyed on UNQUALIFIED patient IDs
// Use getSubjectsUnqualified() to match the unqualified keys
Set<String> stratumSubjectsUnqualified = stratumPopulationDef.getSubjectsUnqualified().stream()
        .collect(Collectors.toSet());

// Filter at the subjectResources Map.Entry level (subject ID is the key)
return populationDef.getSubjectResources().entrySet().stream()
        .filter(entry -> stratumSubjectsUnqualified.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
```

**Why:**
- Makes code explicit about handling COHORT/COMPOSITE scoring types
- Fixes critical bug where qualified IDs don't match unqualified keys in subjectResources
- Without this fix, 14 CONTINUOUSVARIABLE tests will fail in Part 2

**Files Modified:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorer.java`

---

### Phase 2: R4 Builder Score Copying Logic with Gap 1 Fix

**File:** `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilder.java`

**Changes:**

1. Add `copyScoresFromDef(R4MeasureReportBuilderContext bc)` private method:
```java
/**
 * Copy scores from MeasureDef to MeasureReport.
 * Added in Part 1 (integrate-measure-def-scorer-part1-foundation).
 * In Part 1, this does nothing because Def objects have null scores.
 * In Part 2 (integrate-measure-def-scorer-part2-integration), this becomes active
 * when MeasureDefScorer is called in MeasureEvaluationResultHandler.
 *
 * Logic is driven by Def objects, matching report structures by ID.
 * Logs warnings when matching report structures are not found.
 *
 * @param bc the builder context
 */
private void copyScoresFromDef(R4MeasureReportBuilderContext bc) {
    var report = bc.report();
    var measureDef = bc.measureDef();

    // Iterate through GroupDefs (drive from Def side)
    for (var groupDef : measureDef.groups()) {
        // Find matching report group by ID
        var reportGroup = report.getGroup().stream()
                .filter(rg -> groupDef.id().equals(rg.getId()))
                .findFirst()
                .orElse(null);

        if (reportGroup == null) {
            logger.warn("No matching MeasureReport group found for GroupDef with id: {}", groupDef.id());
            continue;
        }

        // Copy group-level score
        Double groupScore = groupDef.getMeasureScore();
        if (groupScore != null) {
            reportGroup.getMeasureScore().setValue(groupScore);
        }

        // Copy stratifier scores
        copyStratifierScores(reportGroup, groupDef);
    }
}
```

2. Add `copyStratifierScores()` helper method:
```java
/**
 * Copy stratifier scores from StratifierDef objects to MeasureReport stratifiers.
 * Logic is driven by StratifierDef objects, matching report stratifiers by ID.
 *
 * @param reportGroup the MeasureReport group component
 * @param groupDef the GroupDef containing stratifier scores
 */
private void copyStratifierScores(MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
    // Iterate through StratifierDefs (drive from Def side)
    for (var stratifierDef : groupDef.stratifiers()) {
        // Find matching report stratifier by ID
        var reportStratifier = reportGroup.getStratifier().stream()
                .filter(rs -> stratifierDef.id().equals(rs.getId()))
                .findFirst()
                .orElse(null);

        if (reportStratifier == null) {
            logger.warn("No matching MeasureReport stratifier found for StratifierDef with id: {}",
                    stratifierDef.id());
            continue;
        }

        // Iterate through StratumDefs (drive from Def side)
        for (var stratumDef : stratifierDef.getStratum()) {
            // Find matching report stratum by comparing value strings
            var reportStratum = reportStratifier.getStratum().stream()
                    .filter(rs -> matchesStratumValue(rs, stratumDef))
                    .findFirst()
                    .orElse(null);

            if (reportStratum == null) {
                logger.debug("No matching MeasureReport stratum found for StratumDef in stratifier: {}",
                        stratifierDef.id());
                continue;
            }

            // Copy stratum score
            Double stratumScore = stratumDef.getScore();
            if (stratumScore != null) {
                reportStratum.getMeasureScore().setValue(stratumScore);
            }
        }
    }
}
```

3. **Add Gap 1 Fix** - Add `matchesStratumValue()` with text-based comparison:
```java
/**
 * Check if a MeasureReport stratum matches a StratumDef by comparing text representations.
 * Uses text-based comparison to match R4MeasureReportScorer behavior.
 * Added in Part 1 to fix Gap 1 (text-based stratum matching).
 *
 * @param reportStratum the MeasureReport stratum
 * @param stratumDef the StratumDef
 * @return true if values match
 */
private boolean matchesStratumValue(MeasureReport.StratifierGroupComponent reportStratum, StratumDef stratumDef) {
    // Use the same logic as R4MeasureReportScorer: compare CodeableConcept.text
    String reportText = reportStratum.hasValue() ? reportStratum.getValue().getText() : null;
    String defText = getStratumDefText(stratumDef);
    return Objects.equals(reportText, defText);
}

/**
 * Extract text representation from StratumDef for matching.
 * Based on R4MeasureReportScorer#getStratumDefTextForR4.
 * Added in Part 1 to fix Gap 1 (text-based stratum matching).
 *
 * @param stratumDef the StratumDef
 * @return text representation of the stratum value
 */
private String getStratumDefText(StratumDef stratumDef) {
    String stratumText = null;

    for (StratumValueDef valuePair : stratumDef.valueDefs()) {
        var value = valuePair.value();
        var componentDef = valuePair.def();

        // Handle CodeableConcept values
        if (value.getValueClass().equals(org.hl7.fhir.r4.model.CodeableConcept.class)) {
            if (stratumDef.isComponent()) {
                // component stratifier: use code text
                stratumText = componentDef != null && componentDef.code() != null
                        ? componentDef.code().text()
                        : null;
            } else {
                // non-component: extract text from CodeableConcept value
                if (value.getValue() instanceof org.hl7.fhir.r4.model.CodeableConcept codeableConcept) {
                    stratumText = codeableConcept.getText();
                }
            }
        } else if (stratumDef.isComponent()) {
            // Component with non-CodeableConcept value: convert to string
            stratumText = value != null ? value.getValueAsString() : null;
        } else {
            // Non-component with non-CodeableConcept value: convert to string
            stratumText = value != null ? value.getValueAsString() : null;
        }
    }

    return stratumText;
}
```

4. Insert call to `copyScoresFromDef(bc)` in `build()` method BEFORE existing scorer call (around line 96):
```java
// NEW: Copy scores from Def objects (Part 1: does nothing, null scores)
// This becomes active in Part 2 when MeasureDefScorer populates Def scores
copyScoresFromDef(bc);

// KEEP: Old scorer still produces scores (Part 1: source of truth)
this.measureReportScorer.score(measure.getUrl(), measureDef, bc.report());
```

**Why:**
- Prepares infrastructure for Def-first scoring
- In Part 1, this does nothing because MeasureDefScorer isn't called during evaluation (scores are null)
- Gap 1 fix ensures text-based stratum matching works correctly (prevents 17 test failures in Part 2)

**Files Modified:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilder.java`
- Add import: `org.opencds.cqf.fhir.cr.measure.common.StratumValueDef`

---

### Phase 3: DSTU3 Builder Score Copying Logic

**File:** `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportBuilder.java`

**Changes:**

1. Add `copyScoresFromDef(MeasureDef measureDef)` private method (similar to R4 but simpler)
2. Add `copyStratifierScores()` helper method (DSTU3 has no component stratifiers)
3. Add `matchesStratumValue()` helper method (uses String comparison, simpler than R4)
4. Insert call to `copyScoresFromDef(measureDef)` BEFORE existing scorer call

**Key Differences from R4:**
- DSTU3 uses `setMeasureScore(Double)` instead of Quantity wrapper
- DSTU3 has no component stratifiers (simpler matching logic)
- Uses instance field `this.report` instead of context

**Why:**
- DSTU3 version of Phase 2 logic
- Same principles apply
- Gap 3 verification test confirms DSTU3 matching already works correctly

**Files Modified:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportBuilder.java`

---

### Phase 4: MeasureReportScoringFhirAdapter (Critical - cdr-cr API)

**Purpose:** Version-agnostic utility for post-hoc scoring of FHIR MeasureReports (cdr-cr use case).

**Architecture:** Interface-based design with version-specific implementations in separate packages.

**Files Created:**

1. **`IMeasureToReportScoreFhirHandler`** (common package)
   - Version-agnostic interface defining the scoring contract
   - Provides default implementation of score() method
   - Methods: getMeasureDefScorer(), getMeasureUrl(), buildMeasureDef(), populateMeasureDefFromReport(), copyScoresToReport(), validateGroupCounts()

2. **`R4MeasureToReportScoreFhirHandler`** (r4 package)
   - R4-specific implementation as a record
   - Handles R4 Measure/MeasureReport types
   - Uses R4MeasureDefBuilder
   - Populates MeasureDef with synthetic subjects matching report counts
   - Copies scores back using R4 Quantity type

3. **`Dstu3MeasureToReportScoreFhirHandler`** (dstu3 package)
   - DSTU3-specific implementation as a record
   - Handles DSTU3 Measure/MeasureReport types
   - Uses Dstu3MeasureDefBuilder
   - Populates MeasureDef with synthetic subjects matching report counts
   - Copies scores back using Double type directly

4. **`MeasureReportScoringFhirAdapter`** (common package)
   - Static utility class with version auto-detection
   - Shared MeasureDefScorer instance
   - Static R4 and DSTU3 handler instances
   - Delegates to appropriate handler based on FHIR version

**Structure:**
```java
// Interface (common)
public interface IMeasureToReportScoreFhirHandler<M extends IBaseResource, R extends IBaseResource> {
    default void score(M measure, R measureReport) { /* ... */ }
    MeasureDefScorer getMeasureDefScorer();
    String getMeasureUrl(M measure);
    MeasureDef buildMeasureDef(M measure);
    void populateMeasureDefFromReport(MeasureDef measureDef, R measureReport);
    void copyScoresToReport(MeasureDef measureDef, R measureReport);
    default void validateGroupCounts(int measureDefGroupCount, int measureReportGroupCount) { /* ... */ }
}

// R4 Implementation (r4 package)
public record R4MeasureToReportScoreFhirHandler(MeasureDefScorer measureDefScorer)
        implements IMeasureToReportScoreFhirHandler<Measure, MeasureReport> {
    // R4-specific implementation
}

// DSTU3 Implementation (dstu3 package)
public record Dstu3MeasureToReportScoreFhirHandler(MeasureDefScorer measureDefScorer)
        implements IMeasureToReportScoreFhirHandler<Measure, MeasureReport> {
    // DSTU3-specific implementation
}

// Facade (common)
public class MeasureReportScoringFhirAdapter {
    private static final MeasureDefScorer MEASURE_DEF_SCORER = new MeasureDefScorer();
    private static final R4MeasureToReportScoreFhirHandler R4_SCORER =
        new R4MeasureToReportScoreFhirHandler(MEASURE_DEF_SCORER);
    private static final Dstu3MeasureToReportScoreFhirHandler DSTU3_SCORER =
        new Dstu3MeasureToReportScoreFhirHandler(MEASURE_DEF_SCORER);

    public static void score(IBaseResource measure, IBaseResource measureReport) {
        // Version detection and delegation
    }
}
```

**Key Logic Flow:**
1. Auto-detect FHIR version from Measure resource
2. Delegate to version-specific handler (R4MeasureToReportScoreFhirHandler or Dstu3MeasureToReportScoreFhirHandler)
3. Handler builds MeasureDef from Measure structure
4. Handler populates MeasureDef with synthetic subjects matching MeasureReport counts
5. Handler calls MeasureDefScorer.score() to compute scores
6. Handler copies computed scores back to MeasureReport

**Example Usage (cdr-cr project):**
```java
// Before (version-specific):
public void scoreMeasureReport(Measure theMeasure, MeasureReport theMeasureReport) {
    R4MeasureDefBuilder measureDefBuilder = new R4MeasureDefBuilder();
    R4MeasureReportScorer scorer = new R4MeasureReportScorer();
    scorer.score(theMeasure.getUrl(), measureDefBuilder.build(theMeasure), theMeasureReport);
}

// After (version-agnostic):
public void scoreMeasureReport(Measure theMeasure, MeasureReport theMeasureReport) {
    MeasureReportScoringFhirAdapter.score(theMeasure, theMeasureReport);
}
```

**Why:**
- This is THE primary deliverable for Part 1
- Provides immediate value to cdr-cr project for post-hoc scoring
- This is the ONLY place in Part 1 where MeasureDefScorer is actually used (bypassing old scorers)
- Version-agnostic API reduces maintenance burden
- Interface-based design allows for clean separation of concerns
- Record pattern provides concise, immutable implementations

**Files Created:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/IMeasureToReportScoreFhirHandler.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureToReportScoreFhirHandler.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureToReportScoreFhirHandler.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureReportScoringFhirAdapter.java`

**Implementation Notes:**
- Based on improved architecture from cr1 project (iterative refinement)
- Version-specific handlers placed in r4/dstu3 packages (not common)
- Uses modern Java record pattern for handler implementations
- Synthetic subjects approach: handlers create placeholder resources to populate PopulationDef.subjectResources matching MeasureReport counts

---

### Phase 5: Deprecation Annotations

**Files:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportScorer.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/IMeasureReportScorer.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/BaseMeasureReportScorer.java`

**Changes:**
Add `@Deprecated(since = "3.x.x", forRemoval = true)` to each class with JavaDoc:
```java
/**
 * Evaluation of Measure Report Data showing raw CQL criteria results compared to resulting Measure Report.
 *
 * @deprecated This class is deprecated and will be removed in a future release.
 *             For external consumers (e.g., cdr-cr project), use
 *             {@link MeasureReportScoringFhirAdapter#score(IBaseResource, IBaseResource)}
 *             for version-agnostic post-hoc scoring.
 *             For internal use, this class will be replaced by {@link MeasureDefScorer}
 *             integrated into the evaluation workflow in Part 2.
 *             See: integrate-measure-def-scorer-part2-integration PRP
 */
@Deprecated(since = "3.x.x", forRemoval = true)
@SuppressWarnings("squid:S1135")
public class R4MeasureReportScorer extends BaseMeasureReportScorer<MeasureReport> {
    // ... existing implementation unchanged ...
}
```

**Why:**
- Clear migration path for consumers
- Signals that old scorers are on deprecation track without breaking anything
- Points to new API and future Part 2 changes

**Files Modified:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportScorer.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/IMeasureReportScorer.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/BaseMeasureReportScorer.java`

---

### Phase 6: Comprehensive Testing

**Test Files to Create/Enhance:**

#### 1. MeasureDefScorerTest.java (enhance existing)
- Add test for COHORT scoring (returns null)
- Add test for COMPOSITE scoring (returns null)
- **CRITICAL (Gap 2 Fix)**: Add `testScoreStratifier_QualifiedVsUnqualifiedSubjectIds()`
  - Tests qualified vs unqualified ID matching in proportion measures
  - Verifies female stratum (2/2 = 1.0) and male stratum (1/2 = 0.5) with mixed ID formats
  - Reference: cr1 MeasureDefScorerTest.java:1176+
- Verify no regression in existing 20+ tests

#### 2. R4MeasureReportBuilderTest.java (create if missing, else enhance)
- Test copyScoresFromDef with null scores (Part 1 behavior - does nothing)
- Test copyScoresFromDef with populated scores (simulating Part 2)
- Test stratifier score copying
- **CRITICAL (Gap 1 Fix)**: Add `testScoreCopying_StratifierWithTextBasedMatching()`
  - Verifies text-based stratum matching with qualified IDs
  - Ensures CodeableConcept.text comparison works correctly
  - Reference: cr1 R4MeasureReportBuilderTest.java:400+
- Test improvement notation (should be handled by GroupDef.getMeasureScore())
- Test multiple groups
- Test component vs single-value stratifiers
- Test null handling and missing matches

#### 3. Dstu3MeasureReportBuilderTest.java (create if missing, else enhance)
- Similar tests to R4 but for DSTU3
- Test DSTU3-specific API differences (setMeasureScore vs Quantity)
- **CRITICAL (Gap 3 Verification)**: Add `testScoreCopying_StratumScore()`
  - Verifies DSTU3 stratum score copying with qualified IDs

#### 4. MeasureReportScoringFhirAdapterTest.java (CREATE - critical)
**Version Detection Tests:**
- Test R4 auto-detection
- Test DSTU3 auto-detection
- Test version mismatch error (measure R4, report DSTU3)
- Test R5 not implemented error

**R4 Scoring Tests:**
- Test proportion measure scoring
- Test ratio measure scoring
- Test ratio with measure observations scoring
- Test cohort measure (null scores)
- Test stratifiers with scores
- Test improvement notation (increase vs decrease)
- Test multiple groups
- Test zero denominator (null score)

**DSTU3 Scoring Tests:**
- Test proportion measure scoring
- Test ratio measure scoring
- Test cohort measure (null scores)
- Test stratifiers with scores
- Test improvement notation

**Error Handling Tests:**
- Test null measure input
- Test null measureReport input
- Test group count mismatch

**Integration Test:**
- Simulate real-world cdr-cr usage (retrieve report from DB, score it, verify results)

**Target: 19+ comprehensive tests**

**Files Created/Modified:**
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorerTest.java` (enhance)
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilderTest.java` (create or enhance)
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportBuilderTest.java` (create or enhance)
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/MeasureReportScoringFhirAdapterTest.java` (CREATE)

---

## Files Summary

### Files Created (Part 1)
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureReportScoringFhirAdapter.java`
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/MeasureReportScoringFhirAdapterTest.java`
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilderTest.java` (if doesn't exist)
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportBuilderTest.java` (if doesn't exist)

### Files Modified (Part 1)
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorer.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilder.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportBuilder.java`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java` (@Deprecated)
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportScorer.java` (@Deprecated)
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/IMeasureReportScorer.java` (@Deprecated)
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/BaseMeasureReportScorer.java` (@Deprecated)
- `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorerTest.java` (enhance)

### Files NOT Modified (Part 2)
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureEvaluationResultHandler.java` (Part 2 integration point)

---

## Success Criteria (Part 1)

### Functional Requirements
- ✅ All existing tests pass (no regressions)
- ✅ COHORT case explicitly handled (returns null)
- ✅ COMPOSITE case explicitly handled (returns null)
- ✅ Gap 2 fixed: Qualified/unqualified ID matching works correctly
- ✅ copyScoresFromDef() methods do nothing in Part 1 (null scores)
- ✅ Old scorers still produce scores (unchanged behavior)
- ✅ MeasureReportScoringFhirAdapter works for cdr-cr post-hoc scoring use case
- ✅ Gap 1 fixed: Text-based stratum matching works correctly
- ✅ Scores identical to baseline for regular measure evaluation

### Non-Functional Requirements
- ✅ Zero behavioral change for existing measure evaluation workflows
- ✅ Version-agnostic API ready for external consumers (cdr-cr)
- ✅ Clear deprecation path documented
- ✅ Comprehensive JavaDoc on new classes/methods
- ✅ Code formatted with spotless
- ✅ Performance maintained or improved

### Testing Requirements
- ✅ All existing tests pass
- ✅ New unit tests for COHORT/COMPOSITE (2 tests)
- ✅ Gap prevention tests (3 critical tests)
- ✅ R4 builder score copying tests (7+ tests)
- ✅ DSTU3 builder score copying tests (7+ tests)
- ✅ MeasureReportScoringFhirAdapter tests (19+ tests)
- ✅ Total new test coverage: 38+ tests

### Code Quality
- ✅ JavaDoc on all new classes/methods
- ✅ Consistent naming conventions
- ✅ Proper null handling
- ✅ Code formatted with spotless
- ✅ Critical comments explaining Gap fixes

---

## Testing Strategy

### Test Phases
1. **Unit Tests** - All new components tested in isolation
2. **Integration Tests** - Run full existing test suite
3. **Regression Tests** - Compare scores before/after
4. **Performance Tests** - Verify observation caching optimization
5. **Gap Prevention Tests** - Verify fixes for Gaps 1, 2, 3

### Expected Results
- All existing tests pass unchanged
- New unit tests pass
- Gap prevention tests pass (critical for Part 2)
- Scores match baseline exactly (old scorers still active)
- No performance regression
- MeasureReportScoringFhirAdapter ready for cdr-cr

---

## Risk Mitigation

### Risk 1: Accidental Behavior Change
**Likelihood:** Very Low
**Impact:** High
**Mitigation:**
- copyScoresFromDef() has null checks and does nothing with null scores
- Old scorers remain fully active and produce all scores
- Full regression test suite run before merge
- No changes to MeasureEvaluationResultHandler (scoring entry point)

### Risk 2: Gap Fixes Introduce New Issues
**Likelihood:** Low
**Impact:** Medium
**Mitigation:**
- All gap fixes verified in cr1 codebase (tested and working)
- Comprehensive tests added for each gap fix
- Tests serve as regression prevention for Part 2
- Old scorers still active, so gaps remain hidden until Part 2

### Risk 3: Test Complexity for MeasureReportScoringFhirAdapter
**Likelihood:** Medium
**Impact:** Medium
**Mitigation:**
- Use existing test fixtures from MeasureDefScorerTest
- Follow patterns from existing R4/DSTU3 scorer tests
- Start with simple proportion tests, then add complexity
- Mock/stub can be used for complex scenarios

### Risk 4: API Design for cdr-cr
**Likelihood:** Low
**Impact:** Medium
**Mitigation:**
- API already designed in original plan (reviewed by team)
- Static utility method is simple and matches cdr-cr usage patterns
- Version auto-detection makes it easy to use correctly

---

## Part 2 Preview

**Part 2 Scope (Future PRP):**
Part 2 will complete the integration by:

1. **Activate MeasureDefScorer in Workflow**
   - Add MeasureDefScorer call in MeasureEvaluationResultHandler.processResults()
   - Populates Def scores during regular measure evaluation
   - Guard with applyScoring parameter

2. **Remove Old Scorer Calls**
   - Remove measureReportScorer.score() calls from R4/DSTU3 builders
   - copyScoresFromDef() becomes the sole scoring path
   - Old scorer fields can be optionally removed

3. **Enhanced Def Integration Testing**
   - Refactor/enhance test framework to capture Def objects during evaluation
   - Add score assertions to existing tests
   - Create comprehensive scoring validation tests for all measure types
   - Test improvement notation, stratifiers, edge cases

4. **Iterative Refinement**
   - Fix any gaps in MeasureDefScorer discovered during Part 2 testing
   - Compare scores to Part 1 baseline to ensure no regression
   - Add unit tests for any fixes

5. **Future Cleanup**
   - After cdr-cr migrates to MeasureReportScoringFhirAdapter
   - Remove deprecated scorer class files (future PR)

**Part 2 Dependencies:**
Part 2 CANNOT start until Part 1 provides:
- ✅ copyScoresFromDef() in R4/DSTU3 builders
- ✅ MeasureDefScorer with COHORT/COMPOSITE support
- ✅ MeasureReportScoringFhirAdapter for cdr-cr
- ✅ Gap fixes (text-based matching, qualified/unqualified IDs)
- ✅ Deprecated old scorers
- ✅ All Part 1 tests passing

---

## Conclusion

Part 1 establishes a safe, tested foundation for version-agnostic measure scoring:
- **Zero risk** - No behavior change for existing workflows
- **Immediate value** - MeasureReportScoringFhirAdapter ready for cdr-cr
- **Critical bug fixes** - Prevents 31 test failures in Part 2
- **Performance improved** - MeasureObservationStratumCache optimization (from PR #852)
- **Well tested** - 38+ new unit tests including gap prevention tests
- **Clear path forward** - Part 2 ready to activate new scoring

**MeasureReportScoringFhirAdapter is the star deliverable** - it provides immediate value to cdr-cr project while the rest of the code prepares for Part 2 integration.

**Critical Gap Fixes are essential** - fixing text-based matching and qualified/unqualified ID handling in Part 1 will prevent 31 test failures in Part 2 and ensure smooth Part 2 execution.

**Ready for Part 2:** integrate-measure-def-scorer-part2-integration PRP (to be created)
