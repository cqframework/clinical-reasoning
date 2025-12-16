# PRP-5: Update DSTU3 HAPI Providers and Tests

**Phase**: 3 - Unified Service Architecture (DSTU3)
**Dependencies**: PRP-4
**Estimated Size**: Medium (150-200 lines)
**Estimated Time**: 1-2 days
**Complexity**: Medium

---

## Goal

Wire Dstu3UnifiedMeasureService into HAPI providers and create test infrastructure.

---

## Files to Modify

### 1. DSTU3 HAPI Operation Provider

Update to use Dstu3UnifiedMeasureService

### 2. DSTU3 Spring Config

Add Dstu3UnifiedMeasureServiceFactory bean

---

## Success Criteria

✅ DSTU3 operations wired
✅ Tests pass
