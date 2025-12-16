# PRP Index: Unified Measure Service Architecture with MeasureDef/MeasureReportDef Separation

## Overview

This implementation plan refactors the measure evaluation architecture with two major changes:
1. **Separate MeasureDef (immutable) from MeasureReportDef (mutable)** - Clean separation between FHIR Measure definition and evaluation results
2. **Unified services** - Handle both single and multi-measure evaluation through a clean, layered API design

The work is broken into **10+ independent PRPs** organized into 4 phases.

## Key Design Principles

✅ **Immutable MeasureDef** - Created by builders, frozen after construction, represents FHIR Measure structure only
✅ **Mutable MeasureReportDef** - Contains MeasureDef + evaluation state (strata, scores, populations)
✅ **Clear separation** - MeasureDef = what to evaluate, MeasureReportDef = evaluation results
✅ **Thin single-measure layer** - Single measure methods wrap List.of(measure) and delegate to multi-measure
✅ **Multi-measure as foundation** - All real logic in multi-measure methods
✅ **R4 first, then DSTU3** - Prove pattern in R4 before porting to DSTU3
✅ **Independent PRPs** - Each phase can be developed and merged separately
✅ **Backward compatibility during transition** - Keep old services working until all callers migrate

---

## Phase 1: MeasureDef/MeasureReportDef Separation (Foundation)

| PRP | Title | File | Dependencies | Size |
|-----|-------|------|--------------|------|
| **PRP-0A** | Create MeasureReportDef and composed classes | [PRP-0A-create-measure-report-def.md](PRP-0A-create-measure-report-def.md) | None | Large (800-1000 lines) |
| **PRP-0B** | Refactor MeasureDef to immutable | [PRP-0B-refactor-measure-def-immutable.md](PRP-0B-refactor-measure-def-immutable.md) | PRP-0A | Large (400-500 lines) |
| **PRP-0C** | Update MeasureEvaluator to use MeasureReportDef | [PRP-0C-update-measure-evaluator.md](PRP-0C-update-measure-evaluator.md) | PRP-0A, PRP-0B | Medium (300-400 lines) |
| **PRP-0D** | Update MeasureMultiSubjectEvaluator | [PRP-0D-update-multi-subject-evaluator.md](PRP-0D-update-multi-subject-evaluator.md) | PRP-0C | Small (100-150 lines) |
| **PRP-0E** | Update test frameworks for Def/ReportDef assertions | [PRP-0E-update-test-frameworks.md](PRP-0E-update-test-frameworks.md) | PRP-0C, PRP-0D | Medium (200-300 lines) |

**Phase 1 Goals:**
- Separate immutable measure structure (MeasureDef) from mutable evaluation results (MeasureReportDef)
- MeasureEvaluator returns MeasureReportDef instead of mutating MeasureDef
- Test frameworks support assertions on both structure and results
- All existing tests pass with new architecture

---

## Phase 2: Unified Service Architecture (R4)

| PRP | Title | File | Dependencies | Size |
|-----|-------|------|--------------|------|
| **PRP-1** | Create R4UnifiedMeasureService (core implementation) | [PRP-1-create-r4-unified-service.md](PRP-1-create-r4-unified-service.md) | PRP-0E | Large (400-500 lines) |
| **PRP-2** | Update R4 HAPI providers and Spring config | [PRP-2-update-r4-hapi-providers.md](PRP-2-update-r4-hapi-providers.md) | PRP-1 | Small (50-100 lines) |
| **PRP-3** | Refactor R4 tests to use R4UnifiedMeasureService | [PRP-3-refactor-r4-tests.md](PRP-3-refactor-r4-tests.md) | PRP-2 | Medium (200-300 lines) |

**Phase 2 Goals:**
- Create unified R4 service that handles both single and multi-measure evaluation
- Single-measure methods are thin wrappers (< 20 lines) over multi-measure
- Wire into HAPI FHIR operation providers
- Migrate all R4 tests to new service
- Old R4 services marked deprecated but remain functional

---

## Phase 3: Unified Service Architecture (DSTU3)

