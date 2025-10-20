
# Commonalities

* IDef interface (probably with no methods, just a marker)
* equals/hashCode/toString (compare objects)

# LibraryDef

* VersionedIdentifier
* Content
  * Base64 encoded CQL
  * Reference to CQL file (testing only)
  * automagically resolve this in a version-agnostic way though private methods that return an InputStream

## Thoughts

* Front load all the validation that a Library is coherent before creating a MeasureDef + LibraryDef
* Figure out the Base64 content vs Path and initialize the CQL content with a ByteArrayInputStream, figuring out all the FHIR version specific machinery up front
* Do we need a wrapper interface for FHIR resources and data that are involved in data providers, just as some sort of marker?

# MeasureDef

* id
* url
* version
* LibraryDef (Collection)
* groups
  * populations
  * stratifiers
  * scoring
  * etc...
* sdes
* errors
 
## Thoughts

* Don't completely replace FHIR measure because we need it to create the FHIR-specific MeasureReport and probably other FHIR output resources
* Use these constructs specifically for input into the CQL engine
  * Library VersionedIdentifier
  * Library CQL content


# Etc...

* Talk to Brenin

# Integration Points

* FHIR Web APIs
* clinical-reasoning services (may overlap with the first)
* VersionedIdentifier computation
  * CQL evaluation
* CQL data providers
  * When CQL is evaluating and sets results/evaluated resources
* Providers
  * Library Source
    * RepositoryFhirLibrarySourceProvider
      * IRepository.search() > IBaseResource
      * LibraryVersionSelector
    * BaseFhirLibrarySourceProvider
      * Take IBaseResource from RepositoryFhirLibrarySourceProvider
      * Pass to IAdapterFactory
      * Obtain an ILibraryAdapter
      * Obtain an IAttachmentAdapter
      * Check the contentType is what we expect
      * Get the byte[] from the attachment adapter
      * Get a ByteArrayInputStream
  * Model Info

# NPM?

* How does this play with NPM?
* Can we have the NPM loader just return IDefs instead of FHIR resources?

# IDefRepo (need better name)

* Top-level interface for all defs (IDef.java)
