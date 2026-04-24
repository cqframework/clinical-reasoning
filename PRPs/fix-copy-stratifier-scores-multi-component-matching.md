# PRP: Fix copyStratifierScores Matching for Multi-Component Stratifiers

## Metadata
- **Title**: Fix broken stratum matching in copyStratifierScores for multi-component stratifiers
- **Status**: Proposed
- **Priority**: High (Bug Fix - DQM-693)
- **Estimated Effort**: 0.5 days
- **Related CQIS Ticket**: DQM-693

## Problem Statement

`R4MeasureReportBuilder#copyStratifierScores` fails to match any multi-component strata, causing stratum scores and per-stratum population aggregation results (criteria references, aggregate methods, aggregation results) to never be copied to the MeasureReport.

### Root Cause

The matching logic in `R4MeasureReportUtils#matchesStratumValue` (line 124-130) only checks the stratum's top-level `value` field:

```java
public static boolean matchesStratumValue(
        StratifierGroupComponent reportStratum, StratumDef stratumDef, StratifierDef stratifierDef) {
    String reportText = reportStratum.hasValue() ? reportStratum.getValue().getText() : null;
    String defText = getStratumDefText(stratifierDef, stratumDef);
    return Objects.equals(reportText, defText);
}
```

For multi-component strata, `R4StratifierBuilder#buildStratum` (line 152-188) sets data on `stratum.component` entries, **never** on `stratum.value`. So `reportStratum.hasValue()` is always `false`, `reportText` is always `null`, and the match always fails.

Meanwhile `getStratumDefText` returns a non-null string for the `StratumDef` (e.g., a component code text like `"Gender"` or a value string like `"74"`).

`Objects.equals(null, "74")` is always `false`. The match never succeeds.

### Downstream Impact

Because `reportStratum` is always null in the `copyStratifierScores` loop, two things never happen:

1. **Stratum scores are never set** (line 636-638 in `R4MeasureReportBuilder`):
   ```java
   Double stratumScore = stratumDef.getScore();
   if (stratumScore != null) {
       reportStratum.getMeasureScore().setValue(stratumScore);
   }
   ```

2. **Per-stratum population aggregation results are never copied** (line 641 `copyStratumPopulationAggregationResults`), which means stratum observation populations are missing:
   - `cqfm-criteriaReference` extension
   - `cqfm-aggregateMethod` extension
   - `cqfm-aggregationMethodResult` extension

   This in turn causes an NPE in CQIS's `StratifierReportAggregator#isNumeratorObservation` when it calls `getCriteriaReference()` on the extensionless stratum population and passes null to `String.equals()`.

### How to Reproduce

Use a Measure with a multi-component stratifier (2+ components in `stratifier.component[]`) and evaluate with `$evaluate-measure`. Inspect the resulting MeasureReport's stratum-level data:
- Stratum measure scores will be absent/null
- Stratum MEASUREOBSERVATION populations will have no `cqfm-criteriaReference`, `cqfm-aggregateMethod`, or `cqfm-aggregationMethodResult` extensions

Compare with a single-component stratifier on the same data — those strata will have scores and extensions properly set.

## Solution Overview

Fix `matchesStratumValue` to handle multi-component strata by matching component-by-component instead of comparing the top-level `value` field.

### How R4StratifierBuilder builds multi-component strata (the "write" side)

For each `StratumValueDef` in a multi-component stratum, `buildStratum` (line 159-182) creates a `StratifierGroupComponentComponent` with:
- `code`: set to `new CodeableConcept().setText(componentDef.code().text())`
- `value`: set to `expressionResultToCodableConcept(value)` which is `new CodeableConcept().setText(value.getValueAsString())`

So each report component has:
- `component.getCode().getText()` = the component definition's code text (e.g., `"Gender"`, `"Age"`)
- `component.getValue().getText()` = the CQL expression result as string (e.g., `"male"`, `"74"`)

### How matching should work (the "read" side)

For each `StratumValueDef` in the `StratumDef`:
- `valueDef.def().code().text()` corresponds to the report component's `code.text`
- `valueDef.value().getValueAsString()` corresponds to the report component's `value.text`

The fix should match all components between the report stratum and the `StratumDef` by pairing on code text and comparing value text.

## Implementation Details

### 1. Fix `matchesStratumValue` in `R4MeasureReportUtils`

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/utils/R4MeasureReportUtils.java` (line 124-130)

Replace the current implementation with one that branches on whether the stratum is component-based:

```java
public static boolean matchesStratumValue(
        StratifierGroupComponent reportStratum, StratumDef stratumDef, StratifierDef stratifierDef) {

    if (stratumDef.isComponent()) {
        return matchesComponentStratumValues(reportStratum, stratumDef);
    }

    // Existing single-value matching logic (unchanged)
    String reportText = reportStratum.hasValue() ? reportStratum.getValue().getText() : null;
    String defText = getStratumDefText(stratifierDef, stratumDef);
    return Objects.equals(reportText, defText);
}
```

Add a new private method for component matching:

```java
/**
 * Match a multi-component report stratum against a StratumDef by comparing
 * each component's code text and value text.
 *
 * For each StratumValueDef in the StratumDef, there must be a matching
 * report component where:
 *   - component.getCode().getText() equals valueDef.def().code().text()
 *   - component.getValue().getText() equals valueDef.value().getValueAsString()
 */
