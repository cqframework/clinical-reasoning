# Operations

This project implements FHIR [Clinical Reasoning](http://hl7.org/fhir/clinicalreasoning-module.html) operations as HAPI FHIR `@Operation` providers in the `cqf-fhir-cr-hapi` module. Operations are grouped below by the Implementation Guide (IG) that defines them. Unless noted otherwise, an operation is available for both `DSTU3` and `R4`; operations introduced by the Canonical Resource Management Infrastructure (CRMI) and Structured Data Capture (SDC) IGs are `R4`-only.

## Clinical Practice Guidelines (CPG)

Spec: [hl7.org/fhir/uv/cpg](http://hl7.org/fhir/uv/cpg/)

Operations for evaluating CQL-based logic and applying guideline-driven recommendations.

| Operation | Resource(s) | Description |
|---|---|---|
| `$apply` | `PlanDefinition`, `ActivityDefinition`, `GraphDefinition` | Applies a definitional resource in the context of a subject, producing a `CarePlan`/`RequestOrchestration` (or, for `ActivityDefinition`, a single resource) representing the actions to take. |
| `$r5.apply` | `PlanDefinition` | Preview of the R5 `$apply` operation signature/behavior, run against R4 data. |
| `$evaluate` | `Library` | Evaluates one or more expressions in a Library and returns the results as a `Parameters` resource. |
| `$evaluate` | `Group` | Evaluates a `Group` definition (e.g. CQL-defined membership criteria) and returns the results as a `Group` resource. |
| `$data-requirements` | `PlanDefinition`, `Library`, `Questionnaire`, `ValueSet`, `GraphDefinition` | Returns the data requirements (`Library` module-definition) for evaluating the resource, without executing it. |
| `$cql` | (system-level) | Ad hoc execution of a CQL expression or library against supplied or server data. |

## Data Exchange for Quality Measures (DEQM) / Quality Measures

Specs: [hl7.org/fhir/us/davinci-deqm](http://hl7.org/fhir/us/davinci-deqm/) (Da Vinci DEQM), [hl7.org/fhir/us/cqfmeasures](http://hl7.org/fhir/us/cqfmeasures/) (US CQFM)

Operations for evaluating quality measures and exchanging measure data between systems.

| Operation | Resource(s) | Description |
|---|---|---|
| `$evaluate-measure` | `Measure` | Evaluates a Measure for a subject or population and returns a `MeasureReport`. |
| `$evaluate` | `Measure` (type-level) | Evaluates multiple measures (by id or url) in a single call. |
| `$care-gaps` | `Measure` | Identifies gaps in care for a subject/population against one or more measures, returning a `Bundle` of `MeasureReport`/`DetectedIssue` resources. |
| `$collect-data` | `Measure` | Collects the data-of-interest for a Measure for a subject, returning a `Parameters` resource of the evaluated resources. |
| `$submit-data` | `Measure` | Accepts a `MeasureReport` and supporting evidence, and stores it on the server. |
| `$data-requirements` | `Measure` | Returns the data requirements for evaluating the Measure. |

## Structured Data Capture (SDC)

Spec: [hl7.org/fhir/uv/sdc](http://hl7.org/fhir/uv/sdc/)

Operations for generating and populating `Questionnaire`s and extracting structured data from responses.

| Operation | Resource(s) | Description |
|---|---|---|
| `$questionnaire` | `StructureDefinition` | Builds a `Questionnaire` from a `StructureDefinition` profile. |
| `$populate` | `Questionnaire` | Pre-populates a `Questionnaire` for a subject, returning a `QuestionnaireResponse` (or a populated `Questionnaire`). |
| `$extract` | `QuestionnaireResponse` | Extracts structured FHIR resources from a completed `QuestionnaireResponse`, using SDC extraction extensions or a `StructureMap`. |

## Canonical Resource Management Infrastructure (CRMI)

Spec: [hl7.org/fhir/uv/crmi](http://hl7.org/fhir/uv/crmi/)

Operations for the authoring lifecycle (draft → review → approve → release → retire/withdraw) and packaging of knowledge artifacts (`Library`, `Measure`, `PlanDefinition`, `Questionnaire`, `ValueSet`, `GraphDefinition`, `ImplementationGuide`, and other canonical resources). `$draft`, `$approve`, `$release`, and `$package` are implemented generically for any `MetadataResource`; `$retire`, `$revise`, `$withdraw`, `$delete`, and `$artifact-diff` are currently wired only for `Library`.

| Operation | Resource(s) | Description |
|---|---|---|
| [`$draft`](http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-draft) | Any `MetadataResource`, `Library` | Creates a new draft version of an artifact and its owned children. |
| [`$approve`](http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-approve) | Any `MetadataResource`, `Library` | Marks an artifact (and optionally its dependencies) as approved, recording an `approvalDate` and endorser/reviewer data. |
| `$release` | Any `MetadataResource`, `Library`, `ImplementationGuide` | Releases an artifact by setting its status to `active`, assigning a version, and resolving unversioned dependency references. |
| `$revise` | `Library` | Updates the content of an existing draft artifact. |
| `$retire` | `Library` | Retires an active artifact, setting its status to `retired`. |
| `$withdraw` | `Library` | Withdraws a draft artifact, removing it and its owned components. |
| `$delete` | `Library` | Deletes an artifact and, optionally, its owned components. |
| `$package` | Any `MetadataResource`, `Library`, `PlanDefinition`, `Questionnaire`, `ValueSet`, `GraphDefinition`, `ImplementationGuide` | Packages an artifact with its dependencies and/or components into a `Bundle`. |
| `$artifact-diff` | `Library` | Produces a diff between two versions of a knowledge artifact. |
| `$data-requirements` | `ImplementationGuide` | Returns the data requirements for the content of an `ImplementationGuide`. |

### Manifest Release Workflow

The manifest release workflow produces a versioned, released manifest `Library` from an `ImplementationGuide`. It uses two operations from the table above plus one repository-specific extension, in sequence:

#### 1. `ImplementationGuide/$data-requirements`

Analyzes an IG and produces a module-definition `Library` listing all dependencies. Dependencies are classified as `key` or `default` based on key element analysis of profiles. ValueSet compose chains are walked to discover transitive CodeSystem and ValueSet dependencies.

**Parameters:**

- `artifactEndpointConfiguration` — endpoint configuration for resolving canonical artifacts
- `terminologyEndpoint` — endpoint for resolving terminology resources not available locally

#### 2. [`Library/$infer-manifest-parameters`](http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-infer-manifest-parameters)

Converts the module-definition Library from step 1 into an asset-collection manifest Library with:

- Expansion parameters (contained Parameters resource) for versioned CodeSystem and ValueSet references
- `depends-on` relatedArtifact entries propagated from the input
- `composed-of` relatedArtifact entries propagated from the input

#### 3. `Library/$release-manifest`

Releases the manifest by resolving unversioned dependency references and updating metadata. Unlike `$release`, this operation does not re-discover dependencies through component traversal — it trusts the pre-computed `depends-on` entries from step 2.

**Parameters:**

- `version` (required) — the version to assign to the released manifest
- `versionBehavior` (required) — how to apply the version (`default`, `check`, `force`)
- `latestFromTxServer` — whether to resolve unversioned references from the terminology server (default: `false`)
- `terminologyEndpoint` — FHIR Endpoint resource with authentication headers for terminology resolution (required when `latestFromTxServer=true`)
- `releaseLabel` — optional label to apply to the released manifest

**Example terminology endpoint:**

```json
{
    "resourceType": "Endpoint",
    "status": "active",
    "connectionType": {
        "system": "http://hl7.org/fhir/ValueSet/endpoint-connection-type",
        "code": "hl7-fhir-rest"
    },
    "header": [
        "Authorization: Basic <base64-encoded credentials>"
    ],
    "address": "https://cts.nlm.nih.gov/fhir",
    "payloadType": [
        {
            "system": "http://hl7.org/fhir/ValueSet/endpoint-payload-type",
            "code": "any"
        }
    ]
}
```

### `$create-changelog`

`Library/$create-changelog` produces a changelog comparing two `Library` versions in a form that can be easily rendered as a table. It is a repository-specific extension of the CRMI authoring workflow, not currently part of the published CRMI IG.

## Electronic Case Reporting / Public Health (eCR)

Spec: [hl7.org/fhir/us/ecr](http://hl7.org/fhir/us/ecr/)

| Operation | Resource(s) | Description |
|---|---|---|
| `$ersd-v2-import` | (system-level) | Imports an active/released eRSD v2.1.1-conformant `Bundle` and transforms it into Value Set Manager authoring state. |

## CDS Hooks

Spec: [cds-hooks.hl7.org](https://cds-hooks.hl7.org/)

[CDS Hooks](https://cds-hooks.hl7.org/) services are implemented separately from the `@Operation`-based FHIR operations above, via a `cds-services` discovery endpoint and hook-execution interceptor that runs `PlanDefinition`/`Library`-based clinical reasoning logic in response to hook invocations from an EHR.
