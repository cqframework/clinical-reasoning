package org.opencds.cqf.fhir.utility.adapter.r5

import ca.uhn.fhir.repository.IRepository
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r5.model.*
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IAdapter
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter

class ImplementationGuideAdapter : KnowledgeArtifactAdapter, IImplementationGuideAdapter {
    constructor(implementationGuide: IDomainResource) : super(implementationGuide) {
        require(implementationGuide is ImplementationGuide) {
            "resource passed as implementationGuide argument is not a ImplementationGuide resource"
        }
    }

    constructor(implementationGuide: ImplementationGuide) : super(implementationGuide)

    protected val implementationGuide: ImplementationGuide
        get() = resource as ImplementationGuide

    override fun get(): ImplementationGuide {
        return resource as ImplementationGuide
    }

    override fun copy(): ImplementationGuide? {
        return get()!!.copy()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            this.implementationGuide.definition.resource.forEach { dr ->
                addProfileReferences(references, dr!!.reference.reference)
            }
            return references
        }

    override fun getDependencies(repository: IRepository?): MutableList<IDependencyInfo?> {
        val references = mutableListOf<IDependencyInfo?>()
        val referenceSource = this.referenceSource
        addProfileReferences(references, referenceSource)

        for (dr in this.implementationGuide.definition.resource) {
            if (
                dr.hasReference() &&
                    dr.reference.hasReference() &&
                    !(dr.hasIsExample() && dr.isExample)
            ) {
                addPackageResourceDependency(dr, repository!!, references)
            }
        }

        return references
    }

    private fun addPackageResourceDependency(
        dr: ImplementationGuide.ImplementationGuideDefinitionResourceComponent,
        repository: IRepository,
        references: MutableList<IDependencyInfo?>,
    ) {
        val artifactUrlExt = "http://hl7.org/fhir/StructureDefinition/artifact-url"
        val refValue = dr.reference.reference
        val refElement = dr.reference.referenceElement
        val read: Any?
        try {
            val refClass: Class<out IBaseResource?> =
                fhirContext.getResourceDefinition(refElement.resourceType).newInstance().javaClass
            read = repository.read(refClass, IdType(refValue))
        } catch (e: Exception) {
            IAdapter.logger.warn("Unable to read resource for reference: {}, skipping", refValue)
            return
        }
        if (read is MetadataResource && (read.hasUrl() || read.hasUrlElement())) {
            val url = if (read.hasUrlElement()) read.urlElement else UrlType(read.url)
            references.add(
                DependencyInfo(
                    refValue,
                    url.valueAsString,
                    read.extension,
                    { theValue -> url.setValue(theValue) },
                )
            )
        } else if (read is DomainResource && read.getExtensionByUrl(artifactUrlExt) != null) {
            val ext = read.getExtensionByUrl(artifactUrlExt)
            val url = UriType(ext.value.primitiveValue())
            references.add(
                DependencyInfo(
                    refValue,
                    url.valueAsString,
                    read.extension,
                    { theValue -> url.setValue(theValue) },
                )
            )
        } else {
            IAdapter.logger.warn("Unable to resolve dependency URL for reference: {}", refValue)
        }
    }
}
