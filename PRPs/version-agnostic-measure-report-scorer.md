# PRP: Create Version-Agnostic MeasureDefScorer Using Def-First Iteration

## Overview
Create a new FHIR-version-agnostic `MeasureDefScorer` class that uses Def classes (MeasureDef, GroupDef, StratifierDef, StratumDef) for iteration order rather than iterating over FHIR MeasureReport structures. The scorer will **mutate Def classes** by setting computed scores directly on them (via getters/setters), making Def classes the single source of truth for data, iteration order, AND scores.

**Key Design**: The new MeasureDefScorer methods are `void` and mutate GroupDef/StratumDef objects by calling setScore() on them.

## Background

### Current Architecture

The current scoring architecture uses FHIR MeasureReport structures to determine what to score:

```java
// R4MeasureReportScorer.score() - line 97-116
public void score(String measureUrl, MeasureDef measureDef, MeasureReport measureReport) {
    // Iterates over FHIR structure
    for (MeasureReportGroupComponent mrgc : measureReport.getGroup()) {
        // Then looks up corresponding GroupDef
        GroupDef groupDef = getGroupDef(measureDef, mrgc);

        // Iterates over FHIR stratifiers
        for (MeasureReportGroupStratifierComponent stratifierComponent : mrgc.getStratifier()) {
            scoreStratifier(measureUrl, groupDef, measureScoring, stratifierComponent);
        }
    }
}
```

**Problems with current approach:**
1. **FHIR-First Iteration**: Iterates over `measureReport.getGroup()` to determine what to score
2. **Backwards Lookup**: After getting FHIR component, looks up corresponding Def object
3. **Version-Specific**: Each FHIR version (R4, DSTU3) has different MeasureReport types
4. **Coupling**: Scoring logic is tightly coupled to FHIR structure traversal

### Recent Architectural Progress

Recent refactorings have progressively moved toward Def-first patterns:

1. **PR #843**: Introduced `QuantityDef` for version-agnostic continuous variable scoring
2. **Recent Work**: Added `PopulationDef.getCount(GroupDef)` and `StratumDef.getPopulationCount(PopulationDef)` to centralize count retrieval
3. **Pattern**: Def classes are becoming the single source of truth for DATA

This PRP extends this pattern to make Def classes the single source of truth for ITERATION ORDER as well.

### Key Files Analyzed

**Scorer Files:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureReportScorer.java` - Interface to be renamed
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/BaseMeasureReportScorer.java` - Base class with helpers
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java` - R4 implementation
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureReportScorer.java` - DSTU3 implementation

**Def Class Files:**
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDef.java` - Has `groups()` method
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/GroupDef.java` - Has `populations()`, `stratifiers()`, `getPopulationCount()`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratifierDef.java` - Has `getStratum()` method
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratumDef.java` - Has `getPopulationCount()`, `stratumPopulations()`
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/PopulationDef.java` - Has `getCount(GroupDef)` method
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratumPopulationDef.java` - Has `getCount()` method

## Requirements

From `initial.md` and follow-up discussions:

1. ✅ Rename `MeasureReportScorer` interface to `IMeasureReportScorer`
2. ✅ Examine all FHIR-version agnostic code in R4/DSTU3/Base/IMeasureReportScorer
3. ✅ Derive iteration order from Def classes (e.g., `GroupDef.groups()`, `StratifierDef.getStratum()`)
4. ✅ Add this functionality to a new `MeasureDefScorer` class
5. ✅ Add robust unit testing to the new `MeasureDefScorer`
6. ✅ Add any non-FHIR code needed to GroupDef, StratifierDef, etc. to support this
7. ✅ Make Def classes mutable if needed to support new functionality (add score fields)
8. ✅ Do NOT update any existing measure scorer classes (R4/DSTU3 stay as-is)
9. ✅ **NEW**: Copy `R4MeasureReportScorer#calculateContinuousVariableAggregateQuantity` to new MeasureDefScorer
21. ✅ **NEW**: Rename class to `MeasureDefScorer` (not MeasureReportScorer - operates on MeasureDef)
22. ✅ **NEW**: Copy top-level javadoc from R4MeasureReportScorer explaining scoring formulas and examples
23. ✅ **NEW**: Rename main public method to `score()` instead of `scoreAllGroups()`
24. ✅ **NEW**: Unit tests use constructors instead of mocks for all Def classes
10. ✅ **NEW**: Create simplified `scoreContinuousVariable` that returns QuantityDef (no FHIR report mutation)
11. ✅ **NEW**: Add modified copy of `R4MeasureReportScorer#getStratumScoreOrNull` without StratifierGroupComponent parameter
12. ✅ **NEW**: Add modified copy of `R4MeasureReportScorer#scoreStratum` that acts on StratumDef instead of StratifierGroupComponent
13. ✅ **NEW**: Add modified copy of `R4MeasureReportScorer#scoreStratifier` that acts on StratifierDef instead of MeasureReportGroupStratifierComponent
14. ✅ **NEW**: Update `scoreGroup()` to forEach on `groupDef.stratifiers()` and call stratifier scoring
15. ✅ **NEW**: Add unit tests for continuous variable scoring (SUM, AVG, stratifiers)
16. ✅ **NEW**: CONTINUOUSVARIABLE case acts only on MEASUREOBSERVATION PopulationDef (not stratum populations directly)
17. ✅ **NEW**: Copy `R4MeasureReportScorer#scoreRatioContVariableStratum` for RATIO + MEASUREOBSERVATION stratifier scoring
18. ✅ **NEW**: Copy helper methods from BaseMeasureReportScorer: `getMeasureObservations`, `findPopulationDef`, `getStratumPopDefFromPopDef`
19. ✅ **NEW**: Include full if-else block in `getStratumScoreOrNull` for RATIO continuous variable vs standard PROPORTION/RATIO
20. ✅ **NEW**: Add test for RATIO continuous variable stratifier (separate numerator/denominator aggregates)

## Implementation Plan

### Phase 1: Rename Interface

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureReportScorer.java`

**Action**: Rename interface `MeasureReportScorer` → `IMeasureReportScorer`

```java
// BEFORE:
public interface MeasureReportScorer<MeasureReportT> {
    void score(String measureUrl, MeasureDef measureDef, MeasureReportT measureReport);
}

// AFTER:
public interface IMeasureReportScorer<MeasureReportT> {
    void score(String measureUrl, MeasureDef measureDef, MeasureReportT measureReport);
}
```

**File**: Rename file to `IMeasureReportScorer.java`

**Update all references:**
- `BaseMeasureReportScorer.java:12` - `implements MeasureReportScorer` → `implements IMeasureReportScorer`
- `R4MeasureReportScorer.java` - extends BaseMeasureReportScorer (no direct change needed)
- `Dstu3MeasureReportScorer.java` - extends BaseMeasureReportScorer (no direct change needed)
- Any test files or other code referencing the interface

### Phase 2: Add Score State to Def Classes

**Goal**: Make GroupDef and StratumDef mutable by adding score fields with getters/setters.

#### Phase 2.1: Add Score to GroupDef

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/GroupDef.java`

**Current State**: GroupDef is a regular class with all final fields (immutable data)

**Changes**:
```java
public class GroupDef {
    // Existing final fields
    private final String id;
    private final ConceptDef code;
    private final List<StratifierDef> stratifiers;
    private final List<PopulationDef> populations;
    // ... other final fields

    // NEW: Add mutable score field
    private Double score;

    // Existing constructor (no changes)
    public GroupDef(...) {
        // ... existing initialization
    }

    // NEW: Score getter
    public Double getScore() {
        return this.score;
    }

    // NEW: Score setter
    public void setScore(Double score) {
        this.score = score;
    }

    // ... existing methods unchanged
}
```

