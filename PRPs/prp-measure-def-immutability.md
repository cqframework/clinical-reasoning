# PRP: Measure Def Immutability & Architecture Alignment

## Status: Complete

All phases implemented. Legacy measure processors deleted. All production measure evaluation
flows through the shared `MeasureEvaluationService` pipeline.

## Completed Work

### Phase 1: Def Types Made Immutable (PopulationDef, GroupDef, SdeDef)
- **PopulationDef**: Removed 3 mutable fields (`subjectResources`, `evaluatedResources`, `aggregationResult`) and 16 mutation/query methods. All callers migrated to `MeasureEvaluationState.PopulationState`.
- **GroupDef**: Removed `score` field, `getScore()`, `setScoreAndAdaptToImprovementNotation()`, `getPopulationCount()`. Score is now exclusively in `MeasureEvaluationState.GroupState`.
- **SdeDef**: Removed `results` map, `putResult()`, `getResults()`. Results now exclusively in `MeasureEvaluationState.SdeState`.
- **R4MeasureReportUtils**: Removed convenience overload that read from mutable PopulationDef.
- **EvaluationResultFormatter**: Changed `printSubjectResources` to accept `PopulationState`.
- **6 test files updated**: All mutable Def calls migrated to state-based equivalents.

### Phase 1b: StratumDef/StratumPopulationDef Documentation
- StratumDef and StratumPopulationDef retain mutable fields (`score`, `aggregationResult`).
- These are **ephemeral evaluation artifacts**, NOT part of the immutable MeasureDef tree. They're created per-evaluation by `MeasureMultiSubjectEvaluator`, stored in `StratifierState.strata`, scored by `MeasureReportDefScorer`, and read by report builders.
- Updated Javadoc on both classes to clarify lifecycle and why mutable fields are acceptable.

### Phase 2a: R4CollectDataService Migrated to MeasureEvaluationService
- **R4CollectDataService**: Complete rewrite. Uses `R4MeasureResolver` + `MeasureEvaluationService` instead of legacy `R4MeasureProcessor`. Constructor now takes `MeasurePeriodValidator`.
- **CrR4Config**: Updated factory to inject `MeasurePeriodValidator`.
- **CollectData test fixture**: Updated constructor call.

### Phase 2b: Legacy Processors Deleted
- **R4MeasureProcessor** (552 lines): Deleted. Zero production callers after R4CollectDataService migration.
- **Dstu3MeasureProcessor** (308 lines): Deleted. Zero production callers after Dstu3MeasureService uses Dstu3MeasureResolver.
- **R4MeasureProcessorTest** (185 lines): Deleted. Tested dead code.
- **Dstu3MeasureProcessorTest** (49 lines): Deleted. Tested dead code.
- **DSTU3 Measure.java test fixture**: Migrated from `Dstu3MeasureProcessor.evaluateMeasureCaptureDefs()` to `Dstu3MeasureResolver` + `MeasureEvaluationService` + `Dstu3MeasureReportBuilder`.

### Phase 3: Extracted Measure Resolvers
- **R4MeasureResolver**: New class with `buildResolvedMeasure()`, `resolveParameterMap()`, `evalTypeToReportType()`. Extracted from R4MeasureProcessor.
- **Dstu3MeasureResolver**: Same pattern for DSTU3.
- **R4MultiMeasureService**: Now uses `R4MeasureResolver` instead of `R4MeasureProcessor`.
- **Dstu3MeasureService**: Now uses `Dstu3MeasureResolver` instead of `Dstu3MeasureProcessor`.

### Phase 4: Dead Code Deleted
- **R4MeasureOperationMapper**: Deleted. Complete implementation but never instantiated.
- **Dstu3MeasureOperationMapper**: Deleted. Complete implementation but never instantiated.
- **MeasureDefAndDstu3MeasureReport**: Moved from production to test code (inner record in `Measure.java` test fixture). Was the only remaining production class with zero production consumers after processor deletion.

## Deferred

### resolveParameterMap Deduplication
`resolveParameterMap()` is nearly identical in both resolvers (~30 lines each). Only difference is the `FhirModelResolver` type (`R4FhirModelResolver` vs `Dstu3FhirModelResolver`). Extracting a shared generic in `common/` requires the FHIR Parameters types to share a common interface — they don't. The duplication is stable and low risk.

## Architecture After

All measure evaluation flows through one shared pipeline:

```
Transport Adapter → R4/Dstu3 MeasureResolver → MeasureEvaluationService → R4/Dstu3 MeasureReportBuilder
```

No parallel pipelines. No dead code. Def types are immutable. One representation per concept.

### What's Aligned
1. **MeasureEvaluationService** — Clean version-agnostic shared service. Zero FHIR-version imports.
2. **MeasureEvaluationState** — Properly separates mutable evaluation state from immutable definitions.
3. **MeasureDef** — Clean immutable domain type. Final fields, no version-specific imports.
4. **MeasureArchitectureTest** — ArchUnit tests enforce that `common/` never imports version-specific types.
5. **Domain-specific exceptions** — Proper hierarchy in `common/`, mapped to HTTP at transport boundary.
6. **ResolvedMeasure / ScoredMeasure** — Named domain types as service contracts.
7. **R4MultiMeasureService / Dstu3MeasureService / R4CollectDataService** — All correctly delegate to `MeasureEvaluationService` via dedicated resolvers.

## Violation Tracker

|#|Violation|Status|
|---|---|---|
|V1|`PopulationDef` mutable state duplicating `PopulationState`|**Fixed** (Phase 1)|
|V2|`GroupDef.score`, `StratumDef.score`, `StratumPopulationDef.aggregationResult` mutable|GroupDef **fixed** (Phase 1); StratumDef/StratumPopulationDef **documented** as ephemeral (Phase 1b)|
|V3|`SdeDef.results` duplicating `SdeState`|**Fixed** (Phase 1)|
|V4|`R4CollectDataService` uses legacy `R4MeasureProcessor` pipeline|**Fixed** (Phase 2a)|
|V5|`Dstu3MeasureProcessor` pipeline parallel to shared service|**Fixed** (Phase 2b — processor deleted)|
|V6|`resolveParameterMap()` duplicated in both resolvers|**Deferred** — stable 30-line duplication, no shared FHIR Parameters interface|
|V7|`MeasureDef` built multiple times per request in processor stream|**Fixed** (Phase 2b — processors deleted, resolvers build once)|
|V8|Dead mapper classes|**Fixed** (Phase 4)|
|V9|`@VisibleForTesting` methods that are production entry points|**Fixed** (Phase 2b — processors deleted; `MeasureDefAndDstu3MeasureReport` moved to test)|

## Coding Values Reference

- **Data transformation over stateful logic.** Model operations as pipelines that transform data in, results out.
- **One concept, one representation.** No parallel APIs or shims bridging mismatches.
- **Build domain representations once.** Parse FHIR to domain types once, thread through pipeline.
