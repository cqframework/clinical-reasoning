# PRP-0B: Refactor MeasureDef to Immutable

**Phase**: 1 - MeasureDef/MeasureReportDef Separation (Foundation)
**Dependencies**: PRP-0A
**Estimated Size**: Large (400-500 lines of deletions/modifications)
**Estimated Time**: 1-2 days
**Complexity**: Medium (surgical removal of mutable state)

---

## Goal

Strip all mutable state from MeasureDef and related classes, making them pure immutable representations of FHIR Measure structure.

---

## Key Changes

- **Remove ALL mutable state** from Def classes
- **Make all collections unmodifiable** (List.copyOf, Collections.unmodifiableList)
- **Remove setters and mutating methods**
- **Keep only structure/definition** - no evaluation results

---

## Files to Modify

### 1. MeasureDef.java
**Remove**:
- `errors` list (moves to MeasureReportDef)
- `addError()` method

**Make immutable**:
- `groups` and `sdes` lists → `List.copyOf()`

```java
public class MeasureDef {
    private final IIdType idType;
    @Nullable private final String url;
    private final String version;
    private final List<GroupDef> groups;  // Unmodifiable
    private final List<SdeDef> sdes;      // Unmodifiable

    public MeasureDef(
            IIdType idType,
            @Nullable String url,
            String version,
            List<GroupDef> groups,
            List<SdeDef> sdes) {
        this.idType = idType;
        this.url = url;
        this.version = version;
        this.groups = List.copyOf(groups);  // Immutable copy
        this.sdes = List.copyOf(sdes);      // Immutable copy
    }

    // Only getters, no setters or mutating methods
    public String id() {
        return this.idType.toUnqualifiedVersionless().getIdPart();
    }

    public List<GroupDef> groups() {
        return this.groups;  // Already immutable
    }

    public List<SdeDef> sdes() {
        return this.sdes;  // Already immutable
    }
}
```

### 2. GroupDef.java
**Remove**:
- `score` field (moves to GroupReportDef)
- `setScore()` method
- `getScore()` method
- `getMeasureScore()` method (moves to GroupReportDef)

**Make immutable**:
- `stratifiers` and `populations` lists

**Keep only**:
- id, code, measureScoring, improvementNotation, populationBasis
- Query methods: `hasPopulationType()`, `getSingle()`, `getPopulationDefs()`, etc.

### 3. PopulationDef.java
**Remove ALL mutable state**:
- `evaluatedResources` field
- `subjectResources` field
- `addResource()` method
- `getEvaluatedResources()` method
- `getSubjectResources()` method
- `getResourcesForSubject()` method
- `retainAllResources()` method
- `removeAllResources()` method
- `getCount()` method (moves to PopulationReportDef)
- `countObservations()` method
- `getAllSubjectResources()` method

**Keep only structure**:
- id, expression, code, type, populationBasis, criteriaReference, aggregateMethod
- Query methods: `isBooleanBasis()`, `getCriteriaReference()`, etc.

### 4. StratifierDef.java
**Remove**:
- `stratum` list (moves to StratifierReportDef)
- `results` map (moves to StratifierReportDef)
- `getStratum()` method
- `addAllStratum()` method
- `putResult()` method
- `getResults()` method
- `getAllCriteriaResultValues()` method

**Keep only structure**:
- id, code, expression, stratifierType, components
- Query methods: `isComponentStratifier()`, `isCriteriaStratifier()`, etc.

### 5. StratumDef.java
**Remove**:
- `score` field (moves to StratumReportDef)
- `setScore()` method
- `getScore()` method

**Keep only structure**:
- stratumPopulations, valueDefs, subjectIds, measureObservationCache
- Query methods: `isComponent()`, `getStratumPopulation()`, etc.

**Note**: StratumDef is unusual - it represents a single stratum, not a definition template.
The score still needs to move out, but the stratum populations are actual values.

### 6. SdeDef.java
**Review and remove** any mutable state if present.

---

## Expected Compilation Failures

After this PRP, code will compile with failures at usage sites:

```
Error: cannot find symbol - method addResource(String, Object)
Error: cannot find symbol - method getEvaluatedResources()
Error: cannot find symbol - method setScore(Double)
Error: cannot find symbol - method addError(String)
```

**These are expected** and will be fixed in PRP-0C when we wire in MeasureReportDef.

---

## Builder Compatibility

**IMPORTANT**: Ensure builders still work:
- `R4MeasureDefBuilder` should continue to build immutable MeasureDef
- `Dstu3MeasureDefBuilder` should continue to build immutable MeasureDef
- Builders construct lists and pass to MeasureDef constructor
- No changes to builder APIs

---

## Success Criteria

✅ MeasureDef and all composed classes are fully immutable
✅ No setters or mutating methods remain
✅ All collections are unmodifiable (List.copyOf, Collections.unmodifiableList)
✅ Builders still work to construct immutable instances
✅ No breaking changes to builder APIs
✅ Code compiles (with expected failures in usage sites - to be fixed in PRP-0C)
✅ Code formatting passes (`./mvnw spotless:check`)

---

## Testing

- **Unit tests**: May need updates for removed methods
- **Integration tests**: Will fail until PRP-0C - this is expected
- **Next PRP**: PRP-0C will wire MeasureReportDef into evaluation flow

---

## Notes

- This is a **breaking change** at usage sites - expected and intentional
- PRP-0C will fix all compilation failures
- Focus on **surgical removal** of mutable state - don't refactor other logic
- If unsure whether something is mutable state, leave it for now
