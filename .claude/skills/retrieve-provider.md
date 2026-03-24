# RetrieveProvider Architecture & Terminology Filter Modes

## Overview

The RetrieveProvider is the CQL engine's interface for fetching clinical data during expression evaluation. When CQL contains a retrieve expression like `[Observation: "LDL Cholesterol"]`, the engine delegates to a RetrieveProvider to find matching FHIR resources. The configuration of the retrieve provider — particularly its terminology filtering strategy — has a major impact on evaluation performance.

## Key Classes

### RetrieveSettings

**File**: `cqf-fhir-cql/src/main/java/org/opencds/cqf/fhir/cql/engine/retrieve/RetrieveSettings.java`

Configuration object controlling three independent dimensions of retrieve behavior:

| Setting | Enum | Default | Governs |
|---------|------|---------|---------|
| `searchParameterMode` | `SEARCH_FILTER_MODE` | `AUTO` | Context filtering (patient scoping), profile filtering, date filtering |
| `terminologyParameterMode` | `TERMINOLOGY_FILTER_MODE` | `AUTO` | How ValueSet/code-based filtering is applied |
| `profileMode` | `PROFILE_MODE` | `OFF` | Whether and how profile declarations are used for filtering |

`RetrieveSettings` uses a fluent builder pattern (setters return `this`) and has a copy constructor.

### BaseRetrieveProvider (Abstract)

**File**: `cqf-fhir-cql/src/main/java/org/opencds/cqf/fhir/cql/engine/retrieve/BaseRetrieveProvider.java`

Abstract base implementing `RetrieveProvider` from the CQL engine. Contains:

- **`populateTerminologySearchParams()`** (line 338): Builds FHIR search parameters for terminology-based filtering. Decides between `:in` modifier and inline code expansion.
- **`shouldUseInCodeModifier()`** (line 387): Returns true if mode is `USE_VALUE_SET_URL`, or if mode is `AUTO` and `:in` is detected as supported.
- **`filterByTerminology()`** (line 192): Creates an in-memory `Predicate<IBaseResource>` that extracts codes via FHIRPath and checks them against the ValueSet/code list.
- **`anyCodeInValueSet()`** (line 254): Per-resource membership check calling `terminologyProvider.in(code, valueSetInfo)`.

### RepositoryRetrieveProvider (Concrete)

**File**: `cqf-fhir-cql/src/main/java/org/opencds/cqf/fhir/cql/engine/retrieve/RepositoryRetrieveProvider.java`

The primary concrete implementation. The `retrieve()` method (line 47) builds a `SearchConfig` (search params + in-memory predicate), then executes `repository.search()` and applies the predicate filter.

**`configureTerminology()`** (line 116) is the central dispatch:

```java
switch (mode) {
    case FILTER_IN_MEMORY:
        config.filter = config.filter.and(filterByTerminology(...));
        break;
    case AUTO:
    case USE_INLINE_CODES:
    case USE_VALUE_SET_URL:
        populateTerminologySearchParams(config.searchParams, ...);
        break;
}
```

