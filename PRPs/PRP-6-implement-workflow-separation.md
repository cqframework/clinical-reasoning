# PRP-6: Implement Workflow Separation

**Phase**: 4 - Workflow Separation
**Dependencies**: PRP-3, PRP-5
**Estimated Size**: Medium (200-300 lines)
**Estimated Time**: 1-2 days
**Complexity**: Medium

---

## Goal

Add workflow separation methods to both R4UnifiedMeasureService and Dstu3UnifiedMeasureService, splitting evaluation into two self-contained workflows.

---

## Workflow Architecture

**Workflow 1**: Entry → Populated MeasureReportDef
- Input: Measure IDs, period, subjects, etc.
- Output: `MeasureDefWithEvaluationResults`
- Includes: CQL evaluation, MeasureDef building, population

**Workflow 2**: Populated MeasureReportDef → MeasureReport/Parameters
- Input: `MeasureDefWithEvaluationResults`
- Output: `MeasureReport` or `Parameters`
- Includes: Report building, scoring

---

## Files to Create

### MeasureDefWithEvaluationResults.java

```java
public record MeasureDefWithEvaluationResults(
    MeasureDef measureDef,
    Interval measurementPeriod,
    List<String> subjectIds,
    String reportType,
    MeasureEvalType evalType) {}
```

---

## Files to Modify

### 1. R4UnifiedMeasureService.java

Add workflow methods:
- `evaluateToMeasureDefs()`
- `buildParametersFromMeasureDefs()`

### 2. Dstu3UnifiedMeasureService.java

Add same workflow methods

---

## Success Criteria

✅ MeasureDefWithEvaluationResults record created
✅ Workflow 1 and 2 methods added
✅ All tests pass
✅ Clean workflow boundary
