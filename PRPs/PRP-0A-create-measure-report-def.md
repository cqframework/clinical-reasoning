# PRP-0A: Create MeasureReportDef and Composed Classes

**Phase**: 1 - MeasureDef/MeasureReportDef Separation (Foundation)
**Dependencies**: None
**Estimated Size**: Large (800-1000 lines)
**Estimated Time**: 2-3 days
**Complexity**: Medium (copy-paste with structural changes)

---

## Goal

Create new MeasureReportDef class hierarchy that represents evaluation results, separate from measure definition. This is the mutable counterpart that holds all evaluation state.

---

## Key Concept

**Current MeasureDef** contains both:
- Measure structure (id, url, groups, populations, stratifiers) ← Will stay in MeasureDef
- Evaluation results (scores, stratum, subject resources, counts) ← Will move to MeasureReportDef

**After this PRP**:
- **MeasureDef** = Immutable FHIR Measure structure (what to evaluate)
- **MeasureReportDef** = Mutable evaluation results (evaluation state)

---

## Files to Create

### 1. MeasureReportDef.java
- **Location**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/MeasureReportDef.java`
- **Structure**: Copy current MeasureDef.java exactly
- **Purpose**: Mutable container for evaluation results

```java
public class MeasureReportDef {
    private final MeasureDef measureDef;  // NEW: Immutable reference to measure definition
    private final List<GroupReportDef> groups;
    private final List<SdeReportDef> sdes;
    private final List<String> errors;

    public MeasureReportDef(MeasureDef measureDef) {
        this.measureDef = measureDef;
        this.groups = new ArrayList<>();
        this.sdes = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public MeasureDef measureDef() {
        return this.measureDef;
    }

    // Delegate measure structure queries to measureDef
    public String id() {
        return measureDef.id();
    }

    public String url() {
        return measureDef.url();
    }

    public String version() {
        return measureDef.version();
    }

    public List<GroupReportDef> groups() {
        return this.groups;
    }

    public List<SdeReportDef> sdes() {
        return this.sdes;
    }

    public List<String> errors() {
        return this.errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }
}
```

### 2. GroupReportDef.java
- **Location**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/GroupReportDef.java`
- **Structure**: Copy current GroupDef.java
- **Key change**: Reference immutable GroupDef from MeasureDef, add mutable evaluation state

```java
public class GroupReportDef {
    private final GroupDef groupDef;  // NEW: Reference to immutable definition
    private final List<StratifierReportDef> stratifiers;
    private final List<PopulationReportDef> populations;

    // Mutable evaluation state
    private Double score;

    public GroupReportDef(GroupDef groupDef) {
        this.groupDef = groupDef;
        this.stratifiers = new ArrayList<>();
        this.populations = new ArrayList<>();
    }

    // Delegate structure queries to groupDef
    public String id() {
        return groupDef.id();
    }

    public ConceptDef code() {
        return groupDef.code();
    }

    public MeasureScoring measureScoring() {
        return groupDef.measureScoring();
    }

    // Mutable state accessors
    public List<PopulationReportDef> populations() {
        return this.populations;
    }

    public List<StratifierReportDef> stratifiers() {
        return this.stratifiers;
    }

    public Double getScore() {
        return this.score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getMeasureScore() {
        if (this.score != null && this.score >= 0) {
            if (groupDef.isIncreaseImprovementNotation()) {
                return this.score;
            } else {
                return 1 - this.score;
            }
        }
        return null;
    }
}
```

### 3. PopulationReportDef.java
- **Location**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/PopulationReportDef.java`
- **Structure**: Copy mutable parts of current PopulationDef.java

```java
public class PopulationReportDef {
    private final PopulationDef populationDef;  // Reference to immutable definition

    // Mutable evaluation state (moved from PopulationDef)
    protected Set<Object> evaluatedResources;
    protected Map<String, Set<Object>> subjectResources = new HashMap<>();

