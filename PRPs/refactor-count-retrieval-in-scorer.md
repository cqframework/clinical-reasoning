# PRP: Refactor R4MeasureReportScorer to Use Def Classes for Population Counts

## Overview
This PRP combines three related refactorings to reduce FHIR coupling and eliminate redundant code in measure scoring:

### 1. Simplify QuantityDef (Remove unit, system, code)
Remove `unit`, `system`, and `code` fields from `QuantityDef` since only the numeric `value` is needed for measure scoring calculations. This simplifies aggregation logic and eliminates unnecessary data preservation.

### 2. Eliminate FHIR Quantity Code Path from Converters
Remove dead code that assumes CQL evaluation could return FHIR Quantities. CQL evaluation NEVER returns FHIR types - only Number, String, or CQL Quantity types. Move the simple Number/String conversion logic from the converter interface to `ContinuousVariableObservationHandler` where it's actually used. This reduces the converter interface to a single method: `convertToFhirQuantity()`.

### 3. Refactor Count Retrieval to Use Def Classes
Refactor `R4MeasureReportScorer` to retrieve population counts directly from `PopulationDef` and `StratumPopulationDef` instead of extracting them from FHIR `MeasureReport` components. This eliminates redundant round-tripping of count data through FHIR objects and establishes a clearer separation between data calculation (in Def classes) and FHIR representation (in Report components).

## Background

### Current Architecture

The current flow for population counts involves unnecessary round-tripping:

1. **Count Calculation** → Counts are calculated in `PopulationDef` / `StratumPopulationDef`
2. **FHIR Population** → Counts are written to `MeasureReportGroupPopulationComponent` / `StratifierGroupPopulationComponent`
3. **Score Calculation** → `R4MeasureReportScorer` reads counts back from FHIR components

**Example Current Flow:**

```
PopulationDef.getSubjects().size()
  → R4MeasureReportBuilder sets MeasureReportGroupPopulationComponent.count
    → R4MeasureReportScorer.getCountFromGroupPopulation reads from MeasureReportGroupPopulationComponent
      → Used in proportion score calculation
```

### Problem Statement

This architecture creates several issues:

1. **Unnecessary Coupling**: `R4MeasureReportScorer` depends on FHIR report structure being populated before scoring can occur
2. **Redundant Logic**: Count calculation logic exists in two places:
   - `R4MeasureReportBuilder.buildPopulation()` (lines 267-278) - calculates and sets counts
   - `R4MeasureReportScorer.getCountFromGroupPopulation()` - extracts counts from FHIR objects
3. **Testing Complexity**: Cannot test scoring logic without building full `MeasureReport` structures
4. **Architectural Misalignment**: Violates the pattern established by `StratumPopulationDef.getCount()` which already provides direct count access

### Key Files Involved

#### Where Counts Are Currently Set
- `R4MeasureReportBuilder.java:268-277` - Sets `MeasureReportGroupPopulationComponent.count` from `PopulationDef`
- `R4StratifierBuilder.java:286` - Sets `StratifierGroupPopulationComponent.count` from `StratumPopulationDef.getCount()`

#### Where Counts Are Currently Read
- `R4MeasureReportScorer.java:577-595` - Methods `getCountFromGroupPopulation()` and `getCountFromStratifierPopulation()`
- Called from:
  - `scoreGroup()` (lines 183-187) - 5 calls for proportion/ratio scoring
  - `getStratumScoreOrNull()` (lines 509-510) - 2 calls for stratifier scoring

#### Existing Count Logic in Def Classes

**PopulationDef** (has count data but no unified method):
- `getSubjects().size()` - for boolean basis measures
- `getAllSubjectResources().size()` - for resource basis measures
- `countObservations()` - for measure observation populations

**StratumPopulationDef** (already has unified count method):
- `getCount()` (lines 40-53) - handles all stratifier types:
  ```java
  public int getCount() {
      if (MeasureStratifierType.CRITERIA == measureStratifierType) {
          return populationDefEvaluationResultIntersection.size();
      }
      if (isBooleanBasis()) {
          return subjectsQualifiedOrUnqualified.size();
      }
      return resourceIdsForSubjectList.size();
  }
  ```

### Existing Patterns

The codebase already demonstrates the desired pattern:

**Good Example - StratumPopulationDef:**
```java
// R4StratifierBuilder.java:286
sgpc.setCount(stratumPopulationDef.getCount()); // Direct from Def class
```

**Anti-Pattern - PopulationDef:**
```java
// R4MeasureReportScorer.java:183-187
getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR) // Reads from FHIR component
```

## Requirements

### Part 1: Simplify QuantityDef
1. Remove `unit`, `system`, and `code` fields from `QuantityDef` (only keep `value`)
2. Remove corresponding constructor parameters and accessor methods
3. Update `R4ContinuousVariableObservationConverter` to not set unit/system/code
4. Update `Dstu3ContinuousVariableObservationConverter` to not set unit/system/code
5. Remove `R4MeasureReportScorer.aggregate()` logic that preserves unit/system/code
6. Update all test assertions that verify unit/system/code values
7. **Eliminate FHIR Quantity code path**: Remove `extractQuantityDef()` method and FHIR Quantity handling from converters - CQL never returns FHIR Quantities
8. **Inline conversion logic**: Move Number/String handling from interface default method to `ContinuousVariableObservationHandler` as private method

### Part 2: Refactor Count Retrieval
1. Add `getCount(GroupDef)` method to `PopulationDef` that mirrors the logic in `R4MeasureReportBuilder:268-277`
2. Modify `R4MeasureReportScorer.scoreGroup()` to retrieve counts directly from `PopulationDef` instances
3. Modify `R4MeasureReportScorer.getStratumScoreOrNull()` to retrieve counts directly from `StratumPopulationDef` instances
4. **Remove** (not deprecate) `getCountFromGroupPopulation()` and `getCountFromStratifierPopulation()` methods - they are private with no external callers
5. Ensure all existing tests pass
6. Update scoring tests to validate direct Def access pattern

## Implementation Plan

### Phase 0: Simplify QuantityDef (Remove unit, system, code)

**Rationale**: The `unit`, `system`, and `code` fields in `QuantityDef` are not needed for measure scoring calculations. Only the numeric `value` is used in aggregation and score calculation. Removing these fields simplifies the class and eliminates unnecessary data preservation logic.