**Rationale**: GroupDef becomes partially mutable. Only the score field is mutable; all other fields remain final.

#### Phase 2.2: Add Score to StratumDef (Convert Record to Class)

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratumDef.java`

**Current State**: StratumDef is a Java record (immutable by design)

**Problem**: Records cannot have mutable fields

**Solution**: Convert StratumDef from record to regular class

**Changes**:
```java
// BEFORE (record):
public record StratumDef(
        List<StratumPopulationDef> stratumPopulations,
        Set<StratumValueDef> valueDefs,
        Collection<String> subjectIds) {

    public StratumDef(
            List<StratumPopulationDef> stratumPopulations,
            Set<StratumValueDef> valueDefs,
            Collection<String> subjectIds) {
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.valueDefs = valueDefs;
        this.subjectIds = subjectIds;
    }

    // ... existing methods
}

// AFTER (class):
public class StratumDef {
    private final List<StratumPopulationDef> stratumPopulations;
    private final Set<StratumValueDef> valueDefs;
    private final Collection<String> subjectIds;

    // NEW: Mutable score field
    private Double score;

    public StratumDef(
            List<StratumPopulationDef> stratumPopulations,
            Set<StratumValueDef> valueDefs,
            Collection<String> subjectIds) {
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.valueDefs = valueDefs;
        this.subjectIds = subjectIds;
    }

    // Existing accessor methods (keep method-style names for compatibility)
    public List<StratumPopulationDef> stratumPopulations() {
        return stratumPopulations;
    }

    public Set<StratumValueDef> valueDefs() {
        return valueDefs;
    }

    public Collection<String> subjectIds() {
        return subjectIds;
    }

    // NEW: Score getter
    public Double getScore() {
        return this.score;
    }

    // NEW: Score setter
    public void setScore(Double score) {
        this.score = score;
    }

    // Keep all existing methods: isComponent(), getStratumPopulation(), getPopulationCount()
}
```

**Important Notes**:
- Keep method-style accessors (`stratumPopulations()` not `getStratumPopulations()`) for backward compatibility
- Constructor signature remains identical
- This is a mostly-compatible breaking change

**Impact Analysis Required**:
- Search for all `new StratumDef(...)` instantiations
- Check for any record-specific features (pattern matching, deconstruction)
- Run full test suite to catch any issues

### Phase 3: Extract Version-Agnostic Scoring Logic

**Goal**: Identify all scoring logic in R4MeasureReportScorer that is version-agnostic and can be moved to the new MeasureReportScorer.

**Version-Agnostic Logic (can be extracted):**
- `calcProportionScore(Integer numeratorCount, Integer denominatorCount)` - already in BaseMeasureReportScorer
- Proportion/Ratio scoring formulas: `(n - nx) / (d - dx - de)`
- Population count retrieval: now uses `PopulationDef.getCount(GroupDef)`
- Stratum count retrieval: uses `StratumDef.getPopulationCount(PopulationDef)`

**Version-Specific Logic (stays in R4/DSTU3):**
- Continuous variable scoring with converters (`R4ContinuousVariableObservationConverter`)
- Setting scores on FHIR MeasureReport components (`.setMeasureScore()`)
- FHIR Quantity creation

### Phase 4: Design New MeasureDefScorer Class

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorer.java`

**Design Philosophy**: All methods are `void` and mutate Def objects by setting scores on them.

