# PRP: Primitive-basis intra-subject duplicate counting (holistic fix for CDO-714)

## Metadata
- **Title**: Basis-aware per-subject storage so non-boolean, non-FHIR-Resource population values preserve intra-subject duplicates
- **Status**: Proposed (deferred — blocked on CQL 5.0 / cql1 ExpressionResult type changes)
- **Priority**: Medium (correctness, narrow surface today)
- **Estimated Effort**: 2–3 days
- **Target Branch**: TBD, post-cql1 merge
- **Implementation Date**: TBD
- **Tracks**: [CDO-714](https://simpaticois.atlassian.net/browse/CDO-714)

## Problem Statement

When a Measure has a non-boolean primitive population basis and a CQL expression for a population yields duplicate values for a single subject, the `MeasureReport` population count collapses. Example from CDO-714: a Measure with `populationBasis: date` whose initial-population CQL is `{ @2025-07-03, @2025-07-03, @2025-07-04 }` reports count `2` instead of `3` for a single patient — the two `@2025-07-03` instances are deduplicated.

The root storage is `PopulationDef.subjectResources: Map<String, Set<Object>>` (`cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/PopulationDef.java:32`). Each subject's value is a `HashSetForFhirResourcesAndCqlTypes` that runs three branches in `add()`:

1. `IBaseResource` → dedup by `(resource type, logical id)` via `FhirResourceAndCqlTypeUtils.areEqualResources`.
2. `CqlType` → dedup via `EqualEvaluator.equal`.
3. Anything else → default `HashSet.add` using the element's own `equals`/`hashCode`.

The tactical CDO-714 fix (already landed on this branch) disables branch (2). Branches (1) and (3) remain.

### Survey — what the CQL engine actually returns

A walk through `/Users/luke/development/smile-digital-health/git/clintel-repos/cql/main` runtime + evaluators against `HashSetForFhirResourcesAndCqlTypes` behaviour:

| CQL System type | Java return type | `equals`/`hashCode` shape | Implements `CqlType`? | Dedup risk in `HashSet` |
| --- | --- | --- | --- | --- |
| `Boolean` | `java.lang.Boolean` | value-based (JDK) | no | YES via default branch |
| `Integer` | `java.lang.Integer` | value-based (JDK) | no | YES via default branch — confirmed by `DuplicateTypeIntraSubjectTest.cqlIntegerBasis_*` |
| `Long` | `java.lang.Long` | value-based (JDK) | no | YES via default branch |
| `Decimal` | `org.cqframework.cql.shared.BigDecimal` | value-based (JDK; strict — `1.0 != 1.00`) | no | YES via default branch |
| `String` | `java.lang.String` | value-based (JDK) | no | YES via default branch |
| `Date` | `runtime.Date` | identity (no `equals` override; relies on `EqualEvaluator.equal`) | YES | YES via the (currently-disabled) CqlType branch |
| `DateTime` | `runtime.DateTime` | value-based AND `CqlType` | YES | YES via both paths — `DateTime.kt:291-311` overrides `equals`/`hashCode` |
| `Time` | `runtime.Time` | identity | YES | YES via CqlType branch |
| `Quantity`, `Ratio`, `Code`, `Concept`, `CodeSystem`, `ValueSet`, `Interval`, `Tuple` | `runtime.<X>` | identity | YES | YES via CqlType branch |
| FHIR `IBaseResource` | HAPI `org.hl7.fhir.r4.model.<Resource>` | identity at JDK; `HashSetForFhirResourcesAndCqlTypes` dedups by `(type, id)` (intended) | no | INTENDED — keep behaviour |
| FHIR `IPrimitiveType<T>` | HAPI `org.hl7.fhir.r4.model.<PrimitiveType>` | identity (HAPI `Base.equals` not overridden) | no | NO — distinct instances kept |

**Conclusion.** The bug surface is the entire union of CQL primitive types (Boolean, Integer, Long, Decimal, String, Date, DateTime, Time) plus CQL composite types (Quantity, Ratio, Code, Concept, CodeSystem, ValueSet, Interval, Tuple). Patching one type — `java.lang.Integer`, say — leaves the rest broken. The classes that must NOT change behaviour are FHIR Resources (current dedup-by-id is intended) and FHIR `IPrimitiveType` (already preserves distinct instances).

The right question is not "which Java type to patch" but **"when should the per-subject collection dedup at all?"** Dedup is correct for boolean basis (subject counting) and FHIR Resource basis (resource identity); dedup is wrong for every other basis because each yielded value is a discrete data point.

## Why this is deferred

The CQL 5.0 work in `/Users/luke/development/smile-digital-health/git/clintel-repos/cql/cql1` will significantly change what types the engine's `ExpressionResult` returns. Landing the holistic, basis-aware redesign now would conflict with that work and force a difficult three-way merge. The survey table itself may shift (for example, if cql1 wraps Java boxed primitives in a structured value type), so the design needs a re-check after cql1 merges. The user has chosen to ship a tactical fix that covers the realistic CDO-714 surface (date, FHIR string, boolean) in the current PR and document the holistic plan here.

## Tactical state of `main` after the current PR

The current PR lands three artifacts:

1. **Disabled CqlType dedup branches** in `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/HashSetForFhirResourcesAndCqlTypes.java` (`add`, `remove`, and the static `contains` helper). Every `CqlType` value now falls to default `HashSet` semantics. For `runtime.Date` (no `equals` override), distinct instances survive — the CDO-714 case goes green. In-code comments at each disabled block point at this PRP.
2. **Disabled integer assertion** in `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/DuplicateTypeIntraSubjectTest.java` (`@Disabled` on `cqlIntegerBasis_intraSubjectDuplicates_populationReport_includesDuplicates`) plus a comment block explaining the tactical scope.
3. **Integer Measure dropped from the MultiMeasure chain** in `MultiMeasureDuplicateTypeIntraSubjectTest.java` (count adjusted from `4` to `3`).

The fixture files (`DuplicateTypeIntraSubjectIntegerBasisMeasure.json`, the `Integer Initial Population` CQL define) remain intact and are usable as-is when this PRP is implemented.

### What the tactical fix covers

- **CQL Date** (`runtime.Date`) — Pass. No `equals` override → default `HashSet` keeps distinct instances.
- **FHIR string** (`IPrimitiveType<String>` via `Patient.address[0].line`) — Pass. Was already passing as a regression guard.
- **Boolean basis** — Unaffected. Boolean basis counts subjects, not values.

### What the tactical fix does NOT cover

- `runtime.DateTime` — value-based `equals` override (`DateTime.kt:291-311`) still dedups. Not exercised by any CQIS test today.
- `java.lang.Integer`, `Long`, `String`, `BigDecimal`, `Boolean` — value-based `equals` still dedups. The Integer case is captured by the now-`@Disabled` test.

## Proposed holistic fix

Make per-subject storage in `PopulationDef` basis-aware. For primitive-basis populations (everything except boolean and FHIR Resource), use a List-shaped collection that preserves duplicates regardless of how the element's `equals`/`hashCode` is implemented. For boolean and FHIR Resource basis, keep the `HashSetForFhirResourcesAndCqlTypes` so resource-id dedup continues. Restore the `CqlType` branch the tactical fix disabled — once the Set is only used for boolean/Resource populations, that branch is correct.

This handles every row of the survey table uniformly:
- Java primitives (`Boolean`, `Integer`, `Long`, `BigDecimal`, `String`): on primitive basis the List preserves them; on Resource basis they don't appear.
- `CqlType` subclasses (`Date`, `DateTime`, `Time`, `Quantity`, …): on primitive basis the List preserves them; on Resource basis the restored `CqlType` branch dedups them.
- FHIR Resources: on Resource basis the Set dedups by `(type, id)`.
- FHIR `IPrimitiveType`: under primitive basis the List preserves them.

### Why not wrap each value in an identity envelope

A wrapper that gives identity-based `equals`/`hashCode` works for `addResource` but breaks `retainAllResources`/`removeAllResources` (`PopulationDef.java:147-160`) and `calculateCriteriaStratifierIntersection` (`MeasureMultiSubjectEvaluator.java:744-763`). Those compare populations across the same subject by value; wrapped instances from different populations are never identity-equal, so intersection would always evict everything. Salvaging cross-population comparisons under a wrapper scheme requires custom `equals` on the wrapper, which is the same complexity as the List class below — but with extra wrap/unwrap plumbing at every call site. The List approach lands the same semantics with less surface area.

### Implementation Details

#### 1. New `ListForFhirResourcesAndCqlTypes`

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/ListForFhirResourcesAndCqlTypes.java`

Extends `ArrayList<T>` and **does not override `add`** — duplicates are preserved regardless of element type. Overrides `contains(Object)`, `retainAll(Collection<?>)`, `removeAll(Collection<?>)` to delegate to `FhirResourceAndCqlTypeUtils.areObjectsEqual` (`FhirResourceAndCqlTypeUtils.java:25-33`). That utility:
- For `IBaseResource` vs `IBaseResource` → compares resource type + logical ID.
- For `CqlType` vs `CqlType` → calls `EqualEvaluator.equal`.
- Otherwise → `Objects.equals` (covers `Integer`, `Long`, `BigDecimal`, `String`, `Boolean`).

Sketch:
```java
public class ListForFhirResourcesAndCqlTypes<T> extends ArrayList<T> {
    @Override
    public boolean contains(Object obj) {
        for (T item : this) {
            if (FhirResourceAndCqlTypeUtils.areObjectsEqual(item, obj)) return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> other) {
        Objects.requireNonNull(other);
        boolean modified = false;
        var it = iterator();
        while (it.hasNext()) {
            T element = it.next();
            boolean found = other.stream()
                    .anyMatch(o -> FhirResourceAndCqlTypeUtils.areObjectsEqual(o, element));
            if (!found) { it.remove(); modified = true; }
        }
        return modified;
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> other) {
        // symmetric to retainAll
        ...
    }
}
```

Cross-population intersection then works correctly for every type the engine returns. Multiplicity is preserved on the source side (a denominator with `[42, 42]` retained against a numerator with `[42]` keeps both 42s) — the intended measure semantics.

#### 2. Basis classifier helper

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/FhirResourceUtils.java:55-70`

`determineFhirResourceTypeOrNull(FhirContext, GroupDef)` already exists. Add a code-string overload and a primitive-basis predicate:

```java
@Nullable
public static String determineFhirResourceTypeOrNull(FhirContext fhirContext, String basisCode) { ... }

public static boolean isPrimitiveBasis(FhirContext fhirContext, String basisCode) {
    return !"boolean".equals(basisCode)
            && determineFhirResourceTypeOrNull(fhirContext, basisCode) == null;
}
```

Refactor the existing `GroupDef` overload to delegate.

#### 3. Restore CqlType branch in `HashSetForFhirResourcesAndCqlTypes`

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/HashSetForFhirResourcesAndCqlTypes.java`

Un-comment the three `CqlType` blocks the tactical fix disabled. Once the Set is only used for boolean/Resource basis (next step), those branches are correct again. Leaving them disabled would silently regress dedup for a stratifier or auxiliary Set that happens to receive a `CqlType` value.

#### 4. Basis-aware storage in `PopulationDef`

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/PopulationDef.java`

- Add `private final boolean primitiveBasis;` field and a constructor parameter on the canonical constructor (`:44-61`). Legacy short constructor (`:34-42`) forwards `false`.
- Change `subjectResources` (`:32`) from `Map<String, Set<Object>>` to `Map<String, Collection<Object>>`.
- Replace `addResource` (`:221-225`):
  ```java
  public void addResource(String key, Object value) {
      subjectResources.computeIfAbsent(key, k -> newCollectionForBasis()).add(value);
  }

  private Collection<Object> newCollectionForBasis() {
      if (primitiveBasis && !hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
          return new ListForFhirResourcesAndCqlTypes<>();
      }
      return new HashSetForFhirResourcesAndCqlTypes<>();
  }
  ```
  The `MEASUREOBSERVATION` guard preserves the `Set<Map<Object, Object>>` invariant used by `MeasureEvaluator`'s observation-handling helpers.
- Widen `getResourcesForSubject(String)` (`:216-218`) and `getSubjectResources()` (`:212-214`) to return `Collection<Object>` / `Map<String, Collection<Object>>`. The empty default in `getResourcesForSubject` should call `newCollectionForBasis()` so the empty default matches runtime semantics.
- `retainAllResources` (`:147-149`), `removeAllResources` (`:155-157`), `removeExcludedMeasureObservationResource` (`:121-145`) already use `Collection`-shaped methods; widen the local `Set<Object>` declarations.
- `getAllSubjectResources` (`:182-187`) and `countObservations` (`:190-200`) already use `flatMap(Collection::stream)` — no change.

#### 5. Adapt call sites

All readers outside `PopulationDef` either iterate the collection or call `Collection`-defined methods; widen `Set<Object>` locals to `Collection<Object>`:

- `MeasureEvaluator.java`: observation helpers `retainObservationSubjectResourcesInPopulation` (`:471`) and `removeObservationSubjectResourcesInPopulation` (`:543`) take `Map<String, Set<Object>>` parameters; widen to `Map<String, Collection<Object>>`. The cast around `:485` widens to `Collection<Map<Object, Object>>` — runtime safety holds because the `MEASUREOBSERVATION` guard in step 4 keeps observation populations on Set storage. Direct field touches at `:354, 358, 369, 373, 450, 457-458` widen locals.
- `MeasureMultiSubjectEvaluator.java:744-763` (`calculateCriteriaStratifierIntersection`): replace `Sets.intersection(populationResultsPerSubject, stratifierResultsPerSubject)` with a stream filter using `stratifierResultsPerSubject::contains` so a List-backed primitive-basis collection also works. For primitive basis, return the list of matches unwrapped; for boolean/Resource basis, keep the existing `new HashSetForFhirResourcesAndCqlTypes<>(...)` wrap. Widen return type to `Collection<Object>`. Branch on `FhirResourceUtils.isPrimitiveBasis(FhirContext.forR4Cached(), populationDef.getPopulationBasis().code())`.
- `MeasureMultiSubjectEvaluator.java:861-865, 919-920`: widen `entry.getValue()` locals.
- `MeasureReportDefScorer.java:516, 543, 569`: widen intermediate `Set<Object>` types in stream pipelines.
- `EvaluationResultFormatter.java:258`, `MeasureObservationHandler.java:39,44`, `R4SupportingEvidenceExtension.java:121-138`: widen typed locals.
- `StratumPopulationDef.java:28` (`populationDefEvaluationResultIntersection`): widen field/accessor/constructor from `Set<Object>` to `Collection<Object>`. The `.size()` consumer at `:114` is polymorphic.

#### 6. Thread `primitiveBasis` through the builders

`R4MeasureDefBuilder` has no `FhirContext` field but is R4-specific; use `FhirContext.forR4Cached()` at the construction sites.

- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/r4/R4MeasureDefBuilder.java:259-309` (`buildPopulationDef`, `buildPopulationDefForDateOfCompliance`): compute `boolean primitiveBasis = FhirResourceUtils.isPrimitiveBasis(FhirContext.forR4Cached(), populationBasis.code());` and pass to the `PopulationDef` constructor.
- `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/dstu3/Dstu3MeasureDefBuilder.java`: same with `FhirContext.forDstu3Cached()`.
- `SupportingEvidenceDef` if it has a similar per-subject map — verify and apply the same treatment at implementation time.

## Adjustments required for cql1 compatibility

Once cql1 (`/Users/luke/development/smile-digital-health/git/clintel-repos/cql/cql1`) merges, re-check the survey table. Specifically:

- If `ExpressionResult` no longer hands raw Java boxed primitives back (e.g., the engine wraps them in a structured value type), the design may simplify to "List for non-boolean non-FHIR-Resource basis" without per-row analysis. The `Object`-typed storage and the `areObjectsEqual` fallback would still work, but the dedup risk table reduces.
- If `runtime.Date`/`DateTime`/etc. gain or lose `equals` overrides, re-check the regression-guard expectations for `runtime.DateTime`.
- If `EqualEvaluator.equal` semantics shift for any type (e.g., new null-handling rules), re-derive the `FhirResourceAndCqlTypeUtils.areEqualCqlTypes` contract.

This re-check is mandatory; do not assume the survey above is still authoritative after cql1.

## Verification

1. Restore the disabled integer test and the integer Measure entry in the MultiMeasure chain:
   - Remove `@Disabled` from `DuplicateTypeIntraSubjectTest#cqlIntegerBasis_intraSubjectDuplicates_populationReport_includesDuplicates`.
   - Add `DuplicateTypeIntraSubjectIntegerBasisMeasure` back to `MultiMeasureDuplicateTypeIntraSubjectTest`'s `.measureId(...)` chain and adjust `hasMeasureReportCount(3)` → `hasMeasureReportCount(4)`.
2. Run:
   ```bash
   ./gradlew spotlessApply
   ./gradlew :cqf-fhir-cr:test \
       --tests "org.opencds.cqf.fhir.cr.measure.r4.DuplicateTypeIntraSubjectTest" \
       --tests "org.opencds.cqf.fhir.cr.measure.r4.MultiMeasureDuplicateTypeIntraSubjectTest" \
       --tests "org.opencds.cqf.fhir.cr.measure.r4.StratifierMultiSubjectDateBasisTest"
   ./gradlew :cqf-fhir-cr:test
   ```
3. Full CQIS suite end-to-end against the branch.

Expected:
- All four methods in `DuplicateTypeIntraSubjectTest` pass (CQL Date, CQL Integer, FHIR string).
- `MultiMeasureDuplicateTypeIntraSubjectTest` passes across all four basis Measures.
- `StratifierMultiSubjectDateBasisTest` continues to pass — cross-subject date counting at 10 and per-subject at 5.
- Full `:cqf-fhir-cr:test` clean.

## Acknowledgement

Survey and design pass authored by Claude Opus 4.7 (1M context) on 2026-05-29 against the working tree of branch `ld-20260528-cql-types-and-fhir-resources-sets`.
