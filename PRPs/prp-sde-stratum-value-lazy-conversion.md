# PRP: Eliminate eager FHIR reconstruction in SDE/stratifier value accumulation

## Metadata
- **Title**: Stop reflectively rebuilding whole FHIR resources in `StratumValueWrapper` to read their id
- **Status**: Parts 1–4 implemented; Parts 1–2 measured, Parts 3–4 covered by tests only
- **Priority**: High (Performance regression + latent correctness defect)
- **Estimated Effort**: 1 day (Parts 1–2, done), 2–3 days (Part 3, done), 1–2 days (Part 4, done)
- **Target Branch**: `ld-20260901-sde-lazy-conversion`, from `main` (`3a82da9c`)
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

1. **`ClassInstanceHelper.getId(ClassInstance)` reads the id straight off the instance**, with no
   conversion: `type.localPart` + `id.value`, returned as `"Type/id"`.

   > **Corrected during implementation.** This section claimed `getId` was *byte-identical* to
   > `resource.getIdElement().toVersionless().getValue()`. It is not. Converting a `ClassInstance`
   > copies `id.value` and nothing else, so the converted resource reports the **bare** id — the
   > current key for `ExplanationOfBenefit/eob-flat` is `eob-flat`, not `ExplanationOfBenefit/eob-flat`.
   > Keying on `getId` would have changed every rendered stratum value and every DSTU3 SDE
   > observation code, and would have split one resource across two strata depending on whether it
   > happened to be converted. Part 1 therefore uses a new `ClassInstanceHelper.getIdPart`, which
   > returns the bare id part, and `getId` is now defined in terms of it. `getId` remains correct for
   > its own callers, which build references.

2. **`ClassInstanceHelper.isFhirResource(FhirVersionEnum, ClassInstance)` already exists** as the
   predicate distinguishing resources (huge, id-keyed) from complex datatypes (small, value-keyed).
   A version-agnostic overload was added alongside it for callers holding no FHIR version of their
   own — see the note at the end of Part 1.

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

> **As implemented.** Threading a version through was not available: `StratumValueWrapper` is
> constructed from `SdeDef.accumulate` and four sites in `MeasureMultiSubjectEvaluator`, none of
> which holds a `FhirVersionEnum`, and the question being asked does not need one. A `ClassInstance`
> names its type but not the version that type came from, and telling a resource from a complex
> datatype has the same answer in every version, so `ClassInstanceHelper` gained
> `isFhirResource(ClassInstance)`, which tests the name against the resource types of DSTU3, R4, R4B
> and R5 together. The version-qualified overload remains for questions where the version does
> matter — conversion — and is what the R4 report builders still call. The two renderer branches use
> `getIdPart`, not `getId`, per the correction in the Solution Overview.

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

> **As implemented**, with three findings worth carrying forward.
>
> **The crash is engine-version-dependent.** `BoundCodeSdeTest` — a measure whose SDE returns
> `MedicationDispense` — fails on the version catalog's `cql = "5.2.0"` with exactly the
> `Enumeration ... to CodeType` message above, and **passes unchanged** on the
> `5.3.0-fix-model-resolver-overrides` snapshot, which makes `resolveType` return the right class
> upstream. Part 3 was validated against 5.2.0 so the test means something at the committed pin. The
> class of defect is what Part 3 removes: the converter no longer guesses, so it does not depend on
> the guess being good.
>
> **Instantiate through HAPI, not through reflection.** Deriving the class was necessary but not
> sufficient. A bound code's implementing class is `Enumeration`, whose no-arg constructor leaves it
> without an `EnumFactory`, so `setValueAsString` still fails. The fix is
> `elementDefinition.newInstance(childDefinition.instanceConstructorArguments)` — HAPI's own parser
> path, where the constructor arguments *are* the `EnumFactory`.
>
> **Two element kinds must stay on the type-name path.** `contained` is a `CONTAINED_RESOURCE_LIST`
> whose implementing class is the `IBaseResource` interface, and `Narrative.div` is an `XhtmlNode`,
> which is not an `IBase` at all — neither can be instantiated from its child definition. The
> implementation accepts only `PRIMITIVE_DATATYPE`, `ID_DATATYPE`, `COMPOSITE_DATATYPE`,
> `RESOURCE_BLOCK` and `RESOURCE`, and falls back for the rest, which handles them no worse than
> before. Also note `getValidChildNames()` is safe to call on every child but `getChildByName` is
> not: it throws `AssertionError` on `modifierExtension`. Only the single-valid-name path calls it;
> choices go through `getChildElementDefinitionByDatatype`.

