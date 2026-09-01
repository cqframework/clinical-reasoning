# PRP: Eliminate eager FHIR reconstruction in SDE/stratifier value accumulation

## Metadata
- **Title**: Stop reflectively rebuilding whole FHIR resources in `StratumValueWrapper` to read their id
- **Status**: Proposed
- **Priority**: High (Performance regression + latent correctness defect)
- **Estimated Effort**: 1 day (Parts 1–2), 2–3 days (Part 3)
- **Target Branch**: TBD — branches from `ld-20260901-anton-fix-fhir-model-overrides` (`3a82da9c`)
- **Reported From**: `cqis-spark` HEDIS 2025 measure evaluation, 400-member cohort

## Problem Statement

`SdeDef.accumulate()` converts **every** SDE value from its engine-native `ClassInstance` into a
fully materialized HAPI FHIR object graph — reflectively, field by field — and then uses only the
resource's **id** as a grouping key. The graph is discarded immediately afterward.

On a measure with many resource-valued SDEs this dominates the entire evaluation.

### Measured impact

Two HEDIS 2025 certified measures were used as a controlled pair. `AAB-Details` and
`AAB-Reporting` are structurally identical — one group, four populations, one stratifier each —
and differ **only** in supplemental data:

| | `AAB-Reporting` | `AAB-Details` |
|---|---|---|
| `supplementalData` | 0 | **38** (≈28 return FHIR resource collections; 9 return `ExplanationOfBenefit`) |
| groups / populations / stratifiers | 1 / 4 / 1 | 1 / 4 / 1 |

Subtracting Reporting from Details on the same build, same 400-member cohort, same host isolates
the SDE cost exactly:

| | CR 4.8.0 / CQL 4.4.0 | CR 4.12.0 / CQL 5.3.0 |
|---|---|---|
| CQL evaluation, Reporting | 122 714 ms | 203 205 ms |
| CQL evaluation, Details | 145 738 ms | 1 177 396 ms |
| **Δ (SDE accumulation)** | **23 024 ms** | **974 191 ms** |
| Report build, Reporting | 3 982 ms | 4 108 ms |
| Report build, Details | 5 874 ms | 74 585 ms |
| **Δ (SDE rendering)** | **1 892 ms** | **70 477 ms** |
| **Total SDE cost per member** | **62 ms** | **2 612 ms** |

**42x.** Wall-clock for the run went from 24 s to 6 minutes.

The regression also destroys parallelism. Per-member cost becomes a function of how many resources
that member's SDEs return, which varies far more between members than CQL logic does, so data skew
becomes time skew. Across 11 Spark partitions, effective parallelism measured 7.7–9.2x on
`AAB-Reporting` but 3.65x on `AAB-Details`; on CR 4.11.1 a single partition consumed 311.5 s of a
312.8 s span.

### Root cause

Under CQL 4.x the engine handed CR real HAPI objects. `StratumValueWrapper`'s constructor was a
plain assignment (CR 4.8.0):

```java
public StratumValueWrapper(Object value) {
    this.value = value;
}
```

CQL 5 changed the value model — the class's own javadoc records it: *"CQL-5 stratifier/SDE results
arrive as engine-native values: FHIR resources and complex types as `ClassInstance`."* The
constructor now converts on every construction:

```java
public StratumValueWrapper(Object value) {
    this.value = normalizeEngineNativeValue(value);   // → full reflective FHIR reconstruction
}
```

The call chain, matching the production stack trace exactly:

```
SdeDef.accumulate                                  ← 38 SDEs × 400 members × N resources each
  → new StratumValueWrapper(value)
    → normalizeEngineNativeValue
      → ClassInstanceHelper.convertToFhirR4IfNeeded
        → CqlFhirParametersConverter.toFhirValue    ← recurses over every child element
          → BaseRuntimeDeclaredChildDefinition.setFieldValue
            → java.lang.reflect.Field.set
```

And the entire result is consumed by this, in `StratumValueWrapper#getKey`:

```java
} else if (value instanceof IBaseResource resource) {
    key = resource.getIdElement().toVersionless().getValue();     // "MedicationDispense/123"
}
```

An `ExplanationOfBenefit` — dozens of nested backbone elements — is reconstructed through
reflection so that its id can be read. The `ClassInstance` already carried that id.

### Secondary defect: the reconstruction is also a crash source

`CqlFhirParametersConverter#toFhirValue` derives the target HAPI class from the **CQL value's own
type name** (`modelResolver.resolveType(typeName)`) rather than from the HAPI child definition it
is about to populate, then patches the guess with heuristics. Two known failures of the same
approach:

```
IllegalArgumentException: Could not resolve inner FHIR type: AdjudicationComponent
```
— nested backbone class-name guessing; addressed in `3a82da9c` by the `parentName` string
replacement at `CqlFhirParametersConverter.kt:400-412`.

```
IllegalArgumentException: Can not set org.hl7.fhir.r4.model.Enumeration field
org.hl7.fhir.r4.model.MedicationDispense.status to org.hl7.fhir.r4.model.CodeType
```
— a bound-code field. `toFhirValue` produced a `CodeType`; `MedicationDispense.status` is declared
`Enumeration<MedicationDispenseStatus>`. The `instance is IBaseEnumeration` branch at line 426
handles enumerations correctly, but is never reached because `resolveType` returned `CodeType`.
This reproduces on CR 4.11.1 / CQL 5.2.0 with `AAB-Details` and fails 134 of 400 members.

Every bound-code field on every resource type an SDE can return is a latent instance of this.
Patching them one at a time will not converge.

## Solution Overview

**Do not convert FHIR resources during accumulation at all.** Derive the grouping key directly from
the `ClassInstance`, and let conversion happen later — only for the values that are actually
rendered.

Three facts make this nearly free to implement, because the pieces already exist:

1. **`ClassInstanceHelper.getId(ClassInstance)` already returns exactly the required key format.**
   It reads `type.localPart` + `id.value` off the `ClassInstance` and returns `"Type/id"` —
   byte-identical to what `resource.getIdElement().toVersionless().getValue()` produces, with no
   conversion.

2. **`ClassInstanceHelper.isFhirResource(FhirVersionEnum, ClassInstance)` already exists** as the
   predicate distinguishing resources (huge, id-keyed) from complex datatypes (small, value-keyed).

3. **`R4MeasureReportBuilder#buildSDE` already handles the unconverted shape** — and this branch is
   currently dead code, because the wrapper converts before the builder ever sees the value:

   ```java
   } else if (key.getValue() instanceof ClassInstance classInstance
           && isFhirResource(FhirVersionEnum.R4, classInstance)) {
       var resource = (Resource) convertToFhirR4(classInstance);
       bc.addCriteriaExtensionToSupplementalData(resource, sde.id(), sde.description());
   }
   ```

**Key insight**: `accumulate()` produces a `Map<StratumValueWrapper, Long>` — a frequency count over
**distinct** values. Deferring conversion to the report builder collapses it from *once per
occurrence per member* to *once per distinct value in the final map*. On a population report over
400 members, most SDE resources are unique per member, so the win is not primarily dedup — it is
that population reports never need the graph at all, only the id reference.

## Implementation Details

### Part 1 — Defer resource conversion in `StratumValueWrapper`

**File**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratumValueWrapper.java`

Convert eagerly for everything **except** FHIR resources. Complex datatypes (`Coding`,
`CodeableConcept`, `Identifier`) must still convert — `getKey()` reads their contents, and they are
small. CQL `SimpleValue` unwrapping must also stay: the existing javadoc explains that a CQL String
renders as `'male'` rather than `male`, and skipping it produces wrong stratum keys.

```java
private static Object normalizeEngineNativeValue(Object rawValue) {
    // A FHIR *resource* is keyed by its id, which ClassInstanceHelper.getId reads directly off the
    // ClassInstance. Converting it to HAPI first means reflectively rebuilding the entire element
    // graph — for an ExplanationOfBenefit, dozens of nested backbone elements — and discarding it.
    // Leave it engine-native; the report builders convert the values they actually render.
    if (rawValue instanceof ClassInstance classInstance
            && ClassInstanceHelper.isFhirResource(FhirVersionEnum.R4, classInstance)) {
        return rawValue;
    }

    // Complex datatypes (Coding, CodeableConcept, Identifier) are small and getKey() reads their
    // contents, so they still convert here.
    var converted = ClassInstanceHelper.convertToFhirR4IfNeeded(rawValue);
    if (converted != rawValue) {
        return converted;
    }
    // ... existing SimpleValue unwrapping, unchanged ...
}
```

Then add a `ClassInstance` branch to the three renderers, ahead of the `IBaseResource` branch they
mirror:

```java
// getKey()
} else if (value instanceof ClassInstance ci
        && ClassInstanceHelper.isFhirResource(FhirVersionEnum.R4, ci)) {
    key = ClassInstanceHelper.getId(ci);
} else if (value instanceof IBaseResource resource) {
    key = resource.getIdElement().toVersionless().getValue();
}
```

The same branch is needed in `getDescription()` and in the private `getValueAsString(Object)`, both
of which currently end at `value instanceof IBaseResource → resource.getIdElement()...`. Without
them a raw `ClassInstance` falls through to `value.toString()`, which yields a wrong key and breaks
dedup.

`getId` returns `null` when the `ClassInstance` has no `id` element. `getKey()` already throws
`InvalidRequestException` on a null key, so fall back to the existing `value.toString()` path rather
than propagating null.

**Note the pre-existing R4 hard-coding**: `normalizeEngineNativeValue` already calls
`convertToFhirR4IfNeeded` unconditionally from a version-agnostic `common` class. This PRP does not
fix that, but `isFhirResource` takes a `FhirVersionEnum`, so the new branch should thread the
version through rather than entrench the assumption further. Resolving it properly is separate work.

### Part 2 — Memoise `getKey()`

`hashCode()` calls `getKey()`. `equals()` calls it **twice** (on both operands). `accumulate()`'s
`Collectors.groupingBy(Function.identity(), Collectors.counting())` calls `hashCode()` per element
and `equals()` on every hash collision. `getKey()` allocates a `CqlExpressionValue` and walks an
eight-branch `instanceof` chain each time.

The wrapped value is effectively immutable after construction, so cache it:

```java
private String cachedKey;