#### Step 0.1: Update QuantityDef Class

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/QuantityDef.java`

**Remove fields and methods:**

```java
package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;

// Updated by Claude Sonnet 4.5 on 2025-12-02
/**
 * FHIR-version-agnostic representation of a Quantity value.
 * Used for continuous variable measure scoring to avoid coupling to specific FHIR versions.
 *
 * Only stores the numeric value - unit, system, and code are not needed for scoring calculations.
 *
 * @see CodeDef
 * @see ConceptDef
 */
public class QuantityDef {

    @Nullable
    private final Double value;

    public QuantityDef(@Nullable Double value) {
        this.value = value;
    }

    @Nullable
    public Double value() {
        return value;
    }

    @Override
    public String toString() {
        return "QuantityDef{value=" + value + '}';
    }
}
```

**Changes:**
- Removed `unit`, `system`, `code` fields
- Removed 4-parameter constructor
- Kept only single-parameter constructor
- Removed `unit()`, `system()`, `code()` accessor methods
- Simplified `toString()` method

#### Step 0.2: Update R4ContinuousVariableObservationConverter

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4ContinuousVariableObservationConverter.java`

**Update `extractQuantityDef()` method (line ~40):**

```java
// BEFORE:
@Override
public QuantityDef extractQuantityDef(Object result) {
    if (result instanceof Quantity existing) {
        return new QuantityDef(
            existing.hasValue() ? existing.getValue().doubleValue() : null,
            existing.getUnit(),
            existing.getSystem(),
            existing.getCode()
        );
    }
    return null;
}

// AFTER:
// Updated by Claude Sonnet 4.5 on 2025-12-02
@Override
public QuantityDef extractQuantityDef(Object result) {
    if (result instanceof Quantity existing) {
        return new QuantityDef(
            existing.hasValue() ? existing.getValue().doubleValue() : null);
    }
    return null;
}
```

**Update `convertToFhirQuantity()` method (line ~55):**

```java
// BEFORE:
@Override
public Quantity convertToFhirQuantity(QuantityDef quantityDef) {
    if (quantityDef == null) {
        return null;
    }

    Quantity quantity = new Quantity();
    Double value = quantityDef.value();
    if (value != null) {
        quantity.setValue(value);
    }
    if (quantityDef.unit() != null) {
        quantity.setUnit(quantityDef.unit());
    }
    if (quantityDef.system() != null) {
        quantity.setSystem(quantityDef.system());
    }
    if (quantityDef.code() != null) {
        quantity.setCode(quantityDef.code());
    }
    return quantity;
}

// AFTER:
// Updated by Claude Sonnet 4.5 on 2025-12-02
@Override
public Quantity convertToFhirQuantity(QuantityDef quantityDef) {
    if (quantityDef == null) {
        return null;
    }

    Quantity quantity = new Quantity();
    Double value = quantityDef.value();
    if (value != null) {
        quantity.setValue(value);
    }
    // Note: unit, system, code are not preserved - only the numeric value
    return quantity;
}
```

#### Step 0.3: Update Dstu3ContinuousVariableObservationConverter

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3ContinuousVariableObservationConverter.java`

**Apply identical changes as R4:**

```java
// Updated by Claude Sonnet 4.5 on 2025-12-02
@Override
public QuantityDef extractQuantityDef(Object result) {
    if (result instanceof Quantity existing) {
        return new QuantityDef(
            existing.hasValue() ? existing.getValue().doubleValue() : null);
    }
    return null;
}

@Override
public Quantity convertToFhirQuantity(QuantityDef quantityDef) {
    if (quantityDef == null) {
        return null;
    }

    Quantity quantity = new Quantity();
    Double value = quantityDef.value();
    if (value != null) {
        quantity.setValue(value);
    }
    // Note: unit, system, code are not preserved - only the numeric value
    return quantity;
}
```

#### Step 0.4: Update R4MeasureReportScorer.aggregate()

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java`

**Simplify `aggregate()` method (lines 284-356):**

```java
// BEFORE:
private static QuantityDef aggregate(
        List<QuantityDef> quantities, ContinuousVariableObservationAggregateMethod method) {
    if (quantities == null || quantities.isEmpty()) {
        return null;
    }

    if (ContinuousVariableObservationAggregateMethod.N_A == method) {
        throw new InvalidRequestException(
                "Aggregate method must be provided for continuous variable scoring, but is NO-OP.");
    }

    // assume all quantities share the same unit/system/code
    QuantityDef base = quantities.get(0);
    String unit = base.unit();
    String system = base.system();
    String code = base.code();

    double result;

    switch (method) {
        // ... calculation logic ...
    }

    return new QuantityDef(result, unit, system, code);
}

// AFTER:
// Updated by Claude Sonnet 4.5 on 2025-12-02
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

    switch (method) {
        case SUM:
            result = quantities.stream()
                    .map(QuantityDef::value)
                    .filter(Objects::nonNull)
                    .mapToDouble(value -> value)
                    .sum();
            break;
        case MAX:
            result = quantities.stream()
                    .map(QuantityDef::value)
                    .filter(Objects::nonNull)
                    .mapToDouble(value -> value)
                    .max()
                    .orElse(Double.NaN);
            break;
        case MIN:
            result = quantities.stream()
                    .map(QuantityDef::value)
                    .filter(Objects::nonNull)
                    .mapToDouble(value -> value)
                    .min()
                    .orElse(Double.NaN);
            break;
        case AVG:
            result = quantities.stream()
                    .map(QuantityDef::value)
                    .filter(Objects::nonNull)
                    .mapToDouble(value -> value)
                    .average()
                    .orElse(Double.NaN);
            break;
        case COUNT:
            result = quantities.size();
            break;
        case MEDIAN:
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
            break;
        default:
            throw new IllegalArgumentException("Unsupported aggregation method: " + method);
    }

    return new QuantityDef(result);
}
```

**Changes:**
- Removed unit/system/code extraction from first QuantityDef
- Removed unit/system/code parameters from return statement
- Calculation logic remains identical

