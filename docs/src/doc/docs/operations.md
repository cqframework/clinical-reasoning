# Operations

## Manifest Release Workflow

The manifest release workflow produces a versioned, released manifest Library from an ImplementationGuide. It consists of three operations used in sequence:

### 1. `ImplementationGuide/$data-requirements`

Analyzes an IG and produces a module-definition Library listing all dependencies. Dependencies are classified as `key` or `default` based on key element analysis of profiles. ValueSet compose chains are walked to discover transitive CodeSystem and ValueSet dependencies.

**Parameters:**

- `artifactEndpointConfiguration` â€” endpoint configuration for resolving canonical artifacts
- `terminologyEndpoint` â€” endpoint for resolving terminology resources not available locally

### 2. `Library/$infer-manifest-parameters`

Converts the module-definition Library from step 1 into an asset-collection manifest Library with:

- Expansion parameters (contained Parameters resource) for versioned CodeSystem and ValueSet references
- `depends-on` relatedArtifact entries propagated from the input
- `composed-of` relatedArtifact entries propagated from the input

### 3. `Library/$release-manifest`

Releases the manifest by resolving unversioned dependency references and updating metadata. Unlike `$release`, this operation does not re-discover dependencies through component traversal â€” it trusts the pre-computed `depends-on` entries from step 2.

**Parameters:**

- `version` (required) â€” the version to assign to the released manifest
- `versionBehavior` (required) â€” how to apply the version (`default`, `check`, `force`)
- `latestFromTxServer` â€” whether to resolve unversioned references from the terminology server (default: `false`)
- `terminologyEndpoint` â€” FHIR Endpoint resource with authentication headers for terminology resolution (required when `latestFromTxServer=true`)
- `releaseLabel` â€” optional label to apply to the released manifest

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


## PlanDefinition applicability and missing data

Applicability processing pauses on unknown by default. An action with an unresolved condition keeps its input questions available but does not apply its descendants or definition. In an ordered `any` group, an unresolved child also stops evaluation of later alternatives. A false child excludes its descendants and permits the next alternative; a true child is selected and stops later alternatives. Independent `all` children remain independently evaluated.

This is a runtime policy for missing data in addition to the first-true selection described by [FHIR-50150](https://jira.hl7.org/browse/FHIR-50150). Hosts can explicitly select the previous non-applicable-on-null behavior through the existing settings API:

```java
var settings = CrSettings.getDefault().withPauseOnUnknownApplicability(false);
var processor = new PlanDefinitionProcessor(repository, settings);
```

The setting applies to shared applicability evaluation for ordinary actions and ordered groups, including nested PlanDefinitions. Disabling it does not make null true: descendants of that action are still excluded, but later ordered alternatives may apply.

With pausing enabled, multiple applicability conditions form a three-state conjunction: known false dominates unknown. Invalid or missing executable expressions, non-Boolean/multiple results, and evaluation failures report OperationOutcome errors and block that ordered branch even if another condition is false. Such errors are not successful requests for a missing answer. Each reached condition is evaluated once per action traversal.

Input questions for a reached action remain available even when its condition is false, so an answer can be corrected. Questions beneath an excluded parent and later alternatives after an ordered pause are not newly generated. Existing items in a caller-supplied Questionnaire are retained; this setting does not retract a previously expanded form. A generated Questionnaire/QuestionnaireResponse and no activity alone are not a guarantee that every missing datum is answerable.
