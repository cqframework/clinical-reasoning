# PRP-0E: Update Test Frameworks for Def/ReportDef Assertions

**Phase**: 1 - MeasureDef/MeasureReportDef Separation (Foundation)
**Dependencies**: PRP-0C, PRP-0D
**Estimated Size**: Medium (200-300 lines)
**Estimated Time**: 1-2 days
**Complexity**: Medium (test framework updates)

---

## Goal

Update test infrastructure to support assertions on both MeasureDef (structure) and MeasureReportDef (evaluation results).

---

## Files to Modify

### 1. MeasureDefAndR4MeasureReport.java

```java
@VisibleForTesting
public record MeasureDefAndR4MeasureReport(
        MeasureReportDef measureReportDef,
        MeasureReport measureReport) {

    public MeasureDef measureDef() {
        return measureReportDef.measureDef();
    }

    public MeasureReportDef reportDef() {
        return measureReportDef;
    }
}
```

### 2. Test assertion builders

- Update Measure.java and MultiMeasure.java test builders
- Support both `.measureDef()` and `.reportDef()` assertions

---

## Success Criteria

✅ Test frameworks support both measureDef() and reportDef()
✅ All integration tests pass
✅ Test code is clear about structure vs results