public String getKey() {
    if (cachedKey == null) {
        cachedKey = computeKey();
    }
    return cachedKey;
}
```

Secondary to Part 1 in magnitude, but it is a few lines and removes a repeated cost on the same hot
path.

### Part 3 — Resolve target types from the HAPI child definition in `toFhirValue`

**File**: `cqf-fhir-cql/src/main/java/org/opencds/cqf/fhir/cql/engine/parameters/CqlFhirParametersConverter.kt`

Parts 1–2 make the conversion rare; they do not make it correct. The report builder still calls
`convertToFhirR4(classInstance)` for rendered values, so the `MedicationDispense.status` crash moves
rather than disappears — it just affects far fewer values.

The underlying flaw is that `toFhirValue` guesses the HAPI class from the CQL type name and then
repairs the guess:

```kotlin
clazz = modelResolver.resolveType(typeName)
if (!parentName.isNullOrBlank() && !clazz.isEnum && clazz.name.contains("$") && ...) {
    val correctClassName = clazz.name.replace(clazz.enclosingClass.simpleName, parentName)
    clazz = Class.forName(correctClassName)     // heuristic repair
}
```

The authoritative answer is available at the recursion site. The loop already holds the `child`
definition:

```kotlin
for (child in definition.getChildren()) {
    val elementValue = (valueToConvert as ClassInstance)[child.elementName]
    ...
    child.mutator.addValue(instance, toFhirValue(elementValue, parentNameForChildren))
}
```

**Change the recursion to pass the child definition rather than a `parentName` string**, and derive
the target class from it (`BaseRuntimeChildDefinition#getChildByName(...).getImplementingClass()`,
or `getChildElementDefinitionByDatatype` where the child is a choice). Fall back to
`modelResolver.resolveType(typeName)` only at the top-level entry point, where there is no parent
child definition.

This subsumes both known failures:
- Nested backbone classes come from the child definition, so the `parentName` string-replacement
  heuristic from `3a82da9c` can be **removed**, not merely retained.
- A bound-code child reports its implementing class as `Enumeration`, so the existing
  `instance is IBaseEnumeration` branch is reached and `MedicationDispense.status` populates
  correctly.

Part 3 is independently valuable and can ship separately from Parts 1–2 in either order.

## Design Decisions

### Decision 1: Why not just cache the conversion per `ClassInstance`?

A conversion cache keyed on identity would help only where the same instance recurs, and SDE
results are largely distinct per member. It would also retain the memory cost — holding full HAPI
graphs for every accumulated value — which is the second half of the problem, since allocation
pressure is what collapses parallelism under Spark. Not converting is strictly better than
converting once.

### Decision 2: Why not convert lazily inside `StratumValueWrapper` (a lazy getter)?

Tempting, but it hides an expensive call behind `getValue()`, which reads as a field accessor. The
report builders are the only consumers that need the HAPI form, they already know they need it, and
`R4MeasureReportBuilder` already has the branch. Keeping conversion at the call site that requires
it keeps the cost visible.

### Decision 3: Why keep converting complex datatypes eagerly?

`getKey()` and `getDescription()` read the *contents* of `Coding` / `CodeableConcept` / `Identifier`
via `IAdapterFactory`, so they need the HAPI form regardless. They are also a handful of primitive
children, not a resource graph. Making them lazy adds branching for no measurable gain.

### Decision 4: Scope — SDEs and stratifiers together

