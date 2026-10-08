package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBaseExtension

/**
 * Represents a dependency on another FHIR artifact.
 *
 * The `reference` property is intended to be a FHIR canonical reference (i.e., `url` or
 * `url|version`) as defined by the FHIR specification. Implementations are responsible for
 * resolving any non-canonical references to canonical form before returning them via this
 * interface.
 */
interface IDependencyInfo {

    var referenceSource: String?

    /**
     * Returns the canonical reference string (url or url|version) for the dependency. This must not
     * be a relative reference or a plain id.
     *
     * @return the canonical reference string for the dependency
     */
    /**
     * Sets the canonical reference string (url or url|version) for the dependency. Callers must
     * supply a canonical reference string; implementations may normalize or validate the reference.
     *
     * @param reference the canonical reference string for the dependency
     */
    var reference: String?

    var referencePackageId: String?

    fun <E : IBaseExtension<*, *>> getExtension(): MutableList<E?>?

    /**
     * Returns the list of dependency roles (key, default, example, test) for this dependency.
     * Multiple roles can apply to the same dependency.
     *
     * @return the list of role codes
     */
    /**
     * Sets the list of dependency roles for this dependency.
     *
     * @param roles the list of role codes
     */
    var roles: MutableList<String?>

    /**
     * Adds a single role to this dependency.
     *
     * @param role the role code to add
     */
    fun addRole(role: String?)

    /**
     * Returns the list of FHIRPath expressions indicating where this dependency was referenced.
     *
     * @return the list of FHIRPath expressions
     */
    val fhirPaths: MutableList<String?>?

    /**
     * Adds a FHIRPath expression indicating where this dependency was referenced.
     *
     * @param fhirPath the FHIRPath expression
     */
    fun addFhirPath(fhirPath: String?)

    /**
     * Builds the CRMI dependency extensions (role, package-source, reference-source) for this
     * dependency.
     *
     * @param <E> the extension type
     * @param fhirVersion the FHIR version to build extensions for
     * @param sourceArtifactUrl the canonical URL of the source artifact (for reference-source
     *   extensions)
     * @return list of extensions representing this dependency's metadata </E>
     */
    fun <E : IBaseExtension<*, *>> buildDependencyExtensions(
        fhirVersion: FhirVersionEnum?,
        sourceArtifactUrl: String?,
    ): MutableList<E?>?
}
