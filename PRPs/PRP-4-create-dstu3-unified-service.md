# PRP-4: Create Dstu3UnifiedMeasureService

**Phase**: 3 - Unified Service Architecture (DSTU3)
**Dependencies**: PRP-1 (reference)
**Estimated Size**: Large (400-500 lines)
**Estimated Time**: 2-3 days
**Complexity**: Medium (port from R4)

---

## Goal

Port the R4 unified service pattern to DSTU3, creating Dstu3UnifiedMeasureService with single and multi-measure capability.

---

## Files to Create

### 1. Dstu3UnifiedMeasureService.java

Mirror R4UnifiedMeasureService structure

### 2. Dstu3UnifiedMeasureEvaluator.java

Combined interface for single + multi

---

## Key DSTU3 vs R4 Differences

| Aspect | DSTU3 | R4 |
|--------|-------|-----|
| Date handling | String | ZonedDateTime |
| Measure lookup | IdType only | Either3 |
| Stratifiers | Simple | Complex |

---

## Success Criteria

✅ Dstu3UnifiedMeasureService compiles
✅ Mirrors R4 structure