#### Step 0.5: Update Test Files

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/QuantityDefTest.java`

**Remove all assertions on unit/system/code:**

```java
// BEFORE - testQuantityDefCreation():
QuantityDef qd = new QuantityDef(42.5, "mg", "http://unitsofmeasure.org", "mg");
assertEquals(42.5, qd.value());
assertEquals("mg", qd.unit());
assertEquals("http://unitsofmeasure.org", qd.system());
assertEquals("mg", qd.code());

// AFTER - testQuantityDefCreation():
QuantityDef qd = new QuantityDef(42.5);
assertEquals(42.5, qd.value());

// BEFORE - testQuantityDefWithValueOnly():
QuantityDef qd = new QuantityDef(100.0);
assertEquals(100.0, qd.value());
assertNull(qd.unit());
assertNull(qd.system());
assertNull(qd.code());

// AFTER - testQuantityDefWithValueOnly():
QuantityDef qd = new QuantityDef(100.0);
assertEquals(100.0, qd.value());

// Also update testQuantityDefWithAllNullValues() and testQuantityDefToString()
```

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4ContinuousVariableObservationConverterTest.java`

**Remove unit/system/code assertions from all tests:**

Tests to update:
- `testWrapNumberAsQuantityDef()` - remove `assertNull(result.unit())`
- `testWrapStringAsQuantityDef()` - remove unit assertion
- `testWrapIntegerAsQuantityDef()` - remove unit assertion
- `testWrapR4QuantityAsQuantityDef()` - remove unit/system/code assertions (lines 64-66)
- `testWrapR4QuantityWithOnlyValueAsQuantityDef()` - remove unit/system/code assertions (lines 82-84)
- `testConvertQuantityDefToR4Quantity()` - remove unit/system/code assertions (lines 100-102)
- `testRoundTripConversion()` - **This test needs to be updated** to NOT expect unit/system/code preservation
- `testConvertQuantityDefWithNullValuesToR4Quantity()` - update expectations

**Example for testConvertQuantityDefToR4Quantity():**

```java
// BEFORE:
@Test
void testConvertQuantityDefToR4Quantity() {
    var converter = R4ContinuousVariableObservationConverter.INSTANCE;
    QuantityDef qd = new QuantityDef(75.0, "mmHg", "http://unitsofmeasure.org", "mm[Hg]");

    Quantity result = converter.convertToFhirQuantity(qd);

    assertEquals(75.0, result.getValue().doubleValue());
    assertEquals("mmHg", result.getUnit());
    assertEquals("http://unitsofmeasure.org", result.getSystem());
    assertEquals("mm[Hg]", result.getCode());
}

// AFTER:
@Test
void testConvertQuantityDefToR4Quantity() {
    var converter = R4ContinuousVariableObservationConverter.INSTANCE;
    QuantityDef qd = new QuantityDef(75.0);

    Quantity result = converter.convertToFhirQuantity(qd);

    assertEquals(75.0, result.getValue().doubleValue());
    // Note: unit, system, code are not preserved - only value matters for scoring
    assertNull(result.getUnit());
    assertNull(result.getSystem());
    assertNull(result.getCode());
}
```

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3ContinuousVariableObservationConverterTest.java`

**Apply identical changes as R4 test file.**

#### Step 0.6: Update Test Helper Methods

Search for any test helper methods that create `QuantityDef` with 4 parameters and update them to use single parameter:

```bash
# Find helper methods
grep -n "new QuantityDef(" cqf-fhir-cr/src/test/java/**/*.java
```

Update each occurrence to use single-parameter constructor.

#### Step 0.7: Eliminate FHIR Quantity Code Path from Converters

**Rationale**: CQL evaluation NEVER returns FHIR Quantities (R4 or DSTU3). The `observationResult.value()` comes directly from CQL evaluation (line 198 in `ContinuousVariableObservationHandler`), which returns Number, String, or CQL Quantity types - never FHIR Quantities. The current `extractQuantityDef()` method and FHIR Quantity handling is dead code that can never execute in production.

**Analysis of call site**:
```java
// ContinuousVariableObservationHandler.java:144
var quantity = continuousVariableObservationConverter.wrapResultAsQuantity(observationResult.value());
```

The `observationResult.value()` comes from:
```java
// Line 198: evaluateObservationCriteria()
result = context.getEvaluationVisitor().visitExpression(ed.getExpression(), context.getState());
```

This is **CQL evaluation output**, which can only be:
- `Number` (Integer, Double, Long, etc.)
- `String`
- CQL `Quantity` (from `org.opencds.cqf.cql.engine.runtime`, NOT FHIR)

It can NEVER be a FHIR Quantity because FHIR objects don't exist in CQL evaluation context.

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/ContinuousVariableObservationConverter.java`

**Remove the interface entirely and replace with simple interface:**

```java
package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.fhir.instance.model.api.ICompositeType;

// Updated by Claude Sonnet 4.5 on 2025-12-02
/**
 * Convert QuantityDef to FHIR-specific Quantity for MeasureReport population.
 *
 * Simplified interface - conversion FROM CQL results is now handled directly in
 * ContinuousVariableObservationHandler since CQL never returns FHIR Quantities.
 */
public interface ContinuousVariableObservationConverter<T extends ICompositeType> {

    /**
     * Convert QuantityDef to FHIR-specific Quantity for MeasureReport population.
     *
     * @param quantityDef the version-agnostic quantity (only contains value)
     * @return FHIR-specific Quantity (R4 or DSTU3)
     */
    T convertToFhirQuantity(QuantityDef quantityDef);
}
```

**Changes:**
- Removed `wrapResultAsQuantity()` default method
- Removed `extractQuantityDef()` abstract method
- Kept only `convertToFhirQuantity()` - the only method actually needed
- Updated javadoc to explain simplified design

#### Step 0.8: Simplify R4ContinuousVariableObservationConverter

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4ContinuousVariableObservationConverter.java`

**Remove all methods except convertToFhirQuantity():**

```java
package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;
import org.opencds.cqf.fhir.cr.measure.common.QuantityDef;

// Updated by Claude Sonnet 4.5 on 2025-12-02
/**
 * R4-specific converter for QuantityDef to FHIR Quantity.
 *
 * This implementation only handles conversion TO R4 Quantity for MeasureReport population.
 * Conversion FROM CQL results is handled in ContinuousVariableObservationHandler.
 */
