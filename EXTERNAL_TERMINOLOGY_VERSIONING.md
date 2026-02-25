# External Terminology Versioning Issue

**Date:** 2026-02-13 (updated 2026-02-17)
**Status:** Implemented - external CodeSystems now have versions stripped

## Problem Statement

When running `ImplementationGuide/$data-requirements` on US Core 7.0.0, external CodeSystems (SNOMED CT, ICD-10-CM, etc.) are being resolved from dependency packages (e.g., hl7.terminology 5.5.0) and their package versions are being included in expansion parameters.

### Example Output
```json
{
    "name": "system-version",
    "valueString": "http://snomed.info/sct|3.2.1"
},
{
    "name": "system-version",
    "valueString": "http://hl7.org/fhir/sid/icd-10-cm|2.0.1"
},
{
    "name": "system-version",
    "valueString": "http://www.cms.gov/Medicare/Coding/ICD10|2.0.1"
}
```

## Root Cause

**Architecture Flow:**

1. **Initial extraction** (KeyElementFilter.java:200-220):
   - External CodeSystems identified from `ValueSet.compose.include.system`
   - Stored as URLs only (no version): `filteringResult.getReferencedCodeSystems()`

2. **Package resolution** (ImplementationGuidePackageResolver):
   - Dependency packages fetched (including hl7.terminology)
   - External CodeSystems resolved to actual resources from these packages
   - Resources have version metadata from the package

3. **RelatedArtifact creation** (DataRequirementsVisitor.java:734-736):
   ```java
   var reference = adapter.hasVersion()
           ? adapter.getUrl().concat("|%s".formatted(adapter.getVersion()))
           : adapter.getUrl();
   ```
   - Resolved resources include version if present
   - No distinction between "external terminology from package" vs "internal terminology"

4. **Expansion parameter generation** (InferManifestParametersVisitor.java:211-216):
   ```java
   case "CodeSystem":
       parameters.addParameter()
           .setName("system-version")
           .setValue(new StringType(canonical)); // Includes version if present
   ```

## Core Issue

**Just because we CAN resolve an external CodeSystem to a package doesn't mean we SHOULD use that package's version for expansion.**

### Concerns:
- External terminologies should be resolved by the terminology server, which knows the appropriate version
- Including specific versions (e.g., `http://snomed.info/sct|3.2.1`) may:
  - Cause expansion failures if that version isn't available on the terminology server
  - Force use of outdated versions
  - Create inconsistencies between package metadata and terminology server reality

## Current Architecture

### How External Terminology is Identified

**KeyElementFilter** (KeyElementFilter.java):
- Extracts CodeSystem URLs from `ValueSet.compose.include.system`
- Returns URLs without versions
- No semantic understanding of "external" vs "internal"

**DataRequirementsVisitor** (DataRequirementsVisitor.java):
- Line 406-414: Adds unresolved external CodeSystems (no version)
- Line 280-304: Adds resolved CodeSystems via `addRelatedArtifactWithSourcePackage()` (with version)

**Key Limitation:** No distinction between:
- External terminology that happens to be in a package (SNOMED from hl7.terminology)
- Internal terminology owned by the IG (US Core custom CodeSystems)

## Key Files Involved

1. **KeyElementFilter.java**
   - Line 200-220: `extractCodeSystemsFromValueSet()` - extracts external CodeSystem URLs

2. **DataRequirementsVisitor.java**
   - Line 280-304: `filterAndAddDependencies()` - adds resolved resources
   - Line 406-414: Adds unresolved external CodeSystems
   - Line 728-872: `addRelatedArtifactWithSourcePackage()` - includes version if present

3. **InferManifestParametersVisitor.java**
   - Line 211-216 (R4: 261-266, R5: 311-316): Creates `system-version` parameters

## Chosen Solution (2026-02-17)

**Approach:** External CodeSystems are identified using the `referencedCodeSystems` set from
`KeyElementFilteringResult` (CodeSystem URLs extracted from `ValueSet.compose.include.system`).
These are the external terminology systems (SNOMED, LOINC, ICD-10, etc.) that are referenced
by ValueSets but maintained by external organizations.

**In `$data-requirements`:** External CodeSystems are included as unversioned relatedArtifacts
(just the base URL, no `|version` suffix). The absence of a version signals that the terminology
server should resolve the appropriate version.

**In `$infer-manifest-parameters`:** Unversioned canonicals flow through naturally to expansion
parameters. The presence/absence of `|version` in the `system-version` parameter value is the flag:
- Internal: `"system-version": "http://myig.org/CodeSystem/my-codes|1.0.0"` (pinned)
- External: `"system-version": "http://snomed.info/sct"` (needs resolution)