### Part 4 — Rebuild the custom sets around id-keying

**Files**:
- `cqf-fhir-cr/.../measure/common/HashSetForFhirResourcesAndCqlTypes.java`
- `cqf-fhir-cr/.../measure/common/HashSetForCqlExpressionValues.java`
- `cqf-fhir-cr/.../measure/common/FhirResourceAndCqlTypeUtils.java`

Both custom sets predate the CQL 5 migration. They were written when expression results were HAPI
objects, and the CQL-5 integration did not revisit them. They are now doing something other than
what they were designed to do.

**4a. The CQL-type detection never fires.** `FhirResourceAndCqlTypeUtils` recognizes exactly one CQL
type:

```java
public static Value castToCqlTypeIfApplicable(Object obj) {
    if (obj instanceof Date cqlDate) return cqlDate;   // only Date
    return null;
}
```

The naming downstream still reflects that origin — `areEqualCqlTypes(Value cqlDate1, Value
cqlDate2)`. CQL 5 expression results are `ClassInstance`, which is neither `IBaseResource` nor
`Date`, so in `add()` and `remove()` both casts return null and control falls through to
`super.add` / `super.remove`. Those are plain `HashSet` operations keyed on
`ClassInstance.hashCode()` — the same deep structural hash Part 1 exists to avoid. **The class is
bypassed precisely where the values now live.**

**4b. `contains()` still fires, on a different relation.** `areObjectsEqual` tests `instanceof
Value`, and `ClassInstance` *is* a `Value`, so `contains` routes into `EqualEvaluator.equal`. The
result is two relations in one collection:

| operation | relation |
|---|---|
| `add`, `remove` | `ClassInstance.equals` — Kotlin structural: `type` + `elements` map equality |
| `contains`, `retainAll` | `EqualEvaluator.equal` — CQL `=` semantics |

These are not defined to agree. CQL `=` deliberately diverges from structural equality for several
types: `Decimal` compares via `compareTo` (so `1.0 = 1.00`) where map equality over `BigDecimal` is
scale-sensitive, and `Quantity`, `Interval` and uncertain `DateTime` each have their own rules. So
`contains(x)` can answer false for an `x` that `add` placed in the set.

This is a **narrow** divergence, not a wholesale one — `structuredValueElementsEqual` skips
element pairs that are null on both sides, so two identical resources still compare equal.
Confirm it against `Decimal.equals` before treating it as a live defect. It matters because
`retainAll` drives stratum population intersection at `MeasureMultiSubjectEvaluator:802,809`, where
a false negative silently drops resources from a population.

> **Confirmed, and the resource case is worse than described.** Measured directly:
>
> | | `equals` | `EqualEvaluator.equal` | same hash |
> |---|---|---|---|
> | `Decimal("1.0")` vs `Decimal("1.00")` | false | **true** | no |
> | two identical `ClassInstance` resources | true | true | yes |
> | same resource, differing `meta.versionId` | false | **null** (uncertain) | — |
>
> The `Decimal` divergence is real, and compounded by 4a: `castToCqlTypeIfApplicable` recognised
> only `Date`, so `Decimal` never reached the CQL relation on `add` at all.
>
> The third row is the one that matters most, and neither relation catches it: the same resource
> retrieved twice with different metadata compares unequal *structurally* **and** uncertain under
> CQL `=`, which `areEqualCqlTypes` reads as unequal. So a set of evaluated resources could hold one
> resource twice regardless of which relation ran. Only id-keying collapses it. Both rows are now
> regression tests in `HashSetForFhirResourcesAndCqlTypesTest`.