**`inModifierSupported()`** (line 149) returns false for `InMemoryFhirRepository` (which doesn't support `:in`).

### FederatedDataProvider

**File**: `cqf-fhir-cql/src/main/java/org/opencds/cqf/fhir/cql/engine/retrieve/FederatedDataProvider.java`

Composite provider wrapping multiple `RetrieveProvider` instances. Each is initialized with the same `RetrieveSettings`.

## TERMINOLOGY_FILTER_MODE Deep Dive

### Enum Values (in decreasing order of typical performance)

```java
public enum TERMINOLOGY_FILTER_MODE {
    AUTO,               // Detect from capability statements
    USE_VALUE_SET_URL,  // code:in=valueSetUrl
    USE_INLINE_CODES,   // code=system|value,system|value
    FILTER_IN_MEMORY    // CQL engine does the filter
}
```

### AUTO

**Behavior**: Falls through to `populateTerminologySearchParams()` (same code path as USE_INLINE_CODES/USE_VALUE_SET_URL). Within that method, calls `shouldUseInCodeModifier()` which checks if `:in` is supported — if yes, behaves like USE_VALUE_SET_URL; if no, behaves like USE_INLINE_CODES.

**Note**: The AUTO detection has a TODO indicating capability statement-based detection is not fully implemented. The base `inModifierSupported()` returns true by default (assuming `:in` is available), except `RepositoryRetrieveProvider` overrides it to return false for `InMemoryFhirRepository`.

**Query pattern**: Depends on detection — either `code:in=valueSetUrl` or `code=sys|c1,sys|c2,...`

### USE_VALUE_SET_URL

**Behavior**: Forces use of the FHIR `:in` search modifier. Sends a single search parameter with the ValueSet URL.

**Query pattern**: `Observation?code:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.201&subject=Patient/123`

**How it works**: The FHIR server is responsible for expanding the ValueSet and filtering resources server-side. No client-side ValueSet expansion or in-memory filtering occurs.

**Requirements**: The FHIR server must support the `:in` modifier on token search parameters.

### USE_INLINE_CODES

**Behavior**: Expands the ValueSet via `terminologyProvider.expand()`, then adds every code from the expansion as inline token search parameters.

**Query pattern**: `Observation?code=http://loinc.org|12345,http://loinc.org|67890,...&subject=Patient/123`

**How it works**:
1. Calls `terminologyProvider.expand(new ValueSetInfo().withId(valueSet))` — a single expansion call
2. Iterates the expansion, creating a `TokenParam` per code with system and code
3. Adds all tokens to the search parameter map (OR'd together by the FHIR server)
4. Server returns only resources matching one of the specified codes

**Requirements**: A functioning TerminologyProvider that can expand ValueSets. Server must support token search parameters with multiple OR'd values.

### FILTER_IN_MEMORY

**Behavior**: Sends no terminology search parameters to the server. Retrieves all resources of the requested type (scoped only by context/patient, dates, profile), then filters in Java.

**Query pattern**: `Observation?subject=Patient/123` (no code parameter)

**How it works**:
1. Adds a `Predicate<IBaseResource>` to the SearchConfig filter
2. The predicate evaluates FHIRPath on each resource to extract its code(s)
3. For each resource, calls `anyCodeMatch()` (direct code comparison) or `anyCodeInValueSet()` (which calls `terminologyProvider.in()` per code)
4. Resources that don't match are discarded after retrieval

**Requirements**: None — works with any repository regardless of search capabilities.

## Performance Analysis: FILTER_IN_MEMORY vs USE_INLINE_CODES

### When USE_INLINE_CODES is dramatically faster

The performance advantage scales with **selectivity x volume x retrieve count x population size**.

**Dominant factor: client data volume and selectivity.** A CQL retrieve like `[Observation: "HbA1c"]` with a ValueSet containing ~5 codes:
- **USE_INLINE_CODES**: Server returns only the 2-3 matching Observations via indexed search
- **FILTER_IN_MEMORY**: Server returns all 10,000+ Observations for the patient; each is deserialized, FHIRPath-evaluated, and checked against the terminology provider

**Multiplying factors from measure structure:**
- CMS 2024 measures typically have 5-10 retrieve expressions per measure, each with a selective ValueSet
- Multiple retrieves often target the same resource type (e.g., two different MedicationRequest retrieves in CMS104/DischargedonAntithromboticTherapyFHIR)
- Each retrieve independently suffers the FILTER_IN_MEMORY penalty
- Population measures multiply the per-patient cost across all subjects

**FILTER_IN_MEMORY hidden costs:**
- `terminologyProvider.in()` is called per-resource per-retrieve — if backed by a remote terminology server, this is N network calls where N = total resources fetched (not just matching ones)
- FHIRPath evaluation on every resource to extract codes is CPU-intensive
- Deserialization of large result sets consumes significant memory

### When FILTER_IN_MEMORY can be faster

- **Huge ValueSet expansions with low selectivity**: If a ValueSet expands to thousands of codes and most resources match anyway, the expansion cost is wasted and the query URL becomes enormous
- **Tiny datasets**: If a patient has only a handful of resources of a given type, the fixed cost of ValueSet expansion exceeds the savings from filtering
- **Expensive/unavailable ValueSet expansion**: If `terminologyProvider.expand()` is backed by a slow remote server, the expansion itself is the bottleneck; `in()` may use a cheaper code path (e.g., local cache)
- **Many overlapping retrieves covering most data**: If multiple retrieves collectively cover nearly all resources of a type, one unfiltered fetch may be cheaper than N filtered ones (especially with HTTP result caching)
- **FHIR server poorly optimized for multi-token OR**: Some servers handle long `code=a,b,c,...` queries poorly compared to a simple compartment scan

### The measure vs data contribution

**What the measures contribute (fixed, structural)**:
- Number of retrieve expressions (typically 5-10 per CMS measure)
- Selectivity of ValueSets (CMS measures are clinically narrow — tens of codes, not thousands)
- Number of resource types queried and whether multiple retrieves hit the same type
- This creates the *conditions* for a performance gap but doesn't determine its magnitude

**What the client's data contributes (variable, dominant)**:
- **Resource volume per patient per type**: The ratio of total resources to matching resources is the primary driver. A patient with 50 Observations vs 10,000 Observations experiences vastly different impact.
- **Population size**: Every per-patient inefficiency multiplies across all subjects in a population measure
- **FHIR server indexing quality**: Determines how fast filtered vs unfiltered queries execute
- **Terminology service performance**: Determines the cost of `expand()` (USE_INLINE_CODES) vs per-resource `in()` (FILTER_IN_MEMORY)
- **Data distribution/skew**: If clinical data is dominated by high-volume types (Observations, DiagnosticReports) but the measure retrieves from low-volume types (Encounters), the impact is smaller

**Rule of thumb**: The CMS 2024 measures provide a ~5-10x structural multiplier (number of retrieve opportunities). The client's data determines whether each opportunity produces a 2x or 1000x difference — making the data the overwhelmingly dominant factor in observed performance.

## Configuration Points

### Default

```java
// RetrieveSettings.java
private TERMINOLOGY_FILTER_MODE terminologyParameterMode = TERMINOLOGY_FILTER_MODE.AUTO;
```

### Test configuration (HAPI integration)

```java
// TestCrR4Config.java - uses FILTER_IN_MEMORY for simplicity
retrieveSettings.setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
```

### CLI configuration

```java
// Utilities.java - also uses FILTER_IN_MEMORY
retrieveSettings.setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
```

### All test infrastructure

Every test helper class (Measure.java, MultiMeasure.java, TestPlanDefinition, TestCql, etc.) defaults to `FILTER_IN_MEMORY`. This is because tests use `InMemoryFhirRepository` which doesn't support search parameter filtering.

### EvaluationSettings

`RetrieveSettings` is carried inside `EvaluationSettings`, which is passed to `Engines.forRepository()` when creating CQL engine instances. This is the primary integration point for configuring terminology filter mode in production.

## Decision Guide: Choosing a Mode

| Scenario | Recommended Mode | Reason |
|----------|-----------------|--------|
| HAPI FHIR server with `:in` support | `USE_VALUE_SET_URL` or `AUTO` | Maximum server-side filtering, minimal data transfer |
| FHIR server without `:in` support | `USE_INLINE_CODES` | Server-side filtering via inline codes |
| InMemoryFhirRepository (tests) | `FILTER_IN_MEMORY` | In-memory repo doesn't support search param filtering |
| Unknown/mixed server capabilities | `AUTO` | Detects capability and falls back appropriately |
| Small dataset, large ValueSets | `FILTER_IN_MEMORY` | Expansion cost exceeds filtering savings |
| Large dataset, selective ValueSets | `USE_INLINE_CODES` or `USE_VALUE_SET_URL` | Critical for performance at scale |

## File Locations

```
cqf-fhir-cql/src/main/java/org/opencds/cqf/fhir/cql/engine/retrieve/
  RetrieveSettings.java              # Configuration enums and storage
  BaseRetrieveProvider.java          # Abstract base with filtering logic
  RepositoryRetrieveProvider.java    # Concrete implementation with mode dispatch
  FederatedDataProvider.java         # Composite provider wrapper

cqf-fhir-cql/src/test/java/org/opencds/cqf/fhir/cql/engine/retrieve/
  BaseRetrieveProviderTests.java     # Unit tests for retrieve settings behavior

cqf-fhir-cr-hapi/src/main/java/org/opencds/cqf/fhir/cr/hapi/config/test/r4/
  TestCrR4Config.java                # Test Spring config (FILTER_IN_MEMORY default)

cqf-fhir-cr-cli/src/main/java/org/opencds/cqf/fhir/cr/cli/command/
  Utilities.java                     # CLI config (FILTER_IN_MEMORY default)
```