**Implementation details:**
- `DataRequirementsVisitor.filterAndAddDependencies()`: Detects external CodeSystems by checking
  if URL is in `filteringResult.getReferencedCodeSystems()`. Routes these through
  `addRelatedArtifactWithSourcePackage(..., stripVersion=true)`.
- `addRelatedArtifactWithSourcePackage()`: New `stripVersion` parameter controls whether
  the adapter's version is included in the canonical reference.
- `InferManifestParametersVisitor`: No changes needed - passes through canonical values as-is.

## Previous Questions (Resolved)

1. **Should external terminologies have versions in expansion parameters?**
   - Should external CodeSystems be included in packages?
   - Should they have version constraints?
   - Should expansion parameters omit versions for external systems?

## Potential Solutions

### Option 1: URL-Based External Terminology Detection

**Implementation:**
```java
private boolean isExternalTerminology(String url) {
    return url.startsWith("http://snomed.info/sct")
        || url.startsWith("http://loinc.org")
        || url.startsWith("http://hl7.org/fhir/sid/")
        || url.startsWith("http://www.cms.gov/Medicare/Coding/")
        || url.startsWith("urn:iso:std:iso:")
        || url.startsWith("http://unitsofmeasure.org")
        || url.startsWith("http://www.nlm.nih.gov/research/umls/rxnorm");
}
```

**In DataRequirementsVisitor.addRelatedArtifactWithSourcePackage():**
- Check if resource is CodeSystem/ValueSet and URL matches external pattern
- Strip version for external terminologies
- Keep version for internal terminologies

**Pros:**
- Simple, explicit
- No ambiguity about what's external

**Cons:**
- Requires maintenance as new terminology systems emerge
- Hardcoded knowledge of external systems

### Option 2: Package-Based Detection

**Implementation:**
- Check if `sourcePackageCanonical` starts with known external terminology packages
- Examples: `http://terminology.hl7.org/ImplementationGuide/hl7.terminology`, THO packages

**Pros:**
- Leverages existing package metadata
- More maintainable than URL lists

**Cons:**
- Assumes package naming conventions
- May not work if external terminologies packaged differently

### Option 3: Separate Processing for External References

**Implementation:**
- In `filterAndAddDependencies()`, track which CodeSystems came from KeyElementFilter
- When adding resolved resources, check if they're in the "external" set
- Strip versions for external, keep for internal

**Pros:**
- Uses existing architecture
- Preserves intent of KeyElementFilter

**Cons:**
- More complex tracking logic
- Still need to identify what qualifies as "external"

### Option 4: Configuration-Based

**Implementation:**
- Add setting to specify external terminology URL patterns
- Allow users/deployments to customize

**Pros:**
- Most flexible
- Adapts to different deployment scenarios

**Cons:**
- Additional configuration burden
- May be overkill for this issue

## Impact

**Affects:**
- ImplementationGuide/$data-requirements operation
- ImplementationGuide/$package operation
- Library/$infer-manifest-parameters operation
- ValueSet expansion workflows
- Terminology server integration
- IG packaging and distribution

**Dependent Components:**
- HAPI FHIR server integration
- VSAC terminology routing
- Federated terminology provider
- Package registry integration

## Related Work

This issue was discovered while fixing:
1. VSAC routing in $package operation (fixed in BaseKnowledgeArtifactVisitor.java)
2. HTTP 500 error handling for terminology servers (fixed with try-catch)
3. cqf-resourceType extension support (added to DataRequirementsVisitor)
4. Resource type inference in $infer-manifest-parameters (fixed ClassCastException)

## Next Steps

1. **Gather feedback** on solution approach from team/community
2. **Decide on strategy** for distinguishing external vs internal terminology
3. **Implement solution** in DataRequirementsVisitor and/or InferManifestParametersVisitor
4. **Add tests** to verify external terminologies handled correctly
5. **Document behavior** in operation documentation

## Test Cases to Consider

1. **US Core 7.0.0** with SNOMED, ICD-10-CM, RxNorm references
2. **Custom IG** with internal CodeSystems
3. **Mixed scenario** with both internal and external CodeSystems
4. **ValueSet expansion** using generated parameters
5. **Package operation** with external terminologies

## References

- FHIR ValueSet/$expand operation: http://hl7.org/fhir/valueset-operation-expand.html
- CRMI IG: http://hl7.org/fhir/uv/crmi/
- HL7 Terminology: http://terminology.hl7.org/
- US Core IG: http://hl7.org/fhir/us/core/