@SuppressWarnings("squid:S6548")
public enum R4ContinuousVariableObservationConverter implements ContinuousVariableObservationConverter<Quantity> {
    INSTANCE;

    @Override
    public Quantity convertToFhirQuantity(QuantityDef quantityDef) {
        if (quantityDef == null) {
            return null;
        }

        Quantity quantity = new Quantity();
        Double value = quantityDef.value();
        if (value != null) {
            quantity.setValue(value);
        }
        // Note: unit, system, code are not preserved - only the numeric value
        return quantity;
    }
}
```

**Changes:**
- Removed `extractQuantityDef()` method entirely (dead code)
- Kept only `convertToFhirQuantity()` method
- Much simpler implementation: ~20 lines instead of ~60

#### Step 0.9: Simplify Dstu3ContinuousVariableObservationConverter

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3ContinuousVariableObservationConverter.java`

**Apply identical simplification as R4:**

```java
package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.hl7.fhir.dstu3.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;
import org.opencds.cqf.fhir.cr.measure.common.QuantityDef;

// Updated by Claude Sonnet 4.5 on 2025-12-02
/**
 * DSTU3-specific converter for QuantityDef to FHIR Quantity.
 *
 * This implementation only handles conversion TO DSTU3 Quantity for MeasureReport population.
 * Conversion FROM CQL results is handled in ContinuousVariableObservationHandler.
 */
@SuppressWarnings("squid:S6548")
public enum Dstu3ContinuousVariableObservationConverter implements ContinuousVariableObservationConverter<Quantity> {
    INSTANCE;

    @Override
    public Quantity convertToFhirQuantity(QuantityDef quantityDef) {
        if (quantityDef == null) {
            return null;
        }

        Quantity quantity = new Quantity();
        Double value = quantityDef.value();
        if (value != null) {
            quantity.setValue(value);
        }
        // Note: unit, system, code are not preserved - only the numeric value
        return quantity;
    }
}
```

#### Step 0.10: Move Conversion Logic to ContinuousVariableObservationHandler

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/ContinuousVariableObservationHandler.java`

**Add private conversion method and update call site:**

```java
// Add after line 156 (end of processMeasureObservationPopulation method)

// Added by Claude Sonnet 4.5 on 2025-12-02
/**
 * Convert CQL evaluation result to QuantityDef.
 *
 * CQL evaluation can return Number, String, or CQL Quantity - never FHIR Quantities.
 *
 * @param result the CQL evaluation result
 * @return QuantityDef containing the numeric value
 * @throws IllegalArgumentException if result cannot be converted to a number
 */
private static QuantityDef convertCqlResultToQuantityDef(Object result) {
    if (result == null) {
        return null;
    }

    // Handle Number (most common case)
    if (result instanceof Number number) {
        return new QuantityDef(number.doubleValue());
    }

    // Handle String with validation
    if (result instanceof String s) {
        try {
            return new QuantityDef(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("String is not a valid number: " + s, e);
        }
    }

    // TODO: Handle CQL Quantity if needed (org.opencds.cqf.cql.engine.runtime.Quantity)
    // For now, unsupported

    throw new IllegalArgumentException(
        "Cannot convert CQL result of type " + result.getClass() + " to QuantityDef. " +
        "Expected Number or String.");
}
```

**Update line 144:**

```java
// BEFORE:
var quantity = continuousVariableObservationConverter.wrapResultAsQuantity(observationResult.value());

// AFTER:
var quantity = convertCqlResultToQuantityDef(observationResult.value());
```

**Changes:**
- Conversion logic is now private method in the only class that needs it
- Method is static - no converter instance needed for this operation
- Clearer naming: `convertCqlResultToQuantityDef` vs generic `wrapResultAsQuantity`
- More specific exception message mentioning CQL
- TODO comment for future CQL Quantity support if needed
- Remove `continuousVariableObservationConverter` parameter from `continuousVariableEvaluation()` method signature if no longer used

#### Step 0.11: Update Converter Tests

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4ContinuousVariableObservationConverterTest.java`

**Remove all tests for `wrapResultAsQuantity()` and `extractQuantityDef()`:**

Tests to **DELETE**:
- `testWrapNumberAsQuantityDef()` - DELETE (now tested in Handler)
- `testWrapIntegerAsQuantityDef()` - DELETE
- `testWrapStringAsQuantityDef()` - DELETE
- `testWrapInvalidStringThrowsException()` - DELETE
- `testWrapR4QuantityAsQuantityDef()` - DELETE (dead code path)
- `testWrapR4QuantityWithOnlyValueAsQuantityDef()` - DELETE (dead code path)
- `testWrapNullReturnsNull()` - DELETE
- `testWrapUnsupportedTypeThrowsException()` - DELETE

Tests to **KEEP**:
- `testConvertQuantityDefToR4Quantity()` - KEEP (update to remove unit/system/code assertions)
- `testConvertNullQuantityDefReturnsNull()` - KEEP
- `testConvertQuantityDefWithNullValuesToR4Quantity()` - KEEP (update expectations)

**After cleanup, R4ContinuousVariableObservationConverterTest should only have ~3-4 tests.**

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3ContinuousVariableObservationConverterTest.java`

**Apply identical deletions as R4 test.**

**New test file**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/ContinuousVariableObservationHandlerTest.java`

**Add tests for new private conversion method:**

```java
// Test convertCqlResultToQuantityDef via reflection or make it package-private for testing
// Tests:
// - testConvertNumberToQuantityDef()
// - testConvertIntegerToQuantityDef()
// - testConvertStringToQuantityDef()
// - testConvertInvalidStringThrowsException()
// - testConvertNullReturnsNull()
// - testConvertUnsupportedTypeThrowsException()
```

Alternatively, if the method remains private, these scenarios are covered by existing integration tests for continuous variable measures.

### Phase 1: Add getCount() Method to PopulationDef

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/PopulationDef.java`

**Add new method after line 149:**

```java
// Added by Claude Sonnet 4.5 on 2025-12-02
/**
 * Get the count for this population based on the measure's population basis.
 *
 * This method centralizes count calculation logic that was previously duplicated
 * between R4MeasureReportBuilder and R4MeasureReportScorer.
 *
 * @param groupDef The group definition to determine population basis
 * @return The count of subjects/resources in this population
 */
public int getCount(GroupDef groupDef) {
    if (groupDef.isBooleanBasis()) {
        return this.getSubjects().size();
    } else if (this.measurePopulationType.equals(MeasurePopulationType.MEASUREOBSERVATION)) {
        return this.countObservations();
    } else {
        return this.getAllSubjectResources().size();
    }
}
```

**Rationale:**
- Mirrors exact logic from `R4MeasureReportBuilder.buildPopulation()` (lines 267-277)
- Provides single source of truth for population counts
- Follows pattern established by `StratumPopulationDef.getCount()`
- Accepts `GroupDef` parameter to determine boolean vs resource basis

### Phase 2: Add Helper Method to Find StratumPopulationDef

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java`

**Add new protected method after line 595:**

```java
// Added by Claude Sonnet 4.5 on 2025-12-02
/**
 * Find StratumPopulationDef matching the given population type within a StratumDef.
 *
 * @param stratumDef The stratum definition to search
 * @param populationType The population type to find (e.g., NUMERATOR, DENOMINATOR)
 * @return The matching StratumPopulationDef or null if not found
 */
@Nullable
protected StratumPopulationDef findStratumPopulationDef(
        @Nullable StratumDef stratumDef, MeasurePopulationType populationType) {
    if (stratumDef == null) {
        return null;
    }

    return stratumDef.stratumPopulations().stream()
            .filter(stratumPopDef -> stratumPopDef.id().startsWith(populationType.toCode()))
            .findFirst()
            .orElse(null);
}
```

**Rationale:**
- Enables lookup of `StratumPopulationDef` by population type
- Follows same pattern as existing `findPopulationDef()` method
- Uses `startsWith()` to match both "numerator" and "numerator-1" style IDs

### Phase 3: Refactor scoreGroup() to Use PopulationDef Directly

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java`

**Replace lines 183-187:**

```java
// BEFORE:
score = calcProportionScore(
        getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR)
                - getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR_EXCLUSION),
        getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR)
                - getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCLUSION)
                - getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCEPTION));