private static boolean matchesComponentStratumValues(
        StratifierGroupComponent reportStratum, StratumDef stratumDef) {

    var reportComponents = reportStratum.getComponent();
    var valueDefs = stratumDef.valueDefs();

    // Component count must match
    if (reportComponents.size() != valueDefs.size()) {
        return false;
    }

    // Every StratumValueDef must find a matching report component
    for (var valueDef : valueDefs) {
        var componentDef = valueDef.def();
        if (componentDef == null || componentDef.code() == null) {
            return false;
        }

        String expectedCodeText = componentDef.code().text();
        String expectedValueText = valueDef.value().getValueAsString();

        boolean found = reportComponents.stream().anyMatch(rc ->
                Objects.equals(expectedCodeText, rc.getCode().getText())
                && Objects.equals(expectedValueText, rc.getValue().getText()));

        if (!found) {
            return false;
        }
    }

    return true;
}
```

**Rationale**:
- Mirrors the symmetry of `R4StratifierBuilder#buildStratum`: the builder sets `code` from `componentDef.code().text()` and `value` from `value.getValueAsString()`, so the matcher reads the same fields
- Order-independent matching (components may appear in different order between reports)
- Size check ensures no extra/missing components
- Single-value matching is unchanged (no risk to existing behavior)

### 2. Add Unit Tests for `matchesStratumValue`

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/utils/R4MeasureReportUtilsTest.java` (create or extend)

Add tests covering:

1. **Single-value stratum matching still works** (regression guard)
   - Report stratum with `value.text = "male"`, StratumDef with single CodeableConcept value "male" -> true
   - Mismatched values -> false

2. **Multi-component matching works**
   - Report stratum with components `[{code:"Gender", value:"male"}, {code:"Age", value:"74"}]` matches StratumDef with matching valueDefs -> true
   - Same components in different order -> true (order-independent)
   - One mismatched value -> false
   - Different component count -> false

3. **Multi-component stratum with non-CodeableConcept values** (e.g., Age as integer)
   - Report component value text "74", StratumDef value `getValueAsString()` returns "74" -> true

### 3. Verify with Existing Integration Tests

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/MeasureStratifierTest.java`

The existing test `cohortBooleanValueStratComponentStrat` (line 194) exercises multi-component stratifiers but does **not** assert stratum scores or population extensions. After the fix, consider extending this test (or adding a new one) to verify that:
- Stratum measure scores are non-null where expected
- Stratum observation populations have `cqfm-criteriaReference` and `cqfm-aggregateMethod` extensions

This is important because these are exactly the fields that were silently not being set before.

## Verification

1. Run existing clinical-reasoning tests: `./gradlew :cqf-fhir-cr:test`
2. Run the multi-component stratifier test specifically: `./gradlew :cqf-fhir-cr:test --tests "*MeasureStratifierTest*cohortBooleanValueStratComponentStrat*"`
3. After integrating this fix into CQIS (by updating the clinical-reasoning dependency version), re-run the CQIS end-to-end test: `./gradlew :cdr-persistence-dqm:test --tests "*BatchMeasureAsyncEndToEndIgCvRcvComboTest*"` — the stratum scores and observation population extensions should now be populated, allowing the CQIS aggregation code to fully function (including RCV stratum score recomputation via `isNumeratorObservation`/`isDenominatorObservation`)
4. At that point, the null guard added to `StratifierReportAggregator#isNumeratorObservation`/`isDenominatorObservation` in CQIS will become a defensive safety net rather than a required workaround

## Files Changed

| File | Change |
|------|--------|
| `cqf-fhir-cr/.../r4/utils/R4MeasureReportUtils.java` | Fix `matchesStratumValue` to handle multi-component strata; add `matchesComponentStratumValues` |
| `cqf-fhir-cr/.../r4/utils/R4MeasureReportUtilsTest.java` | Add unit tests for component matching |
| `cqf-fhir-cr/.../r4/MeasureStratifierTest.java` | Optionally extend to assert stratum scores/extensions for multi-component strata |

## Secondary Concern: `getStratumDefText` for Multi-Component Strata

The `getStratumDefText` method (line 53-110) has a secondary bug in its multi-component path (line 89-109): for CodeableConcept component values, it returns the **component code text** (e.g., `"Gender"`) instead of the **component value text** (e.g., `"male"`). For non-CodeableConcept values, it returns the value but breaks out of the loop on the first component, ignoring the rest. This method is not needed for the component matching fix (which bypasses `getStratumDefText` entirely), but it may cause issues if other callers rely on it for multi-component strata. Consider cleaning it up as a follow-up.
