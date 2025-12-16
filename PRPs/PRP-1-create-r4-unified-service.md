# PRP-1: Create R4UnifiedMeasureService (Core Implementation)

**Phase**: 2 - Unified Service Architecture (R4)
**Dependencies**: PRP-0E
**Estimated Size**: Large (400-500 lines)
**Estimated Time**: 1-2 days
**Complexity**: Medium (mostly copy-paste from existing services)

---

## Goal

Create new R4UnifiedMeasureService that provides both single-measure and multi-measure evaluation through a unified API, with single-measure as a thin wrapper over multi-measure.

---

## Files to Create

### 1. R4UnifiedMeasureService.java

Location: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4UnifiedMeasureService.java`

Implements: Both `R4MeasureEvaluatorSingle` and `R4MeasureEvaluatorMultiple` interfaces

### 2. R4UnifiedMeasureEvaluator.java

Location: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4UnifiedMeasureEvaluator.java`

Combined interface for single + multi evaluation.

---

## Key Design Principles

- Single-measure methods are **thin wrappers** (< 20 lines)
- Multi-measure methods contain **all real logic**
- Copy logic from R4MultiMeasureService.evaluate()

---

## Success Criteria

✅ R4UnifiedMeasureService class compiles
✅ Implements both single and multi interfaces
✅ Single methods are thin wrappers (< 20 lines each)
✅ Multi methods contain all real logic
