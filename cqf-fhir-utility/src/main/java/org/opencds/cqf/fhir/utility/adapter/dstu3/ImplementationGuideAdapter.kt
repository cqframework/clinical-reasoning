package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IDomainResource
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

    protected val implementationGuide: ImplementationGuide?
        get() = resource as ImplementationGuide?

    override fun get(): ImplementationGuide {
        return resource as ImplementationGuide
    }

    override fun copy(): ImplementationGuide? {
        return get().copy()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references: MutableList<IDependencyInfo?> = ArrayList<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            this.implementationGuide!!
                .getPackage()
                .forEach({ p ->
                    p!!
                        .resource
                        .forEach({ pr ->
                            val source = pr!!.getSource()
                            if (source is UriType) {
                                addProfileReferences(references, source.value)
                            } else if (source is Reference) {
                                addProfileReferences(references, source.reference)
                            } else {
                                throw UnprocessableEntityException(
                                    "Package Resource Source must be instance of UriType or Reference"
                                )
                            }
                        })
                })

            return references
        }

    override fun getDependencies(repository: IRepository?): MutableList<IDependencyInfo?> {
        val references: MutableList<IDependencyInfo?> = ArrayList<IDependencyInfo?>()
        val referenceSource = this.referenceSource
        addProfileReferences(references, referenceSource)

        for (pkg in this.implementationGuide!!.getPackage()) {
            for (dr in pkg.resource) {
                if (dr.hasSource() && !(dr.hasExample() && dr.getExample())) {
                    addPackageResourceDependency(dr, repository!!, references)
                }
            }
        }

        return references
    }

    private fun addPackageResourceDependency(
        dr: ImplementationGuide.ImplementationGuidePackageResourceComponent,
        repository: IRepository,
        references: MutableList<IDependencyInfo?>,
    ) {
        val artifactUrlExt = "http://hl7.org/fhir/StructureDefinition/artifact-url"
        val refValue =
            if (dr.hasSourceReference()) dr.getSourceReference()
            else Reference(dr.getSource().primitiveValue())
        val refElement = IdType(refValue.reference)
        val read: Any?
        try {
            val refClass: Class<out IBaseResource?> =
                fhirContext.getResourceDefinition(refElement.resourceType).newInstance().javaClass
            read = repository.read(refClass, IdType(refValue.reference))
        } catch (e: Exception) {
            IAdapter.logger.warn(
                "Unable to read resource for reference: {}, skipping",
                refValue.reference,
            )
            return
        }
        if (read is MetadataResource && (read.hasUrl() || read.hasUrlElement())) {
            val url = if (read.hasUrlElement()) read.urlElement else UriType(read.url)
            references.add(
                DependencyInfo(
                    refValue.reference,
                    url.valueAsString,
                    read.extension,
                    { theValue -> url.value = theValue },
                )
            )
        } else if (read is DomainResource && read.getExtensionByUrl(artifactUrlExt) != null) {
            val ext = read.getExtensionByUrl(artifactUrlExt)
            val url = org.hl7.fhir.r5.model.UriType(ext.value.primitiveValue())
            references.add(
                DependencyInfo(
                    refValue.reference,
                    url.valueAsString,
                    read.extension,
                    { theValue -> url.value = theValue },
                )
            )
        } else {
            IAdapter.logger.warn("Unable to resolve dependency URL for reference: {}", refValue)
        }
    }
}