| PRP | Title | File | Dependencies | Size |
|-----|-------|------|--------------|------|
| **PRP-4** | Create Dstu3UnifiedMeasureService | [PRP-4-create-dstu3-unified-service.md](PRP-4-create-dstu3-unified-service.md) | PRP-1 (reference) | Large (400-500 lines) |
| **PRP-5** | Update DSTU3 HAPI providers and tests | [PRP-5-update-dstu3-hapi-providers.md](PRP-5-update-dstu3-hapi-providers.md) | PRP-4 | Medium (150-200 lines) |

**Phase 3 Goals:**
- Port R4 unified service pattern to DSTU3
- Add multi-measure capability to DSTU3 (currently only single-measure exists)
- Wire into DSTU3 HAPI providers
- Create DSTU3 test infrastructure mirroring R4
- Old DSTU3 service marked deprecated but remains functional

---

## Phase 4: Workflow Separation

| PRP | Title | File | Dependencies | Size |
|-----|-------|------|--------------|------|
| **PRP-6** | Implement workflow separation | [PRP-6-implement-workflow-separation.md](PRP-6-implement-workflow-separation.md) | PRP-3, PRP-5 | Medium (200-300 lines) |

**Phase 4 Goals:**
- Split evaluation into two self-contained workflows:
  - **Workflow 1**: Entry → Populated MeasureReportDef (CQL evaluation, population)
  - **Workflow 2**: Populated MeasureReportDef → MeasureReport/Parameters (report building, scoring)
- Add workflow methods to both R4 and DSTU3 unified services
- Clean workflow boundary (no CQL evaluation in Workflow 2)
- Refactor existing evaluate methods to use workflows internally

---

## Optional Cleanup PRPs (Future)

| PRP | Title | Dependencies |
|-----|-------|--------------|
| **PRP-7** | Remove deprecated R4 services | PRP-6 |
| **PRP-8** | Remove deprecated DSTU3 services | PRP-6 |
| **PRP-9** | External migration documentation | PRP-6 |

**Cleanup Goals:**
- Remove R4MeasureService and R4MultiMeasureService after migration complete
- Remove Dstu3MeasureService after migration complete
- Provide migration guide for external callers (cqis/main, jpa-server-starter)

---

## Implementation Order Recommendation

**Recommended sequence:**

1. **Phase 1** (PRPs 0A → 0B → 0C → 0D → 0E): Foundation - MeasureDef/MeasureReportDef separation
2. **Phase 2** (PRPs 1 → 2 → 3): R4 Unified Service
3. **Phase 3** (PRPs 4 → 5): DSTU3 Unified Service
4. **Phase 4** (PRP 6): Workflow Separation
5. **Optional** (PRPs 7, 8, 9): Cleanup and documentation

**Rationale**:
- Phase 1 establishes the immutable/mutable separation foundation that all other work builds on
- R4 first allows proving the unified service pattern before porting to DSTU3
- Each PRP can be reviewed and merged independently
- Workflow separation comes last after services are stable

**Alternative sequence** (if parallel development possible):
- Team A: Phase 1 (all team members collaborate)
- Team A: Phase 2 (PRPs 1, 2, 3) - R4
- Team B: Phase 3 (PRPs 4, 5) - DSTU3 (waits for PRP-1 as reference)
- Either: Phase 4 (PRP 6) - Workflow separation

---

## Success Criteria (Overall)

✅ MeasureDef is fully immutable (structure only)
✅ MeasureReportDef contains MeasureDef + evaluation results
✅ MeasureEvaluator returns MeasureReportDef instead of void
✅ R4UnifiedMeasureService handles single + multi measure evaluation
✅ Dstu3UnifiedMeasureService handles single + multi measure evaluation
✅ Single-measure methods are thin wrappers (< 20 lines)
✅ Multi-measure methods contain all core logic
✅ Both HAPI operations work (`$evaluate-measure` and `$evaluate`)
✅ All R4 tests pass
✅ All DSTU3 tests pass
✅ Workflow separation implemented
✅ Old services deprecated but functional
✅ Test frameworks support both measureDef() and reportDef() assertions
✅ Code formatting passes (`./mvnw spotless:check`)

---

## Non-Goals

❌ Changing CQL evaluation logic
❌ Optimizing performance
❌ Adding R5 support
❌ Updating cqis/main code directly (documentation only)
❌ Changing scoring algorithms
❌ Renaming unified services (can be done later)
