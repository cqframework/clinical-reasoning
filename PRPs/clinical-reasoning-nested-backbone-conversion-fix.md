# PRP: Fix `CqlFhirParametersConverter` for nested backbone elements (upstream, clinical-reasoning)

## Executive Summary

`CqlFhirParametersConverter.toFhirValue` cannot convert a CQL `ClassInstance` back to HAPI FHIR when the
value is a **backbone element nested inside another backbone element** — for example
`ExplanationOfBenefit.item.adjudication`. It throws:

```
java.lang.IllegalArgumentException: Could not resolve inner FHIR type: AdjudicationComponent
```

The defect is a disambiguation heuristic that guesses HAPI's inner-class name from the **immediate
parent element's** type name, when HAPI actually flattens every backbone element into a direct inner
class of the **owning resource**. The guess produces `ItemComponent$AdjudicationComponent`, which has
never existed, and the code throws rather than falling back.

**Blast radius.** Any measure whose supplemental data elements or stratifiers surface a doubly-nested
backbone element fails *per subject*. In our case (NCQA HEDIS 2025 `AAB-Details`, which declares 38 SDEs,
10 of them returning `ExplanationOfBenefit`) this is a **100% subject failure rate** on real claims data.

**This is a regression introduced by the CQL v5 adoption.** `ClassInstanceHelper` and this error string do
not exist in clinical-reasoning 4.8.0; they arrive with 4.9.0 ("Update to CQL 5.0", PR #1058). CR 4.8.0
evaluates the same measure against the same data with no errors.

**Still present on `main`** as of this writing — verified at
`CqlFhirParametersConverter.java:429-440` (the heuristic) and `:500`/`:503` (the recursion that feeds it
the wrong name). CR 4.11.0 does **not** fix it; its `StratumValueWrapper` change actually routes *more*
values through the failing converter.

**No upstream issue exists** — searches for "inner FHIR type" and "AdjudicationComponent" return nothing.
Filing one is step 1.

**Deliverable:** an issue plus a PR against `cqframework/clinical-reasoning` `main`.

## Reproduction

### Minimal (unit level, preferred for the PR)

Round-trip any resource carrying a backbone-inside-a-backbone through the converter:

```java
// HAPI -> CQL ClassInstance -> HAPI
var eob = new ExplanationOfBenefit();
var item = eob.addItem();
item.setSequence(1);
item.addAdjudication().setAmount(new Money().setValue(new BigDecimal("100.00")).setCurrency("USD"));

var cqlValue = modelResolver.toCqlValue(eob, false);
var roundTripped = converter.toFhirValue((Value) cqlValue);   // throws today
```

`ExplanationOfBenefit.item.adjudication` is depth-2. Depth-1 (`ExplanationOfBenefit.item`) works, which is
why this went unnoticed.

### Integration level (in this repo — added, currently red)

`cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/NestedBackboneSdeTest.java`, backed by a
dedicated IG at `cqf-fhir-cr/src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/NestedBackboneSde/`:

```
input/cql/NestedBackboneSde.cql
input/resources/library/NestedBackboneSde.json
input/resources/measure/NestedBackboneSde.json
input/tests/patient/patient-nested.json
input/tests/patient/patient-flat.json
input/tests/explanationofbenefit/eob-nested.json   <- item.adjudication (depth 2)
input/tests/explanationofbenefit/eob-flat.json     <- item only        (depth 1)
```

A boolean-basis cohort measure with one SDE, `define "SDE Explanation Of Benefit": [ExplanationOfBenefit]`.
Returning whole resources from an SDE is enough: `SdeDef.accumulate()` wraps every returned value in
`StratumValueWrapper`, which converts the engine-native `ClassInstance` back to HAPI.

Three tests:

| Test | Subject / type | On `main` |
|---|---|---|
| `depthOneBackboneElementConvertsForSupplementalData` | `patient-flat`, subject report | **passes** |
| `nestedBackboneElementConvertsForSupplementalData` | `patient-nested`, subject report | **fails** |
| `nestedBackboneElementConvertsForSupplementalDataInPopulationReport` | both, population report | **fails** |

Both failures are the exact PRP trace, reproduced verbatim in-repo:

```
java.lang.IllegalArgumentException: Could not resolve inner FHIR type: AdjudicationComponent
  at CqlFhirParametersConverter.toFhirValue(CqlFhirParametersConverter.java:438)
  at CqlFhirParametersConverter.toFhirValue(CqlFhirParametersConverter.java:500)   <- depth 2
  at CqlFhirParametersConverter.toFhirValue(CqlFhirParametersConverter.java:500)   <- depth 1
  at CqlFhirParametersConverter.toFhirValue(CqlFhirParametersConverter.java:410)   <- entry, parentName=null
  at CqlFhirParametersConverter.convertToFhirIfNeeded(CqlFhirParametersConverter.java:405)
  at ClassInstanceHelper.convertToFhirR4IfNeeded(ClassInstanceHelper.java:57)
  at StratumValueWrapper.normalizeEngineNativeValue(StratumValueWrapper.java:40)
  at StratumValueWrapper.<init>(StratumValueWrapper.java:26)
  at SdeDef.accumulate(SdeDef.java:76)
  at MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(MeasureMultiSubjectEvaluator.java:123)
  at MeasureEvaluationResultHandler.processResults(MeasureEvaluationResultHandler.java:83)
  at R4MeasureProcessor.evaluateMeasureCaptureDef(R4MeasureProcessor.java:236)
```

Three things this test settled that the analysis above left open:

1. **A synthetic EOB *is* sufficient**, as long as the SDE selects it directly. The earlier "not sufficient"
   note is specific to `AAB-Details`, whose SDEs filter on diagnosis codes and service dates; a purpose-built
   measure sidesteps that. The integration test needs no real claims data.
2. **The exception is not reported in-band here.** `SdeDef.accumulate()` runs in
   `postEvaluationMultiSubject`, *outside* the per-subject `try/catch` in
   `MeasureEvaluationResultHandler.processResults` (`:57-81`). So on the SDE path nothing is collected into
   `measureDef.addError`, no `OperationOutcome` is contained, and no MeasureReport is built at all — the
   `IllegalArgumentException` propagates out of `evaluateMeasureCaptureDef`. The "assert on `status`" watch
   item still holds for the *stratifier* path and for CQL errors raised per subject, but a test on the SDE
   path must expect a thrown exception, not `status = ERROR`.
3. **Depth-1 really is unaffected**, confirmed end-to-end and not just by reading the substitution table:
   `eob-flat` (an `item` with no `adjudication`) converts cleanly on `main` today.

Option B was applied locally against this test and all three pass; reverting it returns the two failures.
The `ownerName` computation goes just above the `definition.getChildren()` loop
(`CqlFhirParametersConverter.java:493`), with both recursion sites (`:500`, `:503`) passing `ownerName`
instead of `typeName`. The fix is *not* committed here — this commit carries the red test only, so the
full `:cqf-fhir-cr` / `:cqf-fhir-cql` regression sweep under Option B is still owed by the fix PR.

### Full-stack (how we hit it)

```
--measureId=AAB-Details --terminology=hedis-2025 --inputMode=iceberg --reportType=subject
```

against a warehouse containing `ExplanationOfBenefit` resources with `item.adjudication` populated. Every
subject fails identically:

```
java.lang.IllegalArgumentException: Could not resolve inner FHIR type: AdjudicationComponent
  at CqlFhirParametersConverter.toFhirValue(CqlFhirParametersConverter.java:437)
  at CqlFhirParametersConverter.toFhirValue(CqlFhirParametersConverter.java:499)   <- depth 2
  at CqlFhirParametersConverter.toFhirValue(CqlFhirParametersConverter.java:499)   <- depth 1
  at CqlFhirParametersConverter.toFhirValue(CqlFhirParametersConverter.java:409)   <- entry, parentName=null
  at CqlFhirParametersConverter.convertToFhirIfNeeded(CqlFhirParametersConverter.java:404)
  at ClassInstanceHelper.convertToFhirR4IfNeeded(ClassInstanceHelper.java:57)
  at StratumValueWrapper.normalizeEngineNativeValue(StratumValueWrapper.java:40)
  at StratumValueWrapper.<init>(StratumValueWrapper.java:26)
  at SdeDef.accumulate(SdeDef.java:76)
  at MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(MeasureMultiSubjectEvaluator.java:123)
  at MeasureEvaluationResultHandler.processResults(MeasureEvaluationResultHandler.java:83)
  at R4MeasureProcessor.evaluateMeasureCaptureDef(R4MeasureProcessor.java:236)
```

**A synthetic EOB is not sufficient to reproduce end-to-end.** The EOB must actually be *selected* by one
of the measure's SDE expressions (correct diagnosis codes, service dates inside the measurement period);
otherwise the SDE returns empty and the converter is never called. This is why unit-level round-trip is
the right test.

## Root cause

Two pieces of code, both on `main`.

**1. The heuristic** (`CqlFhirParametersConverter.java:429-440`):

```java
if (StringUtils.isNotBlank(parentName)
        && !clazz.isEnum()
        && clazz.getName().contains("$")                                    // a backbone element
        && !clazz.getEnclosingClass().getSimpleName().equals(parentName)) {
    var correctClassName =
            clazz.getName().replace(clazz.getEnclosingClass().getSimpleName(), parentName);
    try {
        clazz = Class.forName(correctClassName);
    } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("Could not resolve inner FHIR type: " + typeName);
    }
}
```

**2. The recursion that supplies `parentName`** (`:500` and `:503`) passes the **current node's own type
name**:

```java
child.getMutator().addValue(instance, toFhirValue(item, typeName));
```

### Why the heuristic exists

`modelResolver.resolveType()` looks types up by bare local name, and backbone names are not unique. R4 has
**two** `AdjudicationComponent` classes:

```
ClaimResponse$AdjudicationComponent
ExplanationOfBenefit$AdjudicationComponent
```

So `resolveType("AdjudicationComponent")` can only return one of them, and the heuristic exists to correct
the choice using context. The intent is right.

### Why it is wrong

HAPI **flattens** backbone elements into direct inner classes of the resource — it does not nest them by
path. Verified against `org.hl7.fhir.r4`:

```
ExplanationOfBenefit$ItemComponent
ExplanationOfBenefit$AdjudicationComponent      <- NOT ItemComponent$AdjudicationComponent
ExplanationOfBenefit$DetailComponent
```

Walking `ExplanationOfBenefit → item → adjudication`:

| Depth | `typeName` | `parentName` received | Substitution attempted | Result |
|---|---|---|---|---|
| 1 | `ExplanationOfBenefit` | `null` | skipped | ok |
| 2 | `ItemComponent` | `ExplanationOfBenefit` | `ExplanationOfBenefit$ItemComponent` | ok |
| 3 | `AdjudicationComponent` | **`ItemComponent`** | **`ItemComponent$AdjudicationComponent`** | **ClassNotFound → throw** |

The correct disambiguator is the **owning resource** (`ExplanationOfBenefit`), which stays constant for the
whole traversal. The code instead passes the immediate parent, which is only accidentally correct at
depth 1.

### The sharpest detail

The bug fires **even when `resolveType` already returned the correct class**. If it hands back
`ExplanationOfBenefit$AdjudicationComponent`, the guard still compares its enclosing simple name
(`ExplanationOfBenefit`) against `parentName` (`ItemComponent`), sees a mismatch, attempts the bogus
substitution, and throws — discarding a correct answer on the way past. Any backbone nested inside another
backbone fails unconditionally, regardless of whether name resolution was ambiguous at all.

## Goals

- `toFhirValue` correctly converts backbone elements at arbitrary nesting depth.
- Ambiguous bare names (`AdjudicationComponent`, `ItemComponent`, `DetailComponent`, …) resolve to the
  class belonging to the **owning resource**.
- A regression test that would have caught this, at unit level, with no measure or patient data.

## Non-goals

- Redesigning `resolveType`. Bare-name lookup stays; only the disambiguation context changes.
- Anything in `StratumValueWrapper`, `SdeDef` or `ClassInstanceHelper` — they are correct callers of a
  broken converter.
- The separate performance question about the CQL v5 value model (see "Adjacent findings").

## Approach

### Recommended: propagate the owning resource name (Option B)

Thread the **nearest enclosing non-inner type** through the recursion instead of the immediate parent's
type name. The owner is established at the first non-backbone node and then propagated unchanged.

Sketch — compute an owner once per frame and recurse with it:

```java
// The disambiguator is the OWNING RESOURCE, because HAPI declares every backbone element as a direct
// inner class of its resource (ExplanationOfBenefit$AdjudicationComponent), never nested by path. A
// backbone frame keeps the owner it was given; a non-backbone frame becomes the new owner.
var ownerName = clazz.getName().contains("$") && StringUtils.isNotBlank(parentName) ? parentName : typeName;
...
child.getMutator().addValue(instance, toFhirValue(item, ownerName));       // was: typeName
child.getMutator().addValue(instance, toFhirValue(elementValue, ownerName)); // was: typeName
```

Re-walking the table above, depth 3 now receives `ExplanationOfBenefit`, substitutes to
`ExplanationOfBenefit$AdjudicationComponent`, and succeeds. Depths 1 and 2 are unchanged, so existing
behaviour is preserved.

The reset on non-backbone frames matters for resources nested inside resources (`Bundle.entry.resource`,
contained resources): the owner must become the inner resource rather than staying the outer one.

### Fallback if maintainers want the smallest possible diff (Option A)

Keep `resolveType`'s answer when the substitution fails, instead of throwing:

```java
try { clazz = Class.forName(correctClassName); }
catch (ClassNotFoundException e) { /* keep the resolved class */ }
```

Fixes the reported symptom in three lines, but leaves the heuristic wrong: when the bare name really is
ambiguous it can retain the wrong class, converting a clear `IllegalArgumentException` into a
`ClassCastException` from the child mutator. Offer this only if Option B is resisted.

### Worth raising in the issue, not necessarily in this PR (Option C)

Remove the name-guessing entirely. The parent's `BaseRuntimeElementCompositeDefinition` already knows each
child's declared type — `definition.getChildByName(elementName)` — so the correct class can be read rather
than reconstructed from strings. This is the design-level fix; it is larger, and choice types (`value[x]`)
need reconciling against the CQL type. Mention it as a follow-up so the maintainers can weigh it.

## Test strategy

Two layers. The integration layer already exists (see "Integration level" above) and is red on `main`; the
unit layer is what the upstream PR should lead with.

### Integration (done)

`NestedBackboneSdeTest` + the `NestedBackboneSde` IG. It proves the failure is reachable through
`$evaluate-measure` rather than only through a hand-built converter call, pins the depth-1/depth-2 split, and
asserts the adjudication survives the round trip (category coding, `Money` amount and currency) via
`then().def().measureDef()` — the converted resource is an *evaluated* resource, so the report references it
rather than containing it, and the def is the only place the converted object is reachable.

### Unit (still to add)

Add to `cqf-fhir-cql/src/test/java/org/opencds/cqf/fhir/cql/engine/parameters/CqlFhirParametersConverterTests.java`
(299 lines today; no existing coverage of nested backbone conversion).

1. **The regression test** — round-trip an `ExplanationOfBenefit` carrying `item.adjudication` through
   `modelResolver.toCqlValue(...)` → `converter.toFhirValue(...)`, asserting the adjudication survives with
   its category coding and `Money` amount intact. Fails on `main` with
   `Could not resolve inner FHIR type: AdjudicationComponent`; passes with the fix.
2. **The disambiguation test** — a resource using a bare name that collides across resources, asserting the
   result is the *owning resource's* class and not merely non-null. This is what stops Option A being
   mistaken for a complete fix, and what pins Option B's actual contract.
3. **A depth-1 guard** — a simple `EOB.item` round-trip, so the change is shown not to regress the case the
   heuristic currently gets right.

Round-tripping is the general shape worth arguing for: it would have caught this class of bug at any depth,
for any resource.

## Verification

### Upstream gate

```bash
./gradlew :cqf-fhir-cql:test --tests CqlFhirParametersConverterTests
./gradlew spotlessCheck    # Palantir Java Format; runs in CI on PRs
./gradlew build            # full build incl. checkstyle
```

The integration reproduction, which is the one that must flip from red to green:

```bash
./gradlew :cqf-fhir-cr:test --tests "org.opencds.cqf.fhir.cr.measure.r4.NestedBackboneSdeTest"
```

Also run the measure-side suites in full, since `SdeDef`/`StratumValueWrapper` are the real callers:

```bash
./gradlew :cqf-fhir-cr:test
```

### Downstream gate (proves it fixes *our* failure)

clinical-reasoning supports local publication, so the fix can be validated against the real HEDIS content
before the PR merges:

```bash
# in clinical-reasoning
./gradlew publishToMavenLocal            # publishes 4.x.y-SNAPSHOT

# in cqis-spark: point the catalog at the snapshot, add mavenLocal() to settings.gradle.kts repositories
# gradle/libs.versions.toml -> clinical-reasoning = "<snapshot version>"
./gradlew :cqis-spark-core:integrationTest --tests "*BundledContentCompilationIT*"
```

Then the decisive check — a real member compartment, not synthetic FHIR:

```bash
--measureId=AAB-Details --terminology=hedis-2025 --inputMode=iceberg \
  --patientIds=<member with a pharmacy claim that the SDEs actually select>
```

Expect `MeasureReport.status = COMPLETE` and populated SDE extensions, where `main` throws per subject.

### Acceptance criteria

- [ ] Upstream issue filed with the stack trace and the depth-2 explanation.
- [x] Integration reproduction committed (`NestedBackboneSdeTest` + `NestedBackboneSde` IG), red on `main`
      with the exact `Could not resolve inner FHIR type: AdjudicationComponent` trace.
- [ ] `toFhirValue` converts `ExplanationOfBenefit.item.adjudication` without throwing.
- [ ] Ambiguous bare names resolve to the owning resource's class, asserted by test.
- [x] Depth-1 conversion unchanged — verified green on `main` by
      `depthOneBackboneElementConvertsForSupplementalData`, and still green under Option B.
- [ ] `./gradlew build` and `spotlessCheck` clean on clinical-reasoning `main`.
- [ ] `AAB-Details` evaluates to `status = COMPLETE` for a real member against a locally published build.
- [ ] PR opened against `cqframework/clinical-reasoning` `main`.

## Contribution notes

- Repo conventions live in `AGENTS.md` / `ARCHITECTURE.md` at the CR repo root.
- Formatting is **Palantir Java Format** via `./gradlew spotlessApply`; Checkstyle runs as part of `build`.
- CI parses `Depends-On: org/repo#branch` directives from PR descriptions to link upstream repos. **Not
  needed here** — this fix is entirely inside `cqf-fhir-cql`, with no CQL-engine change.
- If a companion engine change ever becomes necessary, `local.properties` (`cql.engine.path=...`) links a
  local `clinical_quality_language` checkout.

## Watch items

- **CR 4.11.0 makes exposure worse, not better.** Its `StratumValueWrapper.getValueAsString` change adds a
  `normalizeEngineNativeValue` call for list elements, pushing more values through the failing converter.
  Anyone testing on 4.11.0 should expect the same or broader failure, not an improvement.
- **Depth-2 is not exotic.** `Claim.item.detail`, `ExplanationOfBenefit.item.detail`,
  `ExplanationOfBenefit.addItem.detail`, and `*.item.adjudication` are all common in claims data. Expect
  other consumers to be hitting this without having diagnosed it — a further argument for Option B over A.
- **Failures are reported in-band, not thrown, one level up — but not on the SDE path.**
  `CompositeEvaluationResultsPerMeasure` collects CQL errors and `R4MeasureReportBuilder.setReportStatus`
  turns them into a contained `OperationOutcome` with `status = ERROR`, so a caller asserting only "a report
  came back" will not notice. That holds for anything raised inside the per-subject loop of
  `MeasureEvaluationResultHandler.processResults`. It does **not** hold for SDE conversion:
  `SdeDef.accumulate()` runs in `postEvaluationMultiSubject`, after that loop and outside its `try/catch`,
  so the `IllegalArgumentException` escapes `evaluateMeasureCaptureDef` and no report is produced at all.
  Confirmed by `NestedBackboneSdeTest`, where the failure arrives as a thrown exception out of `then()`.

## Adjacent findings (context only — do not fold into this PR)

- **CQL v5 appears materially slower for this workload.** A controlled local A/B on the same measure and
  data (`AAB-Reporting`, only the CR version flipped) measured ~44.5s on CR 4.8.0 versus ~65.5s on 4.10.3.
  Compile and context-build cost is identical between the versions (measured: 2.7s vs 2.7s for the
  `AAB-Reporting` closure), so the difference is per-member evaluation. Hypothesis: v5 marshals FHIR
  resources into engine-native `ClassInstance` values eagerly, which loses against v4's lazy reflective
  access in a workload that rejects most resources on a single property. Unprofiled — mention only if the
  maintainers ask why we are on 4.8.0.
- **`FhirModelResolver.resolvePath` in engine 4.4.0** compiles a fresh `Regex` on every property access.
  Real, and removed by the v5 rewrite, but evidently outweighed by the marshalling cost above.
