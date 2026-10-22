package org.opencds.cqf.fhir.utility.r5

import ca.uhn.fhir.repository.IRepository
import java.util.*
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes
import org.hl7.fhir.r5.model.RelatedArtifact
import org.opencds.cqf.fhir.utility.BundleHelper.addEntry
import org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources
import org.opencds.cqf.fhir.utility.Canonicals.getResourceType
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.Companion.forFhirVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** This class consists exclusively of static methods that assist with packaging FHIR Resources. */
object PackageHelper : org.opencds.cqf.fhir.utility.PackageHelper() {
    internal val logger: Logger = LoggerFactory.getLogger(PackageHelper::class.java)

    internal val PACKABLE_RESOURCES =
        mutableListOf<String?>(
            FHIRTypes.LIBRARY.toCode(),
            FHIRTypes.PLANDEFINITION.toCode(),
            FHIRTypes.ACTIVITYDEFINITION.toCode(),
            FHIRTypes.STRUCTUREDEFINITION.toCode(),
            FHIRTypes.CODESYSTEM.toCode(),
            FHIRTypes.VALUESET.toCode(),
        )

    fun addRelatedArtifacts(
        bundle: IBaseBundle,
        artifacts: MutableList<RelatedArtifact?>,
        repository: IRepository,
        isPut: Boolean,
    ) {
        for (artifact in artifacts) {
            if (
                artifact!!.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON &&
                    artifact.hasResourceElement()
            ) {
                try {
                    val canonical = artifact.getResourceElement()
                    if (PACKABLE_RESOURCES.contains(getResourceType(canonical))) {
                        val resource =
                            SearchHelper.searchRepositoryByCanonical(repository, canonical)
                        if (
                            resource != null &&
                                getEntryResources(bundle).none { r ->
                                    r.idElement == resource.idElement
                                }
                        ) {
                            addEntry(bundle, createEntry(resource, isPut))
                            val adapter =
                                forFhirVersion(resource.structureFhirVersionEnum)
                                    .createKnowledgeArtifactAdapter(resource as IDomainResource)
                            if (adapter.hasRelatedArtifact<RelatedArtifact>()) {
                                PackageHelper.addRelatedArtifacts(
                                    bundle,
                                    adapter.getRelatedArtifact(),
                                    repository,
                                    isPut,
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e.message, e)
                }
            }
        }
    }
}