// AFTER:
// Refactored by Claude Sonnet 4.5 on 2025-12-02 to use PopulationDef directly
PopulationDef numPop = groupDef.findPopulationByType(NUMERATOR);
PopulationDef numExPop = groupDef.findPopulationByType(NUMERATOR_EXCLUSION);
PopulationDef denPop = groupDef.findPopulationByType(DENOMINATOR);
PopulationDef denExPop = groupDef.findPopulationByType(DENOMINATOR_EXCLUSION);
PopulationDef denExcPop = groupDef.findPopulationByType(DENOMINATOR_EXCEPTION);

int numCount = numPop != null ? numPop.getCount(groupDef) : 0;
int numExCount = numExPop != null ? numExPop.getCount(groupDef) : 0;
int denCount = denPop != null ? denPop.getCount(groupDef) : 0;
int denExCount = denExPop != null ? denExPop.getCount(groupDef) : 0;
int denExcCount = denExcPop != null ? denExcPop.getCount(groupDef) : 0;

score = calcProportionScore(
        numCount - numExCount,
        denCount - denExCount - denExcCount);
```

**Key Changes:**
- Retrieve `PopulationDef` instances using `groupDef.findPopulationByType()`
- Call `getCount(groupDef)` on each `PopulationDef`
- Handle null `PopulationDef` instances gracefully (return 0)
- Calculation logic remains identical

### Phase 4: Refactor getStratumScoreOrNull() to Use StratumPopulationDef Directly

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java`

**Replace lines 507-510:**

```java
// BEFORE:
// Standard Proportion & Ratio Scoring
score = calcProportionScore(
        getCountFromStratifierPopulation(stratum.getPopulation(), NUMERATOR),
        getCountFromStratifierPopulation(stratum.getPopulation(), DENOMINATOR));

// AFTER:
// Refactored by Claude Sonnet 4.5 on 2025-12-02 to use StratumPopulationDef directly
StratumPopulationDef numStratumPop = findStratumPopulationDef(stratumDef, NUMERATOR);
StratumPopulationDef denStratumPop = findStratumPopulationDef(stratumDef, DENOMINATOR);

int numCount = numStratumPop != null ? numStratumPop.getCount() : 0;
int denCount = denStratumPop != null ? denStratumPop.getCount() : 0;

score = calcProportionScore(numCount, denCount);
```

**Key Changes:**
- Use new `findStratumPopulationDef()` helper method
- Call `getCount()` on `StratumPopulationDef` (method already exists)
- Handle null instances gracefully
- Calculation logic remains identical

### Phase 5: Remove Old Methods

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorer.java`

**Delete methods at lines 577-595:**

```java
// DELETE THESE METHODS ENTIRELY - they are private and have no remaining callers after Phase 3 and 4

// DELETE: getCountFromGroupPopulation() (lines 577-585)
private int getCountFromGroupPopulation(
        List<MeasureReportGroupPopulationComponent> populations, String populationName) {
    // ... DELETE ENTIRE METHOD ...
}

// DELETE: getCountFromStratifierPopulation() (lines 587-595)
private int getCountFromStratifierPopulation(
        List<StratifierGroupPopulationComponent> populations, String populationName) {
    // ... DELETE ENTIRE METHOD ...
}
```

**Rationale:**
- Both methods are `private` - not part of public/protected API
- All 7 call sites are being replaced in Phase 3 and Phase 4
- No external code can reference these methods
- No subclasses can override them
- DSTU3 scorer doesn't have equivalent methods
- No deprecation period needed for internal refactoring

**Complete removal is cleaner and simpler than deprecation** for private implementation details.

### Phase 6: Update R4MeasureReportBuilder (Optional - for consistency)

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportBuilder.java`

**Replace lines 267-278 with new PopulationDef.getCount() method:**

```java
// BEFORE:
if (groupDef.isBooleanBasis()) {
    reportPopulation.setCount(populationDef.getSubjects().size());
} else {
    if (populationDef.type().equals(MeasurePopulationType.MEASUREOBSERVATION)) {
        reportPopulation.setCount(populationDef.countObservations());
    } else {
        reportPopulation.setCount(populationDef.getAllSubjectResources().size());
    }
}

// AFTER:
// Refactored by Claude Sonnet 4.5 on 2025-12-02 to use centralized count logic
reportPopulation.setCount(populationDef.getCount(groupDef));
```

