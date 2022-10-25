# sample-content-ig
A sample, template-driven content implementation guide that provides an illustration of a content IG, with examples of using CQL with FHIR libraries and vocabulary. A content IG is one that includes computable representations of clinical knowledge artifacts as FHIR Resources, specifically Library, ActivityDefinition, PlanDefinition, and Measure resources.

## Building the IG
In addition to the [FHIR Publisher](https://confluence.hl7.org/display/FHIR/IG+Publisher+Documentation), this
IG makes use of the Refresh IG command of [CQF Tooling](https://github.com/cqframework/cqf-tooling). This
command performs several functions related to knowledge artifact content processing:

1. Validates CQL
2. Includes CQL and ELM content in the appropriate Library resource
3. Infers data requirements and dependencies and updates this information in each knowledge artifact
4. Stamps each artifact with a software device that provides tooling information for the CQF Tooling
5. Packages test data associated with each knowledge artifact
6. Packages each artifact with its associated components, dependencies and test data

This refresh process must be performed prior to running the publisher. Just like the publisher, there are two steps:

1. Ensure the CQFTooling is available in the local input-cache by running the _updateCQFTooling script
2. Run the refresh using the _refresh script

The refresh process will update resources in place, as well as place bundled content in the bundles directory.

## Directory Structure
Content IGs follow the same general structure as any FHIR Implementation Guide, but add a few specific directories to support knowledge artifacts:

```
bundles/<artifact-resource-type-name>
input/cql
input/resources/<artifact-resource-type-name>
input/tests/<artifact-resource-type-name>/<artifact-resource-name>
input/tests/<artifact-resource-type-name>/<artifact-resource-name>/<patient-id>
input/tests/<artifact-resource-type-name>/<artifact-resource-name>/<patient-id>/<resource-type-name>/<resource files> // flexible structure
input/vocabulary/codesystem
input/vocabulary/codesystem/external
input/vocabulary/valueset
input/vocabulary/valueset/external
```

This is also the same structure as the [Atom CQL Plugin](https://github.com/cqframework/atom_cql_support) uses to support CQL authoring and evaluation as part of the Atom plugin.

The `bundles/<artifact-resource-type-name>` folder is where artifact bundles are placed.

The `input/cql` folder contains all the source CQL files as well as the `cql-options.json` file.

The `input/resources/<artifact-resource-type-name>` folders (e.g. `input/resources/library`) contain all the source content for the various artifacts. For CQL Libraries in particular, ensure that a corresponding FHIR Library resource shell exists for each CQL source file. For example

```json
{
  "resourceType": "Library",
  "id": "FirstExample",
  "name": "FirstExample",
  "title": "First Example",
  "status": "active",
  "experimental": false,
  "type": {
    "coding": [ {
      "system": "http://terminology.hl7.org/CodeSystem/library-type",
      "code": "logic-library"
    } ]
  },
  "description": "This resource provides a simple example of a CQL library"
}
```

Note that the `name` element of the FHIR Library resource is required to match exactly the `name` of the CQL Library. The refresh tooling will automatically set the `url` element as follows:

`<ig-base-canonical>/Library/<library-name>`

This is important because this is how FHIR-based environments will resolve library name references in CQL.

In addition, because the Library is being published as a conformance resource in a FHIR IG, the `id` of the resource must match the tail (i.e. final path segment) of the `url`, so in this context, the `id` must also match the name of the library. Because the [id](https://hl7.org/fhir/datatypes.html#id) in FHIR cannot include underscores, this effectively means that CQL library names must not include underscores either.

The `input/tests/<artifact-resource-type-name>/<artifact-resource-name>` folders contain test cases for each artifact.

The `input/tests/<artifact-resource-type-name>/<artifact-resource-name>/<patient-id>` folders contain each individual test case, per patient, and the resource files are expected to contain a Patient with the given id.

The `input/vocabulary/codesystem` folder contains code systems defined in this IG.

The `input/vocabulary/codesystem/external` folder contains code systems required by the content but defined in other sources.

The `input/vocabulary/valueset` folder contains value sets defined in this IG.

The `input/vocabulary/valueset/external` folder contains value sets required by the content but defined in other sources.

NOTE: The `external` vocabulary directories here are intended to support evaluation of artifacts that reference external terminology (i.e. terminology defined in other sources such as the HL7 Terminology Authority, the FHIR Specification, the Value Set Authority Center, or other FHIR Implementation Guides). This is a stop-gap measure until the authoring environment and refresh tooling have the ability to resolve external terminologies.

## Artifact Narratives
This sample content IG also contains [FHIR Liquid](https://confluence.hl7.org/display/FHIR/FHIR+Liquid+Profile) templates for knowledge artifacts. Ultimately, these templates should be part of the CQF Content IG Template, but until then, they are typically copied in to content IGs. They are maintained here as the source of truth, and so should be general-purpose templates suitable for use in any realm, and in any content IG. To use them in your IG, copy them in to the `templates/liquid` folder and set the `path-liquid` implementation guide parameter.
