# PRP-0D: Update MeasureMultiSubjectEvaluator

**Phase**: 1 - MeasureDef/MeasureReportDef Separation (Foundation)
**Dependencies**: PRP-0C
**Estimated Size**: Small (100-150 lines)
**Estimated Time**: 0.5-1 day
**Complexity**: Low (straightforward parameter change)

---

## Goal

Update MeasureMultiSubjectEvaluator to mutate MeasureReportDef instead of MeasureDef.

---

## Files to Modify

### 1. MeasureMultiSubjectEvaluator.java

```java
// BEFORE:
void multiSubjectEvaluation(
    MeasureDef measureDef,
    MeasureEvalType measureEvalType);

// AFTER:
void multiSubjectEvaluation(
    MeasureReportDef measureReportDef,
    MeasureEvalType measureEvalType);
```

### 2. All callers

- R4MeasureProcessor.java
- Dstu3MeasureProcessor.java

---

## Success Criteria

✅ MeasureMultiSubjectEvaluator operates on MeasureReportDef
✅ All callers updated
✅ Tests pass