**Benefits:**
- Single line instead of 11 lines
- Eliminates duplication of count logic
- Clearer intent: "set count from population definition"
- Both builder and scorer now use same `getCount()` method

**Note**: This change is optional but recommended for code consistency and maintainability.

## Testing Strategy

### Phase 1: Unit Tests for PopulationDef.getCount()

**New test file**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/PopulationDefCountTest.java`

```java
package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for PopulationDef.getCount() method.
 *
 * Generated by Claude Sonnet 4.5 on 2025-12-02
 */
class PopulationDefCountTest {

    @Test
    void testGetCount_BooleanBasis() {
        // Setup
        GroupDef groupDef = mock(GroupDef.class);
        when(groupDef.isBooleanBasis()).thenReturn(true);

        PopulationDef popDef = new PopulationDef(
            "numerator-1",
            new ConceptDef("Numerator", java.util.List.of()),
            MeasurePopulationType.NUMERATOR,
            "Numerator");

        // Add subjects
        popDef.addResource("Patient/1", new Object());
        popDef.addResource("Patient/2", new Object());
        popDef.addResource("Patient/3", new Object());

        // Execute
        int count = popDef.getCount(groupDef);

        // Verify
        assertEquals(3, count, "Should return subject count for boolean basis");
    }

    @Test
    void testGetCount_ResourceBasis() {
        // Setup
        GroupDef groupDef = mock(GroupDef.class);
        when(groupDef.isBooleanBasis()).thenReturn(false);

        PopulationDef popDef = new PopulationDef(
            "denominator-1",
            new ConceptDef("Denominator", java.util.List.of()),
            MeasurePopulationType.DENOMINATOR,
            "Denominator");

        // Add resources for multiple subjects
        popDef.addResource("Patient/1", new MockResource("Encounter/1"));
        popDef.addResource("Patient/1", new MockResource("Encounter/2"));
        popDef.addResource("Patient/2", new MockResource("Encounter/3"));

        // Execute
        int count = popDef.getCount(groupDef);

        // Verify - 3 total resources
        assertEquals(3, count, "Should return resource count for resource basis");
    }

    @Test
    void testGetCount_MeasureObservation() {
        // Setup
        GroupDef groupDef = mock(GroupDef.class);
        when(groupDef.isBooleanBasis()).thenReturn(false);

        PopulationDef popDef = new PopulationDef(
            "measure-observation-1",
            new ConceptDef("Measure Observation", java.util.List.of()),
            MeasurePopulationType.MEASUREOBSERVATION,
            "MeasureObservation",
            null,
            ContinuousVariableObservationAggregateMethod.SUM);

        // Add observation maps
        Map<String, Object> obs1 = new HashMap<>();
        obs1.put("resource1", new QuantityDef(10.0));
        obs1.put("resource2", new QuantityDef(20.0));

        Map<String, Object> obs2 = new HashMap<>();
        obs2.put("resource3", new QuantityDef(30.0));

        popDef.addResource("Patient/1", obs1);
        popDef.addResource("Patient/2", obs2);

        // Execute
        int count = popDef.getCount(groupDef);

        // Verify - countObservations() counts map entries
        assertEquals(3, count, "Should return observation count for measure observation population");
    }

    @Test
    void testGetCount_EmptyPopulation() {
        // Setup
        GroupDef groupDef = mock(GroupDef.class);
        when(groupDef.isBooleanBasis()).thenReturn(true);

        PopulationDef popDef = new PopulationDef(
            "numerator-1",
            new ConceptDef("Numerator", java.util.List.of()),
            MeasurePopulationType.NUMERATOR,
            "Numerator");

        // Execute - no resources added
        int count = popDef.getCount(groupDef);

        // Verify
        assertEquals(0, count, "Should return 0 for empty population");
    }

    // Helper class for resource basis testing
    private static class MockResource {
        private final String id;