**Design**:
```java
package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluation of Measure Report Data showing raw CQL criteria results compared to resulting Measure Report.
 *
 * <p>Each row represents a subject as raw cql criteria expression output:
 *
 * <pre>{@code
 * Subject | IP | D  | DX | N  | DE | NX | Notes
 * --------|----|----|----|----|----|----|---------------------------------------------------------------
 * A       | A  | A  | A  |    |    |    |
 * B       | B  | B  |    | B  |    |    |
 * C       | C  | C  |    |    | C  |    | InDenominator = true, InDenominatorException = true,
 *                                       | InNumerator = false
 * D       | D  | D  |    | D  |    | D  |
 * E       | E  | E  |    | E  |    |    |
 * F       |    |    |    | F  |    |    | Not in Initial Population or Denominator
 * G       | G  | G  |    | G  | G  |    | InDenominatorException = true & InNumerator = true
 * }</pre>
 *
 * <p>Each row represents a subject and their inclusion/exclusion population criteria on a Measure Report:
 *
 * <pre>{@code
 * Subject | IP | D  | DX | N  | DE | NX | Notes
 * --------|----|----|----|----|----|----|---------------------------------------------------------------
 * A       | A  | A  | A  |    |    |    |
 * B       | B  | B  |    | B  |    |    |
 * C       | C  | C  |    |    | C  |    | InDenominator = true, InDenominatorException = true,
 *                                       | InNumerator = false → Scores as InDenominatorException = true
 * D       | D  | D  |    | D  |    | D  |
 * E       | E  | E  |    | E  |    |    |
 * F       |    |    |    |    |    |    | Excluded: Not in Initial Population or Denominator
 * G       | G  | G  |    | G  |    |    | InDenominatorException = true & InNumerator = true → Remove from DE
 * }</pre>
 *
 * <p><strong>Population Counts:</strong>
 * <ul>
 *   <li>Initial Population (ip): 6</li>
 *   <li>Denominator (d): 6</li>
 *   <li>Denominator Exclusion (dx): 1</li>
 *   <li>Numerator (n): 4</li>
 *   <li>Denominator Exception (de): 1</li>
 *   <li>Numerator Exclusion (nx): 1</li>
 * </ul>
 *
 * <p><strong>Performance Rate Formula:</strong><br>
 * {@code (n - nx) / (d - dx - de)}<br>
 * {@code (4 - 1) / (6 - 1 - 1)} = <b>0.75</b>
 *
 * <p><strong>Measure Score:</strong> {@code 0.75}<br>
 *
 * <p>This is a FHIR-version-agnostic scorer that uses Def classes for iteration order
 * and mutates them by setting computed scores. Unlike R4/DSTU3MeasureReportScorer which
 * iterate over FHIR MeasureReport structures, this scorer iterates using:
 * <ul>
 *   <li>MeasureDef.groups() for group iteration</li>
 *   <li>GroupDef.stratifiers() for stratifier iteration</li>
 *   <li>StratifierDef.getStratum() for stratum iteration</li>
 *   <li>PopulationDef.getCount(GroupDef) for population counts</li>
 *   <li>StratumDef.getPopulationCount(PopulationDef) for stratum counts</li>
 * </ul>
 *
 * <p>This class computes scores and SETS them on Def objects using setScore() methods.
 * All methods are void. This makes Def classes the complete data model for measure scoring.
 *
 * <p>Generated by Claude Sonnet 4.5 on 2025-12-03
 */
public class MeasureDefScorer {

    private static final Logger logger = LoggerFactory.getLogger(MeasureDefScorer.class);

    /**
     * Score all groups in a measure definition - MUTATES GroupDef objects.
     *
     * @param measureUrl the measure URL for error reporting
     * @param measureDef the measure definition containing groups to score
     */
    public void score(String measureUrl, MeasureDef measureDef) {
        // Def-first iteration: iterate over MeasureDef.groups()
        for (GroupDef groupDef : measureDef.groups()) {
            scoreGroup(measureUrl, groupDef);
        }
    }

    /**
     * Score a single group including all its stratifiers - MUTATES GroupDef and StratumDef objects.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition to score (will be mutated with setScore())
     */
    public void scoreGroup(String measureUrl, GroupDef groupDef) {
        MeasureScoring measureScoring = checkMissingScoringType(measureUrl, groupDef.measureScoring());

        // Calculate group-level score
        Double groupScore = calculateGroupScore(measureUrl, groupDef, measureScoring);

        // MUTATE: Set score on GroupDef
        groupDef.setScore(groupScore);

        // Score all stratifiers using Def-first iteration
        // Modified from R4MeasureReportScorer to iterate over Def classes instead of FHIR components
        for (StratifierDef stratifierDef : groupDef.stratifiers()) {
            scoreStratifier(measureUrl, groupDef, stratifierDef, measureScoring);
        }
    }

    /**
     * Calculate score for a group based on its scoring type.
     */
    private Double calculateGroupScore(String measureUrl, GroupDef groupDef, MeasureScoring measureScoring) {
        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                // Standard proportion/ratio scoring: (n - nx) / (d - dx - de)
                int numerator = groupDef.getPopulationCount(MeasurePopulationType.NUMERATOR);
                int numeratorExclusion = groupDef.getPopulationCount(MeasurePopulationType.NUMERATOREXCLUSION);
                int denominator = groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOR);
                int denominatorExclusion = groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCLUSION);
                int denominatorException = groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCEPTION);

                return calcProportionScore(
                    numerator - numeratorExclusion,
                    denominator - denominatorExclusion - denominatorException
                );

            case CONTINUOUSVARIABLE:
                // Continuous variable scoring - returns aggregate value
                PopulationDef measureObsPop = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);
                QuantityDef quantityDef = scoreContinuousVariable(measureUrl, groupDef, measureObsPop);
                return quantityDef != null ? quantityDef.value() : null;

            default:
                return null;
        }
    }

    /**
     * Score continuous variable measure - returns QuantityDef with aggregated value.
     * Simplified version of R4MeasureReportScorer#scoreContinuousVariable that just
     * returns the aggregate without setting it on a FHIR report.
     */
    private QuantityDef scoreContinuousVariable(
            String measureUrl, GroupDef groupDef, PopulationDef populationDef) {
        return calculateContinuousVariableAggregateQuantity(
                measureUrl, populationDef, PopulationDef::getAllSubjectResources);
    }

    /**
     * Score all strata in a stratifier using Def-first iteration - MUTATES StratumDef objects.
     * Modified copy of R4MeasureReportScorer#scoreStratifier without FHIR components.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition containing the stratifier
     * @param stratifierDef the stratifier definition
     * @param measureScoring the scoring type
     */
    private void scoreStratifier(
            String measureUrl,
            GroupDef groupDef,
            StratifierDef stratifierDef,
            MeasureScoring measureScoring) {

        // Def-first iteration: iterate over StratifierDef.getStratum()
        for (StratumDef stratumDef : stratifierDef.getStratum()) {
            scoreStratum(measureUrl, groupDef, stratumDef, measureScoring);
        }
    }

    /**
     * Score a single stratum - MUTATES StratumDef object.
     * Modified copy of R4MeasureReportScorer#scoreStratum without StratifierGroupComponent.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param stratumDef the stratum definition to score (will be mutated)
     * @param measureScoring the scoring type
     */
    private void scoreStratum(
            String measureUrl,
            GroupDef groupDef,
            StratumDef stratumDef,
            MeasureScoring measureScoring) {

        Double score = getStratumScoreOrNull(measureUrl, groupDef, stratumDef, measureScoring);

        // MUTATE: Set score on StratumDef
        stratumDef.setScore(score);
    }

    /**
     * Calculate stratum score based on scoring type.
     * Modified copy of R4MeasureReportScorer#getStratumScoreOrNull without StratifierGroupComponent.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     * @param measureScoring the scoring type
     * @return the calculated score or null
     */
    @Nullable
    private Double getStratumScoreOrNull(
            String measureUrl,
            GroupDef groupDef,
            StratumDef stratumDef,
            MeasureScoring measureScoring) {

        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                // Check for special RATIO continuous variable case
                if (measureScoring.equals(MeasureScoring.RATIO)
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    return scoreRatioMeasureObservationStratum(measureUrl, groupDef, stratumDef);
                } else {
                    return scoreProportionRatioStratum(groupDef, stratumDef);
                }

            case CONTINUOUSVARIABLE:
                return scoreContinuousVariableStratum(measureUrl, groupDef, stratumDef);

            default:
                return null;
        }
    }

    /**
     * Score a stratum for RATIO measures with MEASUREOBSERVATION populations.
     * Handles continuous variable ratio scoring where numerator and denominator have separate observations.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     * @return the calculated score or null
     */
    @Nullable
    private Double scoreRatioMeasureObservationStratum(
            String measureUrl,
            GroupDef groupDef,
            StratumDef stratumDef) {

        if (stratumDef == null) {
            return null;
        }

        // Get all MEASUREOBSERVATION populations
        var populationDefs = getMeasureObservations(groupDef);

        // Find Measure Observations for Numerator and Denominator
        PopulationDef numPopDef = findPopulationDef(groupDef, populationDefs, MeasurePopulationType.NUMERATOR);
        PopulationDef denPopDef = findPopulationDef(groupDef, populationDefs, MeasurePopulationType.DENOMINATOR);

        // Get stratum populations for numerator and denominator
        StratumPopulationDef stratumPopulationDefNum = getStratumPopDefFromPopDef(stratumDef, numPopDef);
        StratumPopulationDef stratumPopulationDefDen = getStratumPopDefFromPopDef(stratumDef, denPopDef);

        return scoreRatioContVariableStratum(
                measureUrl,
                groupDef,
                stratumPopulationDefNum,
                stratumPopulationDefDen,
                numPopDef,
                denPopDef);
    }

    /**
     * Score a stratum for standard PROPORTION or RATIO measures (non-continuous variable).
     * Uses simple numerator/denominator count ratio.
     *
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     * @return the calculated score or null
     */
    @Nullable
    private Double scoreProportionRatioStratum(GroupDef groupDef, StratumDef stratumDef) {
        int numeratorCount = stratumDef.getPopulationCount(
                groupDef.getSingle(MeasurePopulationType.NUMERATOR));
        int denominatorCount = stratumDef.getPopulationCount(
                groupDef.getSingle(MeasurePopulationType.DENOMINATOR));

        return calcProportionScore(numeratorCount, denominatorCount);
    }

    /**
     * Score a stratum for CONTINUOUSVARIABLE measures.
     * Aggregates MEASUREOBSERVATION population observations filtered by stratum subjects.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     * @return the calculated score or null
     */
    @Nullable
    private Double scoreContinuousVariableStratum(
            String measureUrl,
            GroupDef groupDef,
            StratumDef stratumDef) {

        // Get the MEASUREOBSERVATION population from GroupDef
        PopulationDef measureObsPop = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);
        if (measureObsPop == null) {
            return null;
        }

        // Find the stratum population corresponding to MEASUREOBSERVATION
        StratumPopulationDef stratumPopulationDef = stratumDef.stratumPopulations().stream()
                .filter(stratumPopDef ->
                        stratumPopDef.id().startsWith(MeasurePopulationType.MEASUREOBSERVATION.toCode()))
                .findFirst()
                .orElse(null);

        if (stratumPopulationDef == null) {
            return null;
        }

        // Calculate aggregate using stratum-filtered resources
        QuantityDef quantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl,
                measureObsPop,
                populationDef -> getResultsForStratum(populationDef, stratumPopulationDef));

        return quantityDef != null ? quantityDef.value() : null;
    }

    /**
     * Score ratio continuous variable for a stratum.
     * Copied from R4MeasureReportScorer#scoreRatioContVariableStratum.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param measureObsNumStratum stratum population for numerator measure observation
     * @param measureObsDenStratum stratum population for denominator measure observation
     * @param numPopDef numerator population definition
     * @param denPopDef denominator population definition
     * @return the ratio score or null
     */
    private Double scoreRatioContVariableStratum(
            String measureUrl,
            GroupDef groupDef,
            StratumPopulationDef measureObsNumStratum,
            StratumPopulationDef measureObsDenStratum,
            PopulationDef numPopDef,
            PopulationDef denPopDef) {

        // Calculate aggregate for numerator observations filtered by stratum
        QuantityDef aggregateNumQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, numPopDef, populationDef -> getResultsForStratum(populationDef, measureObsNumStratum));

        // Calculate aggregate for denominator observations filtered by stratum
        QuantityDef aggregateDenQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, denPopDef, populationDef -> getResultsForStratum(populationDef, measureObsDenStratum));

        if (aggregateNumQuantityDef == null || aggregateDenQuantityDef == null) {
            return null;
        }

        Double num = aggregateNumQuantityDef.value();
        Double den = aggregateDenQuantityDef.value();

        if (den == null || den == 0.0) {
            return null;
        }

        if (num == null || num == 0.0) {
            // Explicitly handle numerator zero with positive denominator
            return den > 0.0 ? 0.0 : null;
        }

        return num / den;
    }

    /**
     * Get all MEASUREOBSERVATION populations from a group.
     * Copied from BaseMeasureReportScorer - version-agnostic helper.
     *
     * @param groupDef the group definition
     * @return list of MEASUREOBSERVATION PopulationDef
     */
    private List<PopulationDef> getMeasureObservations(GroupDef groupDef) {
        return groupDef.populations().stream()
                .filter(t -> t.type().equals(MeasurePopulationType.MEASUREOBSERVATION))
                .toList();
    }

    /**
     * Find PopulationDef by matching criteria reference.
     * Copied from BaseMeasureReportScorer - version-agnostic helper.
     *
     * @param groupDef the group definition
     * @param populationDefs list of MEASUREOBSERVATION populations to search
     * @param type the population type to find
     * @return matching PopulationDef or null
     */
    @Nullable
    private PopulationDef findPopulationDef(
            GroupDef groupDef, List<PopulationDef> populationDefs, MeasurePopulationType type) {
        var groupPops = groupDef.get(type);
        if (groupPops == null || groupPops.isEmpty() || groupPops.get(0).id() == null) {
            return null;
        }

        String criteriaId = groupPops.get(0).id();

        return populationDefs.stream()
                .filter(p -> criteriaId.equals(p.getCriteriaReference()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Extract StratumPopulationDef from StratumDef that matches a PopulationDef.
     * Copied from BaseMeasureReportScorer - version-agnostic helper.
     *
     * @param stratumDef the stratum definition
     * @param populationDef the population definition to match
     * @return matching StratumPopulationDef or null
     */
    @Nullable
    private StratumPopulationDef getStratumPopDefFromPopDef(StratumDef stratumDef, PopulationDef populationDef) {
        if (populationDef == null) {
            return null;
        }
        return stratumDef.stratumPopulations().stream()
                .filter(t -> t.id().equals(populationDef.id()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get results filtered for a specific stratum.
     * Copied from BaseMeasureReportScorer - extracts resources belonging to stratum subjects.
     *
     * @param populationDef the population definition (MEASUREOBSERVATION)
     * @param stratumPopulationDef the stratum population to filter by
     * @return collection of resources belonging to this stratum
     */
    private static Collection<Object> getResultsForStratum(
            PopulationDef populationDef,
            StratumPopulationDef stratumPopulationDef) {

        if (stratumPopulationDef == null) {
            return List.of();
        }

        // Get all subject resources from the population
        Collection<Object> allResources = populationDef.getAllSubjectResources();

        // Filter to only resources that belong to subjects in this stratum
        Set<String> stratumSubjects = stratumPopulationDef.subjectsQualifiedOrUnqualified();

        return allResources.stream()
            .filter(resource -> {
                if (resource instanceof Map<?, ?> map) {
                    // Check if any key in the map belongs to a stratum subject
                    return map.keySet().stream()
                        .anyMatch(key -> stratumSubjects.contains(String.valueOf(key)));
                }
                return false;
            })
            .collect(Collectors.toList());
    }

    /**
     * Calculate continuous variable aggregate quantity.
     * Copied from R4MeasureReportScorer with minor adaptations.
     *
     * @param measureUrl the measure URL for error reporting
     * @param populationDef the population definition containing observation data
     * @param popDefToResources function to extract resources from population def
     * @return aggregated QuantityDef or null if population is null
     */
    @Nullable
    private static QuantityDef calculateContinuousVariableAggregateQuantity(
            String measureUrl,
            PopulationDef populationDef,
            Function<PopulationDef, Collection<Object>> popDefToResources) {

        if (populationDef == null) {
            logger.warn("Measure population group has no measure population defined for measure: {}", measureUrl);
            return null;
        }

        return calculateContinuousVariableAggregateQuantity(
                populationDef.getAggregateMethod(), popDefToResources.apply(populationDef));
    }

    /**
     * Calculate continuous variable aggregate quantity.
     * Copied from R4MeasureReportScorer.
     *
     * @param aggregateMethod the aggregation method (SUM, AVG, MIN, MAX, MEDIAN, COUNT)
     * @param qualifyingResources the resources containing QuantityDef observations
     * @return aggregated QuantityDef or null if no resources
     */
    @Nullable
    private static QuantityDef calculateContinuousVariableAggregateQuantity(
            ContinuousVariableObservationAggregateMethod aggregateMethod,
            Collection<Object> qualifyingResources) {
        var observationQuantity = collectQuantities(qualifyingResources);
        return aggregate(observationQuantity, aggregateMethod);
    }

    /**
     * Aggregate a list of QuantityDefs using the specified method.
     * Copied from R4MeasureReportScorer.
     *
     * @param quantities list of QuantityDef to aggregate
     * @param method aggregation method
     * @return aggregated QuantityDef with computed value
     */
    private static QuantityDef aggregate(
            List<QuantityDef> quantities, ContinuousVariableObservationAggregateMethod method) {
        if (quantities == null || quantities.isEmpty()) {
            return null;
        }

        if (ContinuousVariableObservationAggregateMethod.N_A == method) {
            throw new InvalidRequestException(
                    "Aggregate method must be provided for continuous variable scoring, but is NO-OP.");
        }

        double result;

        // Optimize by reusing stream creation for SUM, MAX, MIN, AVG
        if (method == ContinuousVariableObservationAggregateMethod.COUNT) {
            result = quantities.size();
        } else if (method == ContinuousVariableObservationAggregateMethod.MEDIAN) {
            List<Double> sorted = quantities.stream()
                    .map(QuantityDef::value)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            int n = sorted.size();
            if (n % 2 == 1) {
                result = sorted.get(n / 2);
            } else {
                result = (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
            }
        } else {
            // Reusable stream for SUM, MAX, MIN, AVG
            var doubleStream = quantities.stream()
                    .map(QuantityDef::value)
                    .filter(Objects::nonNull)
                    .mapToDouble(value -> value);

            switch (method) {
                case SUM:
                    result = doubleStream.sum();
                    break;
                case MAX:
                    result = doubleStream.max().orElse(Double.NaN);
                    break;
                case MIN:
                    result = doubleStream.min().orElse(Double.NaN);
                    break;
                case AVG:
                    result = doubleStream.average().orElse(Double.NaN);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported aggregation method: " + method);
            }
        }

        return new QuantityDef(result);
    }

    /**
     * Collect QuantityDef objects from nested Map structures in resources.
     * Copied from R4MeasureReportScorer.
     *
     * @param resources collection of objects that may contain Maps with QuantityDef values
     * @return list of QuantityDef objects found
     */
    private static List<QuantityDef> collectQuantities(Collection<Object> resources) {
        var mapValues = resources.stream()
                .filter(x -> x instanceof Map<?, ?>)
                .map(x -> (Map<?, ?>) x)
                .map(Map::values)
                .flatMap(Collection::stream)
                .toList();

        return mapValues.stream()
                .filter(QuantityDef.class::isInstance)
                .map(QuantityDef.class::cast)
                .toList();
    }

    /**
     * Calculate proportion/ratio score: numerator / denominator.
     * Reused from BaseMeasureReportScorer pattern.
     */
    protected Double calcProportionScore(Integer numeratorCount, Integer denominatorCount) {
        if (numeratorCount == null) {
            numeratorCount = 0;
        }
        if (denominatorCount != null && denominatorCount != 0) {
            return numeratorCount / (double) denominatorCount;
        }
        return null;
    }

    /**
     * Validate scoring type is present.
     * Reused from BaseMeasureReportScorer pattern.
     */
    protected MeasureScoring checkMissingScoringType(String measureUrl, MeasureScoring measureScoring) {
        if (measureScoring == null) {
            throw new InvalidRequestException(
                "Measure does not have a scoring methodology defined. Add a \"scoring\" property to the measure definition or the group definition for measure: "
                        + measureUrl);
        }
        return measureScoring;
    }
}
```

