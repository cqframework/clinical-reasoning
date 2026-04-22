# Operations

## Manifest Release Workflow

The manifest release workflow produces a versioned, released manifest Library from an ImplementationGuide. It consists of three operations used in sequence:

### 1. `ImplementationGuide/$data-requirements`

Analyzes an IG and produces a module-definition Library listing all dependencies. Dependencies are classified as `key` or `default` based on key element analysis of profiles. ValueSet compose chains are walked to discover transitive CodeSystem and ValueSet dependencies.

**Parameters:**

- `artifactEndpointConfiguration` — endpoint configuration for resolving canonical artifacts
- `terminologyEndpoint` — endpoint for resolving terminology resources not available locally

### 2. `Library/$infer-manifest-parameters`

Converts the module-definition Library from step 1 into an asset-collection manifest Library with:

- Expansion parameters (contained Parameters resource) for versioned CodeSystem and ValueSet references
- `depends-on` relatedArtifact entries propagated from the input
- `composed-of` relatedArtifact entries propagated from the input

### 3. `Library/$release-manifest`

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