        MockResource(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MockResource that = (MockResource) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
```

### Phase 2: Unit Tests for findStratumPopulationDef()

**Add to existing test file**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureReportScorerTest.java`

```java
@Test
void testFindStratumPopulationDef_Found() {
    // Setup
    StratumPopulationDef numPop = mock(StratumPopulationDef.class);
    when(numPop.id()).thenReturn("numerator-1");

    StratumPopulationDef denPop = mock(StratumPopulationDef.class);
    when(denPop.id()).thenReturn("denominator-1");

    StratumDef stratumDef = mock(StratumDef.class);
    when(stratumDef.stratumPopulations()).thenReturn(java.util.List.of(numPop, denPop));

    R4MeasureReportScorer scorer = new R4MeasureReportScorer();

    // Execute
    StratumPopulationDef result = scorer.findStratumPopulationDef(
        stratumDef, MeasurePopulationType.NUMERATOR);

    // Verify
    assertSame(numPop, result);
}

@Test
void testFindStratumPopulationDef_NotFound() {
    // Setup
    StratumDef stratumDef = mock(StratumDef.class);
    when(stratumDef.stratumPopulations()).thenReturn(java.util.List.of());

    R4MeasureReportScorer scorer = new R4MeasureReportScorer();

    // Execute
    StratumPopulationDef result = scorer.findStratumPopulationDef(
        stratumDef, MeasurePopulationType.NUMERATOR);

    // Verify
    assertNull(result);
}

@Test
void testFindStratumPopulationDef_NullStratumDef() {
    // Setup
    R4MeasureReportScorer scorer = new R4MeasureReportScorer();

    // Execute
    StratumPopulationDef result = scorer.findStratumPopulationDef(
        null, MeasurePopulationType.NUMERATOR);

    // Verify
    assertNull(result);
}
```

### Phase 3: Integration Tests

**Verify existing tests still pass:**

```bash
# Run all measure scorer tests
mvn test -pl cqf-fhir-cr -Dtest=*MeasureScorerTest

# Run proportion measure tests
mvn test -pl cqf-fhir-cr -Dtest=MeasureScoringTypeProportionTest

# Run ratio measure tests
mvn test -pl cqf-fhir-cr -Dtest=MeasureScoringTypeRatioTest

# Run stratifier tests
mvn test -pl cqf-fhir-cr -Dtest=*StratifierTest
```

**Expected Results:**
- All existing tests should pass without modification
- MeasureReport output should be identical (same counts, same scores)
- Only implementation detail has changed (data source for counts)

## Validation Gates

### 1. Code Style & Formatting
```bash
mvn spotless:apply
mvn checkstyle:check
```

### 2. Unit Tests
```bash
# Run new PopulationDef count tests
mvn test -pl cqf-fhir-cr -Dtest=PopulationDefCountTest

# Run scorer tests with new methods
mvn test -pl cqf-fhir-cr -Dtest=R4MeasureReportScorerTest
```

### 3. Integration Tests
```bash
# Run all measure-related integration tests
mvn test -pl cqf-fhir-cr -Dtest=*Measure*
```

### 4. Verification Checklist
- [ ] `PopulationDef.getCount(GroupDef)` method added and tested
- [ ] `R4MeasureReportScorer.findStratumPopulationDef()` helper method added
- [ ] `scoreGroup()` refactored to use `PopulationDef.getCount()`
- [ ] `getStratumScoreOrNull()` refactored to use `StratumPopulationDef.getCount()`
- [ ] Old methods deprecated with clear migration path
- [ ] All existing tests pass without modification
- [ ] New unit tests for `PopulationDef.getCount()` pass
- [ ] New unit tests for `findStratumPopulationDef()` pass
- [ ] MeasureReport output is unchanged
- [ ] Code follows existing patterns and style

## Risk Assessment

### Low Risk Areas
- Adding `PopulationDef.getCount()` method (new functionality, no breaking changes)
- Adding `findStratumPopulationDef()` helper (new internal method)
- Deprecating old methods (backward compatible)

### Medium Risk Areas
- Refactoring `scoreGroup()` count retrieval
  - **Mitigation**: Logic is identical, only data source changes
  - **Mitigation**: Comprehensive existing test coverage for proportion/ratio scoring
- Refactoring `getStratumScoreOrNull()` count retrieval
  - **Mitigation**: `StratumPopulationDef.getCount()` already exists and is used elsewhere
  - **Mitigation**: Existing stratifier tests validate behavior

### Testing Strategy
1. **Unit Tests First**: Test new `PopulationDef.getCount()` method with all population basis types
2. **Integration Tests**: Run existing proportion/ratio/stratifier tests to verify no regression
3. **Manual Verification**: Compare MeasureReport output before and after refactoring

## Success Criteria

### Part 1: QuantityDef Simplification
1. ✅ `QuantityDef` only has `value` field (no unit/system/code)
2. ✅ R4 and DSTU3 converters don't extract or set unit/system/code
3. ✅ `R4MeasureReportScorer.aggregate()` doesn't preserve unit/system/code
4. ✅ All test assertions for unit/system/code are removed
5. ✅ `ContinuousVariableObservationConverter` interface only has `convertToFhirQuantity()` method
6. ✅ `extractQuantityDef()` and `wrapResultAsQuantity()` methods are **removed** from interface and implementations
7. ✅ `convertCqlResultToQuantityDef()` private method added to `ContinuousVariableObservationHandler`
8. ✅ Only 3-4 tests remain in R4/DSTU3 converter test files (dead code tests deleted)
9. ✅ Continuous variable measure tests still pass (only value matters for scoring)

### Part 2: Count Retrieval Refactoring
6. ✅ `PopulationDef.getCount(GroupDef)` method exists and handles all population basis types
7. ✅ `R4MeasureReportScorer.findStratumPopulationDef()` helper method exists
8. ✅ `scoreGroup()` retrieves counts from `PopulationDef` instead of FHIR components
9. ✅ `getStratumScoreOrNull()` retrieves counts from `StratumPopulationDef` instead of FHIR components
10. ✅ Old methods `getCountFromGroupPopulation()` and `getCountFromStratifierPopulation()` are **removed** (not deprecated)
11. ✅ New unit tests for `PopulationDef.getCount()` pass
12. ✅ All existing proportion/ratio/stratifier tests pass

### Overall
13. ✅ MeasureReport output is unchanged (same counts, same scores)
14. ✅ Code is properly formatted (spotless:apply, checkstyle:check)

## Architecture Benefits

### Before This Refactoring:
```
┌─────────────┐     calculates    ┌───────────────────────┐
│ PopulationDef│─────count────────▶│ MeasureReport         │
└─────────────┘                    │ GroupPopulation       │
                                   │ Component.count       │
                                   └───────────┬───────────┘
                                               │
┌─────────────────────┐       reads count     │
│ R4MeasureReportScorer│◀──────back from──────┘
│ .scoreGroup()        │       FHIR object
└─────────────────────┘
```

**Problems:**
- Redundant round-trip through FHIR objects
- Scorer depends on report being populated
- Count logic duplicated in builder and scorer
- Cannot test scoring without building full reports

### After This Refactoring:
```
┌─────────────┐
│ PopulationDef│─────────┬─────────▶ R4MeasureReportBuilder
│ .getCount()  │         │           .buildPopulation()
└─────────────┘         │           (sets FHIR component)
                        │
                        └─────────▶ R4MeasureReportScorer
                                    .scoreGroup()
                                    (calculates score)
```

**Benefits:**
- Single source of truth: `PopulationDef.getCount(GroupDef)`
- No dependency on FHIR report structure for scoring
- Eliminated code duplication
- Better testability: can test scoring with just `PopulationDef`
- Clearer separation of concerns:
  - **PopulationDef**: Stores and provides population data
  - **R4MeasureReportBuilder**: Populates FHIR representation
  - **R4MeasureReportScorer**: Calculates scores from data

### Code Quality Improvements

**Before - 5 separate calls to extract counts:**
```java
score = calcProportionScore(
    getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR)
        - getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR_EXCLUSION),
    getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR)
        - getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCLUSION)
        - getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCEPTION));
```

**After - Clear variable names and direct data access:**
```java
PopulationDef numPop = groupDef.findPopulationByType(NUMERATOR);
PopulationDef numExPop = groupDef.findPopulationByType(NUMERATOR_EXCLUSION);
PopulationDef denPop = groupDef.findPopulationByType(DENOMINATOR);
PopulationDef denExPop = groupDef.findPopulationByType(DENOMINATOR_EXCLUSION);
PopulationDef denExcPop = groupDef.findPopulationByType(DENOMINATOR_EXCEPTION);

int numCount = numPop != null ? numPop.getCount(groupDef) : 0;
int numExCount = numExPop != null ? numExPop.getCount(groupDef) : 0;
int denCount = denPop != null ? denPop.getCount(groupDef) : 0;
int denExCount = denExPop != null ? denExPop.getCount(groupDef) : 0;
int denExcCount = denExcPop != null ? denExcPop.getCount(groupDef) : 0;

score = calcProportionScore(numCount - numExCount, denCount - denExCount - denExcCount);
```

**Improvements:**
- More readable: clear variable names show intent
- More maintainable: easy to add logging or validation
- More testable: can mock individual PopulationDef instances
- More efficient: no repeated list filtering of FHIR components

## Implementation Checklist

### Part 1: Simplify QuantityDef
- [ ] **Step 0.1**: Update `QuantityDef` class - remove `unit`, `system`, `code` fields and methods
- [ ] **Step 0.2**: Update `R4ContinuousVariableObservationConverter.extractQuantityDef()` - remove unit/system/code extraction
- [ ] **Step 0.2**: Update `R4ContinuousVariableObservationConverter.convertToFhirQuantity()` - remove unit/system/code setting
- [ ] **Step 0.3**: Update `Dstu3ContinuousVariableObservationConverter.extractQuantityDef()` - remove unit/system/code extraction
- [ ] **Step 0.3**: Update `Dstu3ContinuousVariableObservationConverter.convertToFhirQuantity()` - remove unit/system/code setting
- [ ] **Step 0.4**: Update `R4MeasureReportScorer.aggregate()` - remove unit/system/code preservation logic
- [ ] **Step 0.5**: Update `QuantityDefTest.java` - remove all unit/system/code assertions
- [ ] **Step 0.5**: Update `R4ContinuousVariableObservationConverterTest.java` - remove all unit/system/code assertions
- [ ] **Step 0.5**: Update `Dstu3ContinuousVariableObservationConverterTest.java` - remove all unit/system/code assertions
- [ ] **Step 0.6**: Find and update any test helper methods creating QuantityDef with 4 parameters
- [ ] **Step 0.7**: Simplify `ContinuousVariableObservationConverter` interface - remove `wrapResultAsQuantity()` and `extractQuantityDef()`
- [ ] **Step 0.8**: Simplify `R4ContinuousVariableObservationConverter` - remove `extractQuantityDef()` method (dead code)
- [ ] **Step 0.9**: Simplify `Dstu3ContinuousVariableObservationConverter` - remove `extractQuantityDef()` method (dead code)
- [ ] **Step 0.10**: Add `convertCqlResultToQuantityDef()` private method to `ContinuousVariableObservationHandler`
- [ ] **Step 0.10**: Update call site in `ContinuousVariableObservationHandler` line 144 to use new method
- [ ] **Step 0.11**: Delete 8 tests from `R4ContinuousVariableObservationConverterTest.java` (only keep 3-4 tests for `convertToFhirQuantity`)
- [ ] **Step 0.11**: Delete 8 tests from `Dstu3ContinuousVariableObservationConverterTest.java` (only keep 3-4 tests for `convertToFhirQuantity`)

### Part 2: Refactor Count Retrieval
- [ ] Add `PopulationDef.getCount(GroupDef)` method
- [ ] Add `R4MeasureReportScorer.findStratumPopulationDef()` helper method
- [ ] Refactor `scoreGroup()` to use `PopulationDef.getCount()`
- [ ] Refactor `getStratumScoreOrNull()` to use `StratumPopulationDef.getCount()`
- [ ] **Remove** (not deprecate) `getCountFromGroupPopulation()` and `getCountFromStratifierPopulation()`
- [ ] (Optional) Refactor `R4MeasureReportBuilder.buildPopulation()` to use `PopulationDef.getCount()`
- [ ] Add unit tests for `PopulationDef.getCount()`
- [ ] Add unit tests for `findStratumPopulationDef()`

### Validation
- [ ] Run all validation gates (format, style, tests)
- [ ] Verify MeasureReport output is unchanged
- [ ] Verify continuous variable scoring still works (integration tests)
- [ ] Code review and merge

## Confidence Score

**9/10** - Very high confidence for one-pass implementation success

### Strengths:
- Clear refactoring with well-defined scope
- Strong existing test coverage for scoring logic
- `StratumPopulationDef.getCount()` already exists and demonstrates the pattern
- Logic is simple and well-understood (just moving data source)
- No changes to calculation algorithms
- Backward compatible (old methods deprecated, not removed)

### Potential Challenges:
- Need to verify `GroupDef.findPopulationByType()` handles all edge cases correctly
- Must ensure null handling is consistent between old and new approach
- Stratifier scoring has complex logic - need careful testing

### Mitigation:
- Comprehensive unit tests for new `PopulationDef.getCount()` method
- Existing integration tests validate end-to-end behavior
- Deprecated methods remain available during transition period
- Logic is identical, only data source changes

## Future Work

This refactoring is a stepping stone toward larger architectural improvements:

1. **Extract Scoring Logic**: Move proportion/ratio calculation logic to version-agnostic classes
2. **Decouple Scorer from MeasureReport**: Make `score()` method work with `MeasureDef` and return scores without requiring `MeasureReport`
3. **Unified Scoring Interface**: Create common scoring interface for R4, DSTU3, and R5
4. **Testability**: Enable unit testing of scoring logic without building full FHIR structures

## References

### Related Code
- `PopulationDef.java` - Target for new `getCount()` method
- `StratumPopulationDef.java:40-53` - Existing `getCount()` implementation to follow
- `R4MeasureReportBuilder.java:268-277` - Current count calculation logic to centralize
- `R4StratifierBuilder.java:286` - Good example of direct Def access pattern

### Related PRs
- PR #839: "Reduce FHIR-centric logic: Improve measure evaluation code reuse among FHIR versions"
- PR #843: "Refactor: Introduce QuantityDef for FHIR-Version-Agnostic Measure Scoring"
  - This PRP continues the pattern of reducing FHIR coupling in scoring logic

### FHIR Specifications
- FHIR Measure Scoring: https://www.hl7.org/fhir/measure-definitions.html#Measure.scoring
- MeasureReport: https://www.hl7.org/fhir/measurereport.html