**Key Design Decisions:**

1. **Utility Class, Not Interface Implementation**: This class does NOT implement `IMeasureReportScorer`. It's a new pattern, not a replacement for existing scorers.

2. **Def-First Iteration**: Uses `MeasureDef.groups()`, `GroupDef.stratifiers()`, `StratifierDef.getStratum()` for iteration order.

3. **Mutation-Based Design**: All methods are `void`. Computed scores are set directly on Def objects via `groupDef.setScore()` and `stratumDef.setScore()`. Def classes become the single source of truth for scores.

4. **Version-Agnostic**: No FHIR-specific types (Quantity, MeasureReport, etc.). Only works with Def classes and Java primitives (including QuantityDef).

5. **Continuous Variable Support**: Includes full support for continuous variable scoring by copying logic from R4MeasureReportScorer:
   - `calculateContinuousVariableAggregateQuantity()` - aggregates QuantityDef observations
   - `aggregate()` - performs SUM, AVG, MIN, MAX, MEDIAN, COUNT operations
   - `collectQuantities()` - extracts QuantityDef from nested Map structures
   - `scoreContinuousVariable()` - simplified version that returns QuantityDef

6. **Stratifier Scoring Pattern**: Modified copies of R4MeasureReportScorer methods without FHIR components:
   - `scoreStratifier()` - iterates over StratifierDef.getStratum() (not FHIR components)
   - `scoreStratum()` - scores a single StratumDef by calling getStratumScoreOrNull()
   - `getStratumScoreOrNull()` - calculates score for PROPORTION/RATIO/CONTINUOUSVARIABLE
   - `getResultsForStratum()` - filters PopulationDef resources by stratum subjects
   - Pattern: Get MEASUREOBSERVATION from GroupDef → Filter by stratum subjects → Aggregate