    public PopulationReportDef(PopulationDef populationDef) {
        this.populationDef = populationDef;
    }

    // Delegate structure queries
    public String id() {
        return populationDef.id();
    }

    public ConceptDef code() {
        return populationDef.code();
    }

    public MeasurePopulationType type() {
        return populationDef.type();
    }

    public String expression() {
        return populationDef.expression();
    }

    // Mutable state methods (copied from current PopulationDef)
    public Set<Object> getEvaluatedResources() {
        if (this.evaluatedResources == null) {
            this.evaluatedResources = new HashSetForFhirResourcesAndCqlTypes<>();
        }
        return this.evaluatedResources;
    }

    public Map<String, Set<Object>> getSubjectResources() {
        return subjectResources;
    }

    public void addResource(String key, Object value) {
        subjectResources
            .computeIfAbsent(key, k -> new HashSetForFhirResourcesAndCqlTypes<>())
            .add(value);
    }

    public int getCount() {
        if (populationDef.type() == MeasurePopulationType.MEASUREOBSERVATION) {
            return countObservations();
        }

        if (populationDef.isBooleanBasis()) {
            return getSubjects().size();
        } else {
            return getAllSubjectResources().size();
        }
    }

    // ... other mutable methods from PopulationDef ...
}
```

### 4. StratifierReportDef.java
- **Location**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratifierReportDef.java`
- **Structure**: Copy mutable parts of current StratifierDef.java
- **Key change**: Reference immutable StratifierDef, hold mutable stratum list and results

### 5. StratumReportDef.java
- **Location**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratumReportDef.java`
- **Structure**: Copy current StratumDef.java
- **Key change**: Add score field (mutable)

### 6. StratumPopulationReportDef.java
- **Location**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/StratumPopulationReportDef.java`
- **Structure**: Copy current StratumPopulationDef.java

### 7. SdeReportDef.java
- **Location**: `cqf-fhir-cr/src/main/java/org/opencds/cqf/fhir/cr/measure/common/SdeReportDef.java`
- **Structure**: Copy mutable parts of current SdeDef.java

---

## Class Hierarchy Summary

### Report classes (NEW - mutable):
```
MeasureReportDef (contains MeasureDef reference)
├── GroupReportDef (contains GroupDef reference)
│   ├── PopulationReportDef (contains PopulationDef reference)
│   └── StratifierReportDef (contains StratifierDef reference)
│       └── StratumReportDef
│           └── StratumPopulationReportDef
└── SdeReportDef (contains SdeDef reference)
```

### Definition classes (existing - will become immutable in PRP-0B):
```
MeasureDef (pure measure structure)
├── GroupDef (pure group structure)
│   ├── PopulationDef (pure population structure)
│   └── StratifierDef (pure stratifier structure)
└── SdeDef (pure SDE structure)
```

---

## Implementation Strategy

1. **Copy existing Def classes** to create ReportDef classes
2. **Add MeasureDef reference** to MeasureReportDef constructor
3. **Add corresponding Def references** to each ReportDef class
4. **Delegate structure queries** to Def references (id(), code(), etc.)
5. **Keep mutable state** in ReportDef classes (scores, resources, counts)
6. **Ensure no breaking changes** - existing code continues to work

---

## Success Criteria

✅ All MeasureReportDef classes created and compile
✅ Each ReportDef contains reference to corresponding Def
✅ Mutable state moved from Def to ReportDef classes
✅ Structure queries delegate to Def references
✅ No breaking changes to existing code yet
✅ Code formatting passes (`./mvnw spotless:apply`)

---

## Testing

- **Unit tests**: Constructor tests for each ReportDef class
- **Integration tests**: Not yet - classes created but not wired in
- **Next PRP**: PRP-0B will make Def classes immutable

---

## Notes

- This PRP creates the new classes but doesn't integrate them yet
- Existing code continues to use mutable MeasureDef
- PRP-0B will strip mutable state from Def classes
- PRP-0C will wire ReportDef into evaluation flow
