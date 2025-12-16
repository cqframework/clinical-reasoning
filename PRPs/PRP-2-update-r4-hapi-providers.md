# PRP-2: Update R4 HAPI Providers and Spring Config

**Phase**: 2 - Unified Service Architecture (R4)
**Dependencies**: PRP-1
**Estimated Size**: Small (50-100 lines)
**Estimated Time**: 0.5-1 day
**Complexity**: Low (straightforward wiring)

---

## Goal

Wire the new R4UnifiedMeasureService into HAPI FHIR operation providers and Spring configuration.

---

## Files to Modify

### 1. MeasureOperationsProvider.java

- Keep both `$evaluate-measure` and `$evaluate` operations
- Both delegate to R4UnifiedMeasureService

### 2. CrR4Config.java

- Remove separate factories
- Add single `r4UnifiedMeasureServiceFactory()` bean

### 3. Create R4UnifiedMeasureEvaluatorFactory.java

---

## Success Criteria

✅ HAPI operations wired to R4UnifiedMeasureService
✅ Spring config creates unified service factory
✅ Both operations work
