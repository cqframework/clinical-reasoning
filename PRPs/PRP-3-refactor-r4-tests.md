# PRP-3: Refactor R4 Tests to Use R4UnifiedMeasureService

**Phase**: 2 - Unified Service Architecture (R4)
**Dependencies**: PRP-2
**Estimated Size**: Medium (200-300 lines)
**Estimated Time**: 2-3 days
**Complexity**: Medium (test migration requires care)

---

## Goal

Update test infrastructure to use R4UnifiedMeasureService and migrate all integration tests.

---

## Files to Modify

### 1. MultiMeasure.java (test builder)

- Update `evaluate()` to instantiate R4UnifiedMeasureService
- Add convenience methods for single-measure tests

### 2. Measure.java (legacy test builder)

- Mark as deprecated
- Update to use R4UnifiedMeasureService.evaluateSingle()

---

## Success Criteria

✅ MultiMeasure.java uses R4UnifiedMeasureService
✅ All tests pass