### Phase 5: Verify Def Class Capabilities

**GroupDef** - No additional methods needed:
- ✅ `groups()` - returns List<GroupDef> (for MeasureDef.groups())
- ✅ `stratifiers()` - returns List<StratifierDef>
- ✅ `getPopulationCount(MeasurePopulationType)` - gets count for population type
- ✅ `getSingle(MeasurePopulationType)` - gets single PopulationDef by type (for continuous variable)
- ✅ `setScore(Double)` / `getScore()` - **NEW in Phase 2**

**StratifierDef** - No additional methods needed:
- ✅ `getStratum()` - returns List<StratumDef>
- ✅ `id()` - returns stratifier ID

**StratumDef** - No additional methods needed:
- ✅ `getPopulationCount(PopulationDef)` - gets count for population
- ✅ `getStratumPopulation(PopulationDef)` - gets StratumPopulationDef for a population
- ✅ `setScore(Double)` / `getScore()` - **NEW in Phase 2**

**StratumPopulationDef** - May need new method for continuous variable:
- ❓ `getResources()` - **MAY NEED TO ADD**: Returns Collection<Object> of resources containing QuantityDef observations
- Alternative: If this method doesn't exist, may need to add it OR use a different accessor
- Note: Will need to verify during implementation what accessor is available for stratum observation resources

### Phase 6: Comprehensive Unit Testing

**New test file**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorerTest.java`

**Important**: Do NOT use mocks. Create all Def objects using their constructors with real state.

```java
package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for version-agnostic MeasureDefScorer.
 *
 * Tests the Def-first iteration pattern AND mutation-based design where
 * scores are set directly on Def objects using setScore() methods.
 *
 * All tests use constructors to create Def objects (NO MOCKS).
 *
 * Generated by Claude Sonnet 4.5 on 2025-12-03
 */
class MeasureDefScorerTest {

    @Test
    void testScoreGroup_SetsScoreOnGroupDef() {
        // Setup: Simple proportion measure with 3/4 subjects meeting criteria
        PopulationDef numeratorPop = createPopulationDef("num-1", MeasurePopulationType.NUMERATOR,
            Set.of("patient1", "patient2", "patient3"));
        PopulationDef denominatorPop = createPopulationDef("den-1", MeasurePopulationType.DENOMINATOR,
            Set.of("patient1", "patient2", "patient3", "patient4"));

        GroupDef groupDef = new GroupDef(
            "group-1",
            createTextOnlyConcept("Test Group"),
            List.of(),  // No stratifiers
            List.of(numeratorPop, denominatorPop),
            MeasureScoring.PROPORTION,
            false,
            createImprovementNotationCode("increase"),
            createPopulationBasisCode("boolean")
        );

        // Score is null before scoring
        assertNull(groupDef.getScore());

        MeasureDefScorer scorer = new MeasureDefScorer();
        scorer.scoreGroup("http://example.com/Measure/test", groupDef);

        // VERIFY: Score is set on GroupDef via mutation
        assertEquals(0.75, groupDef.getScore(), 0.001);
    }

    // ============================================================================
    // Helper Methods for Test Data Construction
    // ============================================================================

    /**
     * Create PopulationDef with subjects for boolean basis populations.
     */
    private PopulationDef createPopulationDef(String id, MeasurePopulationType type, Set<String> subjects) {
        ConceptDef code = createMeasurePopulationConcept(type);
        PopulationDef pop = new PopulationDef(id, code, type, "expression");

        // Add subjects to population
        for (String subject : subjects) {
            pop.addResource(subject, true);  // For boolean basis, resource is just boolean true
        }

        return pop;
    }

