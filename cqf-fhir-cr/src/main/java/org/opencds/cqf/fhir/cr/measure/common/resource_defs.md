
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

# JP

* NPM should return FHIR resources, not "Defs"
* Should return un-validated resources
* NPM interface should reflect this, common with IRepository
* These APIs are at the boundary of our "protected world"
* As close as possible to that boundary point, do the validation and "sanitize" the resources
* Have everywhere that uses IAdapter, parse into "Defs" immediately
  * So use IAdapter but confine its use to conversion
* Do we need a MeasureReportDef, that then gets converted to R4 MeasureReport at the last minute?
* Convert to a version of FHIR, like R6, and then use *existing* FHIR core code to convert to a specific version, such as R4
* As an alternative, convert to Measure R6, then pass that around instead of MeasureDef
* In the event of an R7, we just change the imports and tweak the logic a little bit
* Add another line of conditional logic for the back versions we support
* Need to define the incremental steps

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

## Non-requirements

* Any data points exclusively used by MeasureReport builders
* Any data points specific to any FHIR version
 
## Thoughts

* Don't completely replace FHIR measure because we need it to create the FHIR-specific MeasureReport and probably other FHIR output resources
* Use these constructs specifically for input into the CQL engine
  * Library VersionedIdentifier
  * Library CQL content


# Performance

* Do we care about cutting down the number of SQL or other queries to get back libraries?
* Do we want to consider some type of caching mechanism, such as all the measure library CQL inputstreams, and some kind wrapper on top of IRepository or other data retrieval API?

# Etc...

* Other FHIR resources, used outside of measure evaluation, are used by clinical-reasoning and DQM
* Talk to Brenin for more details

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
* Brenin domain
  * IAdapter/etc

# NPM?

* How does this play with NPM?
* Can we have the NPM loader just return IDefs instead of FHIR resources?

# IDefRepo (need better name)

* Top-level interface for all defs (IDef.java)
* At a minimum, use this as a marker for APIs
  * ex:  <T extends IDef.class> T read(Class<T> defClass, IidType (or something))