`StratumValueWrapper` is constructed from five sites: `SdeDef.accumulate` and four in
`MeasureMultiSubjectEvaluator` (lines 605, 633, 658, 675) that build stratum values. The fix is in
the wrapper, so both paths benefit. Stratifiers usually return codes rather than resources, so the
measured win is SDE-side, but resource-valued stratifiers exist and would regress identically.

## Performance Analysis

Expected effect on the `AAB-Details` 400-member benchmark, per member:

| | current | expected |
|---|---|---|
| SDE accumulation (`cql_eval` delta) | 2 435 ms | ≈ 0 — an id read replaces a graph rebuild |
| SDE rendering (`report_build` delta) | 176 ms | unchanged for individual reports; population reports emit references, not graphs |
| **total SDE cost** | **2 612 ms** | **target < 100 ms** (CR 4.8.0 measured 62 ms) |

Projected run wall-clock: ~6 minutes → under 1 minute, the remainder being the separate ~2.7x CQL
hot-path regression between 4.8 and 4.11 that this PRP does **not** address (visible on
`AAB-Reporting`, which has no SDEs: 118 → 315 ms per member steady-state). That is a distinct
investigation.

Memory: proportional reduction in transient allocation during accumulation, which is what should
restore parallelism under Spark from 3.65x toward the 7.7–9.2x that `AAB-Reporting` sustains.

## Testing Strategy

### Correctness
- **`NestedBackboneSdeTest` (existing, from `3a82da9c`) must stay green.** It is the regression
  guard for the conversion path and covers depth-1 and depth-2 backbone elements.
- **New**: an SDE returning `MedicationDispense` resources, asserting the bound-code `status` field
  round-trips. This is red before Part 3 and green after.
- **New**: assert `StratumValueWrapper.getKey()` returns the identical string for a given resource
  whether constructed from a `ClassInstance` or from a HAPI `IBaseResource`. This is the invariant
  Part 1 depends on and the one that would silently corrupt SDE grouping if `getId`'s format ever
  drifted from `getIdElement().toVersionless().getValue()`.
- **DSTU3**: `Dstu3MeasureReportBuilder` has **no** `ClassInstance` branch — it calls
  `getValueAsString()` / `getKey()` directly. Part 1's additions to those two methods are what keep
  it working; add a DSTU3 SDE test covering a resource-valued SDE explicitly.

### Performance
- Benchmark `AAB-Details` (38 SDEs) against `AAB-Reporting` (0 SDEs) over a fixed cohort. The
  Details-minus-Reporting delta is the metric; it isolates SDE cost from everything else and both
  measures ship in the HEDIS 2025 certified content.
- Assert the delta does not scale with resource count per member — that is the property being
  restored.

## Implementation Checklist

- [ ] Part 1: skip eager conversion for `ClassInstance` resources in `normalizeEngineNativeValue`
- [ ] Part 1: add `ClassInstance` branches to `getKey()`, `getDescription()`, `getValueAsString()`
- [ ] Part 1: handle `getId()` returning null (fall back, do not propagate)
- [ ] Part 1: thread `FhirVersionEnum` into `isFhirResource` rather than hard-coding R4 further
- [ ] Part 2: memoise `getKey()`
- [ ] Part 3: pass child definitions through `toFhirValue` recursion; derive target class from them
- [ ] Part 3: remove the `parentName` string-replacement heuristic
- [ ] Tests: `MedicationDispense.status` bound-code SDE
- [ ] Tests: `ClassInstance` vs `IBaseResource` key equivalence
- [ ] Tests: DSTU3 resource-valued SDE
- [ ] Benchmark: `AAB-Details` minus `AAB-Reporting` delta before/after

## Success Criteria

### Functional
- `NestedBackboneSdeTest` and the existing measure test suite pass unchanged.
- `AAB-Details` completes over 400 members with **zero** per-member evaluation failures on
  CR 4.11.1+ content (currently 134 of 400 fail).
- SDE observation output is byte-identical to current output where current output succeeds.

### Non-functional
- SDE cost per member on `AAB-Details` under 100 ms (from 2 612 ms).
- Effective parallelism on `AAB-Details` within 20% of `AAB-Reporting` on the same host.

### Code quality
- The `parentName` heuristic is deleted, not extended.
- No new version-specific hard-coding in the version-agnostic `common` package.

## Conclusion

The 42x regression is one line in a constructor: a full reflective FHIR reconstruction performed to
read an id that the source object already carried. Every component needed to fix it —
`ClassInstanceHelper.getId`, `ClassInstanceHelper.isFhirResource`, and the report builder's
`ClassInstance` branch — is already in the codebase; the eager conversion is what renders them
unreachable. Parts 1–2 are small and low-risk. Part 3 is the larger change, and it converts a class
of recurring crashes into a resolved one by asking HAPI what type a field is instead of guessing.