**4c. Both sets are O(n) per operation.** `contains` is a linear scan (`containsInner` iterates the
collection) and `add` calls `contains`, so building an n-element set is **O(n²)** — and under CQL 5
each comparison walks a resource graph, making it O(n² × graph). `HashSetForCqlExpressionValues`
documents this in its own javadoc: *"Bucket placement still uses the wrapper's default
`Object.hashCode()` … so `add` / `remove` / `contains` / `retainAll` fall through to linear-time
identity checks."*

**The fix is the same primitive as Part 1.** Key elements on `(resourceType, logical id)` — for
`IBaseResource` and for FHIR-resource `ClassInstance` alike — falling back to identity for values
carrying no id. That yields three things at once: O(1) `add`/`contains`/`remove`, a **single**
relation across all four operations, and no deep hashing.

Unlike the equivalent change in the CQL engine, this code can see FHIR types directly, so no
model-agnostic key extraction is needed — `ClassInstanceHelper.getId` already supplies the
`ClassInstance` half and `IBaseResource.getIdElement()` the other.

`HashSetForCqlExpressionValues` most likely collapses into the same class once keyed; it is the same
structure plus a `CqlExpressionValue.raw()` unwrap step. Decide that during implementation rather
than committing to it here.

> **The design premise has inverted.** The javadoc states these exist "strictly to compensate for the
> fact that FHIR resource classes and CQL types do not implement equals() and hashCode()". Under
> CQL 5 `ClassInstance` implements both — deeply. The job is no longer to *supply* an equality but to
> *suppress* the expensive one in favour of id-keying. That is why this is a rewrite rather than a
> repair, and why leaving the classes as-is is not a neutral choice: they are currently slower than a
> plain `HashSet` for `contains`, and inconsistent with it for `add`.

Part 4 is independent of Parts 1–3 and can ship on its own.