    /**
     * Create CodeDef for measure-population system.
     */
    private CodeDef createMeasurePopulationCode(String code) {
        return new CodeDef("http://terminology.hl7.org/CodeSystem/measure-population", code);
    }

    /**
     * Create CodeDef for measure-improvement-notation system.
     */
    private CodeDef createImprovementNotationCode(String notation) {
        return new CodeDef("http://terminology.hl7.org/CodeSystem/measure-improvement-notation", notation);
    }

    /**
     * Create CodeDef for measure population basis.
     */
    private CodeDef createPopulationBasisCode(String basis) {
        return new CodeDef("http://hl7.org/fhir/ValueSet/measure-population", basis);
    }

    /**
     * Create ConceptDef for measure population type.
     */
    private ConceptDef createMeasurePopulationConcept(MeasurePopulationType type) {
        return new ConceptDef(
            List.of(createMeasurePopulationCode(type.toCode())),
            type.toCode()
        );
    }

    /**
     * Create ConceptDef with text only (no codes).
     */
    private ConceptDef createTextOnlyConcept(String text) {
        return new ConceptDef(List.of(), text);
    }

    @Test
    void testScoreGroup_ProportionWithExclusions() {
        // Setup: Proportion measure with exclusions and exceptions
        // Formula: (n - nx) / (d - dx - de)
        // (10 - 2) / (20 - 3 - 1) = 8 / 16 = 0.5
        PopulationDef numeratorPop = createPopulationDef("num-1", MeasurePopulationType.NUMERATOR,
            Set.of("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10"));
        PopulationDef denominatorPop = createPopulationDef("den-1", MeasurePopulationType.DENOMINATOR,
            Set.of("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "p11", "p12", "p13", "p14", "p15", "p16", "p17", "p18", "p19", "p20"));
        PopulationDef denExclusionPop = createPopulationDef("dex-1", MeasurePopulationType.DENOMINATOREXCLUSION,
            Set.of("p11", "p12", "p13"));
        PopulationDef denExceptionPop = createPopulationDef("dexc-1", MeasurePopulationType.DENOMINATOREXCEPTION,
            Set.of("p14"));
        PopulationDef numExclusionPop = createPopulationDef("nex-1", MeasurePopulationType.NUMERATOREXCLUSION,
            Set.of("p1", "p2"));

        GroupDef groupDef = new GroupDef(
            "group-1",
            createTextOnlyConcept("Test Group"),
            List.of(),
            List.of(numeratorPop, denominatorPop, denExclusionPop, denExceptionPop, numExclusionPop),
            MeasureScoring.PROPORTION,
            false,
            createImprovementNotationCode("increase"),
            createPopulationBasisCode("boolean")
        );

        MeasureDefScorer scorer = new MeasureDefScorer();
        scorer.scoreGroup("http://example.com/Measure/test", groupDef);

        // VERIFY: Score is set correctly
        assertEquals(0.5, groupDef.getScore(), 0.001);
    }

    @Test
    void testScoreGroup_ZeroDenominator_SetsNullScore() {
        // Setup: All subjects excluded from denominator
        PopulationDef numeratorPop = createPopulationDef("num-1", MeasurePopulationType.NUMERATOR,
            Set.of("p1", "p2", "p3", "p4", "p5"));
        PopulationDef denominatorPop = createPopulationDef("den-1", MeasurePopulationType.DENOMINATOR,
            Set.of("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10"));
        PopulationDef denExclusionPop = createPopulationDef("dex-1", MeasurePopulationType.DENOMINATOREXCLUSION,
            Set.of("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10"));  // All excluded

        GroupDef groupDef = new GroupDef(
            "group-1",
            createTextOnlyConcept("Test Group"),
            List.of(),
            List.of(numeratorPop, denominatorPop, denExclusionPop),
            MeasureScoring.PROPORTION,
            false,
            createImprovementNotationCode("increase"),
            createPopulationBasisCode("boolean")
        );

        MeasureDefScorer scorer = new MeasureDefScorer();
        scorer.scoreGroup("http://example.com/Measure/test", groupDef);

        // VERIFY: Null score set for zero denominator
        assertNull(groupDef.getScore());
    }

    @Test
    void testScoreStratifier_SetsScoresOnStratumDefs() {
        // Setup: Group with stratifier containing two strata
        // Male stratum: 3/5 = 0.6
        // Female stratum: 5/5 = 1.0

        PopulationDef numeratorPop = createPopulationDef("num-1", MeasurePopulationType.NUMERATOR,
            Set.of("male1", "male2", "male3", "female1", "female2", "female3", "female4", "female5"));
        PopulationDef denominatorPop = createPopulationDef("den-1", MeasurePopulationType.DENOMINATOR,
            Set.of("male1", "male2", "male3", "male4", "male5", "female1", "female2", "female3", "female4", "female5"));

        // Create stratum populations for Male stratum
        CodeDef booleanBasisCode = createPopulationBasisCode("boolean");

        StratumPopulationDef maleNumPop = new StratumPopulationDef(
            "num-1",
            Set.of("male1", "male2", "male3"),
            Set.of(),
            List.of(),
            MeasureStratifierType.VALUE,
            booleanBasisCode
        );
        StratumPopulationDef maleDenPop = new StratumPopulationDef(
            "den-1",
            Set.of("male1", "male2", "male3", "male4", "male5"),
            Set.of(),
            List.of(),
            MeasureStratifierType.VALUE,
            booleanBasisCode
        );

        StratumDef maleStratum = new StratumDef(
            List.of(maleNumPop, maleDenPop),
            Set.of(new StratumValueDef("male", "gender")),
            Set.of("male1", "male2", "male3", "male4", "male5")
        );

        // Create stratum populations for Female stratum
        StratumPopulationDef femaleNumPop = new StratumPopulationDef(
            "num-1",
            Set.of("female1", "female2", "female3", "female4", "female5"),
            Set.of(),
            List.of(),
            MeasureStratifierType.VALUE,
            booleanBasisCode
        );
        StratumPopulationDef femaleDenPop = new StratumPopulationDef(
            "den-1",
            Set.of("female1", "female2", "female3", "female4", "female5"),
            Set.of(),
            List.of(),
            MeasureStratifierType.VALUE,
            booleanBasisCode
        );

        StratumDef femaleStratum = new StratumDef(
            List.of(femaleNumPop, femaleDenPop),
            Set.of(new StratumValueDef("female", "gender")),
            Set.of("female1", "female2", "female3", "female4", "female5")
        );

        StratifierDef stratifierDef = new StratifierDef(
            "gender-stratifier",
            createTextOnlyConcept("Gender Stratifier"),
            "Gender",
            MeasureStratifierType.VALUE
        );
        stratifierDef.addAllStratum(List.of(maleStratum, femaleStratum));

        GroupDef groupDef = new GroupDef(
            "group-1",
            createTextOnlyConcept("Test Group"),
            List.of(stratifierDef),
            List.of(numeratorPop, denominatorPop),
            MeasureScoring.PROPORTION,
            false,
            createImprovementNotationCode("increase"),
            booleanBasisCode
        );

        // Scores are null before scoring
        assertNull(maleStratum.getScore());
        assertNull(femaleStratum.getScore());

        // Execute
        MeasureDefScorer scorer = new MeasureDefScorer();
        scorer.scoreGroup("http://example.com/Measure/test", groupDef);

        // VERIFY: Scores are set on each StratumDef via mutation
        assertEquals(0.6, maleStratum.getScore(), 0.001);  // Male: 3/5
        assertEquals(1.0, femaleStratum.getScore(), 0.001);  // Female: 5/5
    }

    // ============================================================================
    // Additional Test Coverage (Constructor-Based Pattern Examples)
    // ============================================================================
    //
    // The following tests should follow the same constructor-based pattern:
    //
    // 1. testScore_MultipleGroups() - Create MeasureDef with multiple GroupDef objects
    // 2. testScoreGroup_RatioMeasure() - Test RATIO scoring (6/12 = 0.5)
    // 3. testScoreGroup_ContinuousVariable_SumAggregation() - Create PopulationDef with MEASUREOBSERVATION
    //    and Map<String, QuantityDef> observations, verify SUM aggregate
    // 4. testScoreGroup_ContinuousVariable_AvgAggregation() - Same as above but verify AVG
    // 5. testScoreStratifier_ContinuousVariable() - Create GroupDef with stratifier, MEASUREOBSERVATION population,
    //    and stratum filtering pattern
    // 6. testScoreStratifier_RatioContinuousVariable() - Test RATIO + MEASUREOBSERVATION special case with
    //    separate numerator and denominator MEASUREOBSERVATION populations
    //
    // Constructor Pattern Summary:
    //
    // Use the helper methods defined below for cleaner test code:
    //
    // - createPopulationDef(id, type, subjects) - Creates PopulationDef with boolean basis subjects
    // - createMeasurePopulationCode(code) - Creates CodeDef for measure-population system
    // - createImprovementNotationCode(notation) - Creates CodeDef for improvement notation (increase/decrease)
    // - createPopulationBasisCode(basis) - Creates CodeDef for population basis (boolean/Encounter/etc)
    // - createMeasurePopulationConcept(type) - Creates ConceptDef for population type
    // - createTextOnlyConcept(text) - Creates ConceptDef with text only, no codes
    //
    // Direct Constructor Usage (when helpers don't fit):
    //
    // - PopulationDef: new PopulationDef(id, code, type, expression)
    //   Then call pop.addResource(subjectId, resource) for each subject
    //
    // - For MEASUREOBSERVATION with QuantityDef:
    //   Map<String, QuantityDef> observations = new HashMap<>();
    //   observations.put("obs-1", new QuantityDef(10.0));
    //   pop.addResource(subjectId, observations);
    //
    // - StratumPopulationDef: new StratumPopulationDef(id, subjects, intersectionSet, resourceIds, stratifierType, populationBasis)
    //
    // - StratumDef: new StratumDef(stratumPopulations, valueDefs, subjectIds)
    //
    // - StratifierDef: new StratifierDef(id, code, expression, stratifierType)
    //   Then call stratifierDef.addAllStratum(List.of(stratum1, stratum2, ...))
    //
    // - GroupDef: new GroupDef(id, code, stratifiers, populations, measureScoring, isGroupImpNotation, improvementNotation, populationBasis)
    //
    // - MeasureDef: Will need to examine constructor - likely similar pattern with groups list
    //
    // All tests verify mutation behavior: assertNull(def.getScore()) before, assertNotNull/assertEquals after scoring.
    //
    // ============================================================================
}
```

**Test Coverage Summary:**

The test file demonstrates the complete constructor-based testing pattern for MeasureDefScorer:

- ✅ Simple proportion measure scoring with mutation verification
- ✅ Proportion with exclusions and exceptions (complex formula)
- ✅ Zero denominator handling (null score)
- ✅ Stratifier scoring with multiple strata and mutation verification
- ✅ Helper method (createPopulationDef) showing how to construct PopulationDef with subjects

**Additional tests to implement** using the same constructor pattern (no mocks):

- ✅ Multiple groups scored via score() method
- ✅ RATIO measure scoring (standard and continuous variable)
- ✅ Continuous variable with SUM, AVG, MIN, MAX, MEDIAN, COUNT aggregations
- ✅ Stratifier continuous variable scoring with filtering pattern
- ✅ RATIO continuous variable with separate numerator/denominator MEASUREOBSERVATION populations

All tests must follow these principles:

1. **No Mocks**: Use `new GroupDef(...)`, `new PopulationDef(...)`, etc. with real constructor parameters
2. **Mutation Verification**: Assert `assertNull(def.getScore())` before scoring, then verify score after
3. **Real Data**: Create actual subject sets, observations, and strata using constructors
4. **Helper Methods**: Extract common construction patterns like `createPopulationDef()` to reduce duplication

---

## Phase 7: Update Critical Files Section

**Files to Create:**

1. **New Scorer Class**:
   - `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorer.java`

2. **New Test Class**:
   - `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/MeasureDefScorerTest.java`

**Files to Modify:**

3. **Add Score State** (NOTE: StratumDef requires record → class conversion):
   - `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/GroupDef.java`
     * Add: `private Double score;`
     * Add: `public Double getScore() { return this.score; }`
     * Add: `public void setScore(Double score) { this.score = score; }`

   - `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratumDef.java`
     * **BREAKING CHANGE**: Convert from `record` to `class`
     * Keep all existing fields as `private final`
     * Keep all accessor method names identical (`stratumPopulations()`, `valueDefs()`, `subjectIds()`)
     * Add: `private Double score;`
     * Add: `public Double getScore() { return this.score; }`
     * Add: `public void setScore(Double score) { this.score = score; }`
     * Keep existing methods: `isComponent()`, `getStratumPopulation()`, `getPopulationCount()`

**Note**: The PRP deliberately does NOT rename the existing `MeasureReportScorer` interface or modify `R4MeasureReportScorer`, `Dstu3MeasureReportScorer`, or `BaseMeasureReportScorer` classes. The new `MeasureDefScorer` class is a standalone utility, not an implementation of any existing interface.

---

## Phase 8: Validation and Testing

### Validation Gates

```bash
# 1. Format and style
mvn spotless:apply
mvn checkstyle:check

# 2. Compile and verify no breaking changes
mvn clean compile

# 3. Run new MeasureDefScorer tests
mvn test -pl cqf-fhir-cr -Dtest=MeasureDefScorerTest

# 4. Run all measure tests to check for StratumDef record→class regressions
mvn test -pl cqf-fhir-cr -Dtest=*Measure*

# 5. Search for StratumDef usages and verify compatibility
grep -r "new StratumDef" cqf-fhir-cr/src/
```

### Success Criteria

- [  ] MeasureDefScorer class created with complete scoring logic
- [ ] GroupDef has score field with getter/setter
- [ ] StratumDef converted from record to class with score field + getter/setter
- [ ] All scoring methods use Def-first iteration (MeasureDef.groups(), StratifierDef.getStratum())
- [ ] All scoring methods are void and mutate Def objects via setScore()
- [ ] >10 unit tests using constructors (no mocks) verifying score mutation behavior
- [ ] All existing tests pass (no regressions from StratumDef change)
- [ ] Code formatted and passes checkstyle
- [ ] Full javadoc copied from R4MeasureReportScorer

---

## Risk Assessment

**Medium Risk**: Converting StratumDef from record to class

- **Risk**: May break code that relies on record-specific features (pattern matching, deconstruction)
- **Mitigation**:
  - Keep method signatures identical (use method style: `stratumPopulations()` not `getStratumPopulations()`)
  - Keep all fields as `private final` (except score)
  - Comprehensive testing of all measure-related tests
- **Search**: Identify all StratumDef instantiation sites before implementing:
  ```bash
  grep -r "new StratumDef" cqf-fhir-cr/src/
  ```

**Low Risk**: All other changes are additive

- GroupDef score field is new (doesn't affect existing code)
- MeasureDefScorer is new standalone class (doesn't replace anything)
- No changes to R4/DSTU3 scorers or interfaces

---

## Architecture Benefits

**Before**: Scores computed in FHIR-specific scorers and returned

```
R4MeasureReportScorer.scoreGroup() → Double → Applied to FHIR MeasureReport
Dstu3MeasureReportScorer.scoreGroup() → Double → Applied to FHIR MeasureReport
```

**After**: Def classes are single source of truth for scores

```
MeasureDefScorer.score() → void (sets scores on GroupDef and StratumDef)
GroupDef.getScore() → Double (retrieve score from Def)
StratumDef.getScore() → Double (retrieve score from Def)
```

**Benefits**:
1. **Version-Agnostic**: Scoring logic doesn't depend on R4 vs DSTU3 FHIR types
2. **Def-First Iteration**: Uses Def classes for both data and iteration order
3. **Single Source of Truth**: Def classes store both raw data and computed scores
4. **Reusability**: Can be called from any FHIR version's report builder
5. **Testability**: Pure logic using constructors, no FHIR mocking needed

---

## Implementation Timeline

This PRP is ready for implementation. The implementation order should be:

1. ✅ **Phase 1-2**: Analysis complete (understanding existing code and Def classes)
2. **Phase 3**: Add score fields to GroupDef (low risk, 10 minutes)
3. **Phase 4**: Convert StratumDef from record to class with score field (medium risk, analyze impact first, 30 minutes)
4. **Phase 5**: Create MeasureDefScorer class with full scoring logic (2-3 hours)
5. **Phase 6**: Write comprehensive unit tests using constructors (2-3 hours)
6. **Phase 7**: Run validation gates and fix any issues (1 hour)
7. **Phase 8**: Final review and commit (30 minutes)

**Total Estimated Effort**: 6-8 hours

---

## Questions for Review

1. ✅ **Confirmed**: Should MeasureDefScorer mutate Def objects via setScore() (void methods)?
2. ✅ **Confirmed**: Should we convert StratumDef from record to class to support mutable score field?
3. ✅ **Confirmed**: Should we copy continuous variable logic including aggregate methods (SUM, AVG, etc.)?
4. ✅ **Confirmed**: Should we use the R4 pattern for stratifier scoring (scoreStratifier → scoreStratum → getStratumScoreOrNull)?
5. ✅ **Confirmed**: Should we include RATIO + MEASUREOBSERVATION continuous variable scoring?
6. ✅ **Confirmed**: Should the class be named MeasureDefScorer (not MeasureReportScorer)?
7. ✅ **Confirmed**: Should the main method be named `score()` (not `scoreAllGroups()`)?
8. ✅ **Confirmed**: Should tests use constructors (NOT mocks)?

All questions have been answered and incorporated into this PRP.

---

## Appendix: Key Code Patterns

### Creating PopulationDef with Boolean Basis

```java
private PopulationDef createPopulationDef(String id, MeasurePopulationType type, Set<String> subjects) {
    ConceptDef code = new ConceptDef(
        List.of(new CodeDef("http://terminology.hl7.org/CodeSystem/measure-population", type.toCode())),
        type.toCode()
    );
    PopulationDef pop = new PopulationDef(id, code, type, "expression");

    // Add subjects to population
    for (String subject : subjects) {
        pop.addResource(subject, true);  // For boolean basis, resource is just boolean true
    }

    return pop;
}
```

### Creating PopulationDef with MEASUREOBSERVATION (Continuous Variable)

```java
PopulationDef measureObsPop = new PopulationDef(
    "measure-obs-1",
    new ConceptDef(List.of(new CodeDef("http://terminology.hl7.org/CodeSystem/measure-population", "measure-observation")), "measure-observation"),
    MeasurePopulationType.MEASUREOBSERVATION,
    "MeasureObservationExpression",
    null,  // criteriaReference
    ContinuousVariableObservationAggregateMethod.SUM
);

// Add observations for each subject
Map<String, QuantityDef> patient1Obs = new HashMap<>();
patient1Obs.put("obs-1", new QuantityDef(10.0));
patient1Obs.put("obs-2", new QuantityDef(20.0));
measureObsPop.addResource("patient1", patient1Obs);

Map<String, QuantityDef> patient2Obs = new HashMap<>();
patient2Obs.put("obs-3", new QuantityDef(15.0));
measureObsPop.addResource("patient2", patient2Obs);
```

### Creating StratumDef with StratumPopulationDef

```java
StratumPopulationDef stratumPopDef = new StratumPopulationDef(
    "num-1",  // ID matching the PopulationDef
    Set.of("male1", "male2", "male3"),  // Subjects in this stratum
    Set.of(),  // populationDefEvaluationResultIntersection
    List.of(),  // resourceIdsForSubjectList
    MeasureStratifierType.VALUE,
    new CodeDef("http://hl7.org/fhir/ValueSet/measure-population", "boolean")
);

StratumDef stratumDef = new StratumDef(
    List.of(stratumPopDef),  // stratumPopulations
    Set.of(new StratumValueDef("male", "gender")),  // valueDefs
    Set.of("male1", "male2", "male3")  // subjectIds
);
```

### Creating GroupDef with Stratifiers

```java
GroupDef groupDef = new GroupDef(
    "group-1",
    new ConceptDef(List.of(), "Test Group"),
    List.of(stratifierDef),  // stratifiers
    List.of(numeratorPop, denominatorPop),  // populations
    MeasureScoring.PROPORTION,
    false,  // isGroupImprovementNotation
    new CodeDef("http://terminology.hl7.org/CodeSystem/measure-improvement-notation", "increase"),
    new CodeDef("http://hl7.org/fhir/ValueSet/measure-population", "boolean")
);
```

---

**PRP Status**: ✅ **READY FOR IMPLEMENTATION**

All requirements clarified, design decisions finalized, and implementation approach documented.

**PRP generated by**: Claude Sonnet 4.5 (2025-12-03)
**Based on requirements from**: initial.md
**Refined through**: Multiple user feedback iterations focusing on mutation-based design, continuous variable support, correct stratifier patterns, RATIO continuous variable, class naming, javadoc, and constructor-based testing.

---

**Change Log**:

- 2025-12-03: Initial PRP created with Def-first iteration pattern
- 2025-12-03: Updated to add mutation-based design (setScore on Def classes, void methods)
- 2025-12-03: Added continuous variable support (aggregate methods, scoreContinuousVariable)
- 2025-12-03: Corrected stratifier pattern (scoreStratifier → scoreStratum → getStratumScoreOrNull)
- 2025-12-03: Added RATIO continuous variable support (complete if-else block in getStratumScoreOrNull)
- 2025-12-03: Renamed class to MeasureDefScorer, added full javadoc, renamed main method to score(), removed all mocks from tests
- 2025-12-03: Style improvements:
  - Refactored getStratumScoreOrNull() into three focused private methods: scoreRatioMeasureObservationStratum(), scoreProportionRatioStratum(), scoreContinuousVariableStratum()
  - Optimized aggregate() method to reuse stream pattern for SUM/MAX/MIN/AVG operations
  - Added test helper methods: createMeasurePopulationCode(), createImprovementNotationCode(), createPopulationBasisCode(), createMeasurePopulationConcept(), createTextOnlyConcept()
  - Updated all test examples to use helper methods instead of direct URL strings in constructors

---

**End of PRP**
