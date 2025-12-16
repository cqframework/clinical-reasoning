# PRP-0C: Update MeasureEvaluator to Use MeasureReportDef

**Phase**: 1 - MeasureDef/MeasureReportDef Separation (Foundation)
**Dependencies**: PRP-0A, PRP-0B
**Estimated Size**: Medium (300-400 lines)
**Estimated Time**: 2-3 days
**Complexity**: Medium (threading MeasureReportDef through call chains)

---

## Goal

Change MeasureEvaluator from mutating a MeasureDef to building and returning a MeasureReportDef.

---

## Key Changes

- **MeasureEvaluator.evaluateCriteria()** returns MeasureReportDef instead of void
- **R4MeasureProcessor.evaluateMeasureCaptureDefs()** works with MeasureReportDef
- **MeasureDefAndR4MeasureReport** contains MeasureReportDef instead of MeasureDef
- **R4MeasureReportBuilder** builds from MeasureReportDef
- **R4MeasureReportScorer** scores MeasureReportDef

---

## Files to Modify

### 1. MeasureEvaluator.java

Change method signature:

```java
// BEFORE:
void evaluateCriteria(
    CqlEngine context,
    MeasureDef measureDef,
    Iterable<String> subjectIds,
    MeasureEvalType measureEvalType);

// AFTER:
MeasureReportDef evaluateCriteria(
    CqlEngine context,
    MeasureDef measureDef,
    Iterable<String> subjectIds,
    MeasureEvalType measureEvalType);
```

Implementation:
- Create new `MeasureReportDef reportDef = new MeasureReportDef(measureDef)`
- For each GroupDef in measureDef, create GroupReportDef
- For each PopulationDef in groupDef, create PopulationReportDef
- Populate report classes with evaluation results (instead of mutating Def classes)
- Return reportDef

### 2. R4MeasureProcessor.java

Update `evaluateMeasureCaptureDefs()`:

```java
// BEFORE:
public MeasureDefAndR4MeasureReport evaluateMeasureCaptureDefs(...) {
    MeasureDef measureDef = buildMeasureDef(measure);
    measureEvaluator.evaluateCriteria(..., measureDef, ...);  // mutates
    MeasureReport report = buildReport(measureDef);
    return new MeasureDefAndR4MeasureReport(measureDef, report);
}

// AFTER:
public MeasureDefAndR4MeasureReport evaluateMeasureCaptureDefs(...) {
    MeasureDef measureDef = buildMeasureDef(measure);  // immutable
    MeasureReportDef reportDef = measureEvaluator.evaluateCriteria(..., measureDef, ...);  // returns new
    MeasureReport report = buildReport(reportDef);
    return new MeasureDefAndR4MeasureReport(reportDef, report);
}
```

### 3. MeasureDefAndR4MeasureReport.java

Change to accept MeasureReportDef:

```java
// BEFORE:
@VisibleForTesting
public record MeasureDefAndR4MeasureReport(MeasureDef measureDef, MeasureReport measureReport) {}

// AFTER:
@VisibleForTesting
public record MeasureDefAndR4MeasureReport(MeasureReportDef measureReportDef, MeasureReport measureReport) {
    // Convenience getter for tests that need MeasureDef
    public MeasureDef measureDef() {
        return measureReportDef.measureDef();
    }
}
```

### 4. R4MeasureReportBuilder.java

Update to build from MeasureReportDef:

```java
// BEFORE:
public MeasureReport build(MeasureDef measureDef, ...) {
    // Read populations, scores from measureDef
}

// AFTER:
public MeasureReport build(MeasureReportDef measureReportDef, ...) {
    MeasureDef measureDef = measureReportDef.measureDef();
    // Read structure from measureDef
    // Read evaluation results from measureReportDef
}
```

### 5. R4MeasureReportScorer.java

Update to score MeasureReportDef:

```java
// BEFORE:
public void score(MeasureDef measureDef, MeasureReport report) {
    // Read counts, compute scores, mutate measureDef
}

// AFTER:
public void score(MeasureReportDef measureReportDef, MeasureReport report) {
    // Read counts, compute scores, mutate measureReportDef
}
```

### 6. Similar updates for DSTU3

- Dstu3MeasureProcessor.java
- Dstu3MeasureReportBuilder.java (if exists)
- MeasureDefAndDstu3MeasureReport.java

---

## Implementation Strategy

1. **Start with MeasureEvaluator** - change return type
2. **Update R4MeasureProcessor** - handle returned MeasureReportDef
3. **Update MeasureDefAndR4MeasureReport** - change field type
4. **Update builders and scorers** - read from MeasureReportDef
5. **Fix all compilation errors** - thread MeasureReportDef through
6. **Run tests** - ensure all pass

---

## Success Criteria

✅ MeasureEvaluator returns MeasureReportDef instead of void
✅ All callers updated to handle MeasureReportDef
✅ MeasureDef remains immutable throughout evaluation
✅ MeasureReportDef is mutated during evaluation
✅ Report builders read from MeasureReportDef
✅ Scorers write to MeasureReportDef
✅ All tests compile and pass
✅ Code formatting passes (`./mvnw spotless:check`)

---

## Testing

- **Unit tests**: Update to work with MeasureReportDef
- **Integration tests**: Should all pass after this PRP
- **Next PRP**: PRP-0D will update MeasureMultiSubjectEvaluator

---

## Notes

- This is the PRP that "connects the dots" and makes everything work
- All compilation failures from PRP-0B should be fixed
- Tests should pass after this PRP
- MeasureDef is now truly immutable, MeasureReportDef holds evaluation state