> **As implemented.** Elements are stored in a `LinkedHashMap` under a new package-private
> `IdentityKey`, which is what makes one relation answer every operation rather than each reaching
> for its own:
>
> | key | for | cost |
> |---|---|---|
> | `ResourceKey(String)` | a FHIR resource, HAPI or `ClassInstance`, as `Type/id` | O(1), a string compare instead of a graph walk |
> | `CqlValueKey(Value)` | any other CQL value, compared with CQL `=` | one shared bucket, so linear among themselves |
> | `PlainKey(Object)` | everything else, on its own `equals` | O(1) |
>
> `CqlValueKey` has a constant hash because CQL `=` is not hash-compatible — `1.0 = 1.00` is true
> across differing hashes, and cross-precision `DateTime` comparison is uncertain rather than false.
> There is no key that spreads these across buckets, so they keep the linear behaviour they had. This
> is not a compromise in practice: resources take the keyed path, and what is left is the `Date`s and
> `Decimal`s a stratifier returns.
>
> **`HashSetForCqlExpressionValues` did collapse** — into a `keyFor` override on the parent that
> unwraps the wrapper, 140 lines down to 57. That is the whole of the difference between them.
>
> **`retainAll` and `removeAll` are overridden**, and must be. The inherited implementations ask the
> *other* collection what it contains, and a plain `List` or `HashSet` answers by Java object
> identity — which is precisely how a population intersection drops resources. Both now key the
> other collection first, so the comparison runs in this set's relation whatever it is handed.
>
> **4a is resolved by deletion, not extension.** With keying, `castToResourceIfApplicable` and
> `castToCqlTypeIfApplicable` have no callers left; the `Date`-only detection is gone rather than
> widened to `ClassInstance`. `areObjectsEqual` and the keys now share one `resourceIdentity`
> function, so `HashMapForFhirResourcesAndCqlTypes` and the ad-hoc `areObjectsEqual` call sites move
> with them. (That map remains O(n) per operation — out of scope here, and a candidate for the same
> treatment.)
>
> **Two deliberate deviations, both behaviour changes rather than speedups:**
>
> - **Id-less resources do not fall back to identity**, as this section proposed. The existing
>   `addFhirResourceWithNullIdTwiceAddsOnlyOne` pins the opposite: two id-less HAPI resources of a
>   type are today one element. That is preserved (key `Type/`), while an id-less `ClassInstance`
>   falls through to CQL equality, which is the relation *that* form already had. Each keeps its own
>   current behaviour; changing either is a separate decision.
> - **Keying on the logical id normalises.** `Patient/1` and `Patient/1/_history/2` are now one
>   element where full-`IdElement` comparison called them two, and a HAPI resource and the
>   `ClassInstance` of the same resource now unify. Both follow from "key on `(resourceType,
>   logical id)`" as written, both match what `StratumValueWrapper` does after Part 1, and both are
>   semantic changes to state plainly rather than fold into a performance claim.
>
> **The risk to watch**: in `HashSetForCqlExpressionValues`, wrappers around *equal non-resource*
> raws now deduplicate where identity-based `add` kept both — two `ObservationAccumulator`s carrying
> identical entries, say. The measure-observation suites pass, but this is the change most likely to
> surprise if something downstream depended on multiplicity.

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

> **Corrected after Parts 1–2 were implemented and measured.** The original version of this section
> predicted that the fix would also eliminate the 2 435 ms/member `cql_eval` delta. That was wrong,
> and the error is worth recording because it was a reasoning error, not a measurement error:
> `SdeDef.accumulate` is reached from `R4MeasureProcessor.evaluateMeasure`, **not** from
> `evaluateMeasureWithCqlEngine`, so all of its cost was in report building from the outset. Parts
> 1–2 could only ever move that half, and the `cql_eval` delta is a different problem — evaluating
> the 38 SDE **CQL expressions**, which is the CQL-engine defect covered by a separate PRP in the
> engine repo.

Measured on the `AAB-Details` 400-member benchmark:

| | before | after Parts 1–2 | |
|---|---|---|---|
| `report_build_ms` (whole run) | 74 585 | **14 249** | **−81%** |
| SDE rendering delta, per member | 176 ms | **25 ms** | target was < 100 ms |
| `cql_eval_ms` (whole run) | 1 177 396 | 1 095 340 | −7% — not this PRP's target |
| evaluations completed | 266 of 400 (CR 4.11.1) | **400 of 400** | crash eliminated |

Parts 1–2 met their target. Profiling afterward put `SdeDef.accumulate` at **0.6% of execution
samples**, so this path is no longer material.

> **The crash was not removed by Parts 1–2**, though the run above shows it gone. Parts 1–2 stop
> converting during accumulation, but a report still renders SDE resources, and `buildSDE` converts
> each one — the same conversion, later and less often. What removed it from that run is the
> `5.3.0-fix-model-resolver-overrides` snapshot the benchmark built against, which resolves the type
> upstream. Part 3 is what removes it at this layer, on any engine version. Worth keeping straight:
> the 400-of-400 result is evidence for the *snapshot*, not for Parts 1–2.

Remaining wall-clock is dominated by two things outside Parts 1–3, both since diagnosed by JFR
profiling of the same run:

- **83.6% of CPU** sits in a deep recursive `hashCode()` over CQL values, driven by the engine's
  evaluated-resources set (`State.carryOverEvaluatedResourcesUpCallStack` 42.3%,
  `ExpressionDefEvaluator.internalEvaluate` 40.4%). That is a CQL-engine fix.
- Part 4's custom sets contribute the clinical-reasoning share of the same pattern.

**Part 4 expected effect**: `add`/`contains`/`remove` go from O(n) — with each comparison walking a
resource graph — to O(1). Set construction goes from O(n² × graph) to O(n). No standalone benchmark
figure is offered here because the cost is currently masked by the engine-side hashing; measure it
after the engine fix lands, or in isolation with a microbenchmark over a synthetic population set.

Memory: reduced transient allocation during accumulation. Note that GC was measured at only
6 630 ms of pause across a 160 s recording, so allocation pressure is **not** what limits parallelism
under Spark — an earlier hypothesis in this document's history that the profile disproved.

## Testing Strategy

### Correctness
- **`NestedBackboneSdeTest` (existing, from `3a82da9c`) must stay green.** It is the regression
  guard for the conversion path and covers depth-1 and depth-2 backbone elements. *Its helper needed
  one change: the accumulated value is now a `ClassInstance`, so the test converts it the way the
  report builders do. The conversion path it guards is unchanged.*
- **New**: an SDE returning `MedicationDispense` resources, asserting the bound-code `status` field
  round-trips. This is red before Part 3 and green after — on CQL 5.2.0. See the Part 3 note: it is
  green either way on the 5.3.0 snapshot.
- **New**: assert `StratumValueWrapper.getKey()` returns the identical string for a given resource
  whether constructed from a `ClassInstance` or from a HAPI `IBaseResource`. This is the invariant
  Part 1 depends on and the one that would silently corrupt SDE grouping if the key format ever
  drifted between the two forms. *This is what caught the `getId`/`getIdPart` error in the Solution
  Overview: written against `getId` it fails, and the failure is the real one.*
- **DSTU3**: `Dstu3MeasureReportBuilder` has **no** `ClassInstance` branch — it calls
  `getValueAsString()` / `getKey()` directly. Part 1's additions to those two methods are what keep
  it working. *Descoped: R4 is the target, so no DSTU3 test was added. Part 1's DSTU3 path is
  nonetheless intact — `isFhirResource(ClassInstance)` recognises DSTU3 resource names, so a
  resource-valued DSTU3 SDE still renders as its id, and untested is not the same as unhandled.*

### Part 4
- **`add` then `contains` agree for every value shape the sets hold** — HAPI resource, engine-native
  resource, CQL `Date`, CQL `Decimal`, plain `String`. The `Decimal` case fails before Part 4.
- **Engine-native resources with the same id are one element**, including when they differ in
  content. Fails before Part 4 under both old relations.
- **A HAPI resource and the `ClassInstance` of the same resource are one element.**
- **`retainAll` / `removeAll` against a plain `List`** — the `MeasureMultiSubjectEvaluator:802,809`
  shape — intersect in the set's own relation.
- **`HashSetForCqlExpressionValuesTest`** (new file) covers the same ground through the wrapper:
  dedup by wrapped resource, `contains` / `remove` accepting a raw value or a HAPI resource,
  `retainAll` against both a wrapper set and a plain list of raws.

### Performance
- Benchmark `AAB-Details` (38 SDEs) against `AAB-Reporting` (0 SDEs) over a fixed cohort. The
  Details-minus-Reporting delta is the metric; it isolates SDE cost from everything else and both
  measures ship in the HEDIS 2025 certified content.
- Assert the delta does not scale with resource count per member — that is the property being
  restored.

## Implementation Checklist

> Marks below are from the diff on `ld-20260901-sde-lazy-conversion`, except the two Part 1 rows the
> benchmark alone supports. The full test suite and `spotlessCheck` pass on **both** CQL 5.2.0 and
> the 5.3.0 snapshot.

- [x] Part 1: skip eager conversion for `ClassInstance` resources in `normalizeEngineNativeValue`
- [x] Part 1: add `ClassInstance` branches to `getKey()`, `getDescription()`, `getValueAsString()`
- [x] Part 1: handle `getId()` returning null (fall back, do not propagate)
- [x] Part 1: stop hard-coding R4 — done with a version-agnostic `isFhirResource(ClassInstance)`
      rather than by threading a `FhirVersionEnum` the construction sites do not have
- [x] Part 1 (unplanned): key on `getIdPart`, not `getId` — see the Solution Overview correction
- [x] Part 2: memoise `getKey()`
- [x] Part 3: pass child definitions through `toFhirValue` recursion; derive target class from them
- [x] Part 3: remove the `parentName` string-replacement heuristic
- [x] Part 3 (unplanned): instantiate through HAPI so a bound code gets its `EnumFactory`
- [x] Part 4: key `HashSetForFhirResourcesAndCqlTypes` on `(resourceType, id)`; O(1) add/contains/remove
- [x] Part 4: extend type detection past `Date` — resolved by deleting `castToCqlTypeIfApplicable`
      and `castToResourceIfApplicable`, which keying leaves without callers
- [x] Part 4: unify the relation so `add`/`remove` and `contains`/`retainAll` cannot disagree
- [x] Part 4: decide whether `HashSetForCqlExpressionValues` collapses into the keyed class — it does
- [x] Part 4: verify the `Decimal.equals` vs `EqualEvaluator.equal` divergence is real — it is
- [x] Part 4 (unplanned): override `retainAll` **and** `removeAll`; the inherited versions delegate
      to the other collection's `contains`
- [x] Tests: `MedicationDispense.status` bound-code SDE
- [x] Tests: `ClassInstance` vs `IBaseResource` key equivalence
- [ ] Tests: DSTU3 resource-valued SDE — descoped, R4 is the target
- [x] Tests (Part 4): `add` then `contains` agree for every value shape the sets hold
- [x] Tests (Part 4): `retainAll` population intersection unchanged (`MeasureMultiSubjectEvaluator:802,809`)
- [ ] Benchmark: `AAB-Details` minus `AAB-Reporting` delta before/after — Parts 1–2 only; Parts 3–4
      are unmeasured, and the content does not live in this repo
- [ ] Confirm the Part 4 dedup risk: wrappers around equal non-resource raws now collapse

## Success Criteria

### Functional
- ✅ `NestedBackboneSdeTest` and the existing measure test suite pass. *One test helper changed:
  see Testing Strategy.*
- ✅ `AAB-Details` completes over 400 members with **zero** per-member evaluation failures
  (was 266 of 400). *Attributable to the CQL snapshot on that run; Part 3 secures it at this layer.*
- ✅ SDE observation output is byte-identical to current output where current output succeeds. *This
  is the criterion that forced `getIdPart` over `getId`.* **Part 4 is the exception and does not
  meet it**: keying on the logical id normalises versioned ids and unifies the HAPI and
  engine-native forms of a resource, both deliberate.

### Non-functional
- ✅ SDE cost per member on `AAB-Details` 25 ms (from 2 612 ms), against a target of 100 ms.
- ⏳ Effective parallelism on `AAB-Details` within 20% of `AAB-Reporting` on the same host —
  unmeasured, and now expected to be gated by the engine-side hashing rather than by this code.

### Code quality
- ✅ The `parentName` heuristic is deleted, not extended.
- ✅ No new version-specific hard-coding in the version-agnostic `common` package. *The pre-existing
  `convertToFhirR4IfNeeded` call is untouched and still R4-only.*

## Conclusion

The 42x regression is one line in a constructor: a full reflective FHIR reconstruction performed to
read an id that the source object already carried. Every component needed to fix it —
`ClassInstanceHelper.getId`, `ClassInstanceHelper.isFhirResource`, and the report builder's
`ClassInstance` branch — is already in the codebase; the eager conversion is what renders them
unreachable. Parts 1–2 are small and low-risk. Part 3 is the larger change, and it converts a class
of recurring crashes into a resolved one by asking HAPI what type a field is instead of guessing.

**All four parts are implemented on `ld-20260901-sde-lazy-conversion`.** What the work taught that
this document did not anticipate, in one place:

1. **Converting a `ClassInstance` loses the resource type from the id.** That single fact decided
   Part 1's key (`getIdPart`, not `getId`) and, later, made Part 4's id-keying a semantic change
   rather than a pure speedup. It was found by writing the equivalence test this document asked for,
   which is the argument for writing that kind of test first.
2. **The bound-code crash was already fixed upstream** in the CQL snapshot the benchmark used, so
   the 400-of-400 result is not evidence for Parts 1–2. Part 3 still earns its place: it stops the
   converter guessing rather than improving the guess.
3. **Both of Part 4's relations were wrong about the same resource**, not just inconsistent with each
   other. Structural equality and CQL `=` both call one resource two when its metadata differs, so
   id-keying is a correctness fix and not only an O(n²)-to-O(n) one.

Parts 3 and 4 are covered by tests but unmeasured. The remaining wall-clock is in the CQL engine.
