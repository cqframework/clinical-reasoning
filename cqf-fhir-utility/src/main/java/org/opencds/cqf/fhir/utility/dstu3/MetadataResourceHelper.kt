package org.opencds.cqf.fhir.utility.dstu3

import ca.uhn.fhir.repository.IRepository
import java.util.function.Consumer
import org.hl7.fhir.dstu3.model.*

object MetadataResourceHelper {
    @JvmStatic
    fun forEachMetadataResource(
        entries: MutableList<Bundle.BundleEntryComponent?>,
        callback: Consumer<MetadataResource?>?,
        repository: IRepository,
    ) {
        entries
            .map { entry -> entry!!.response.location }
            .map { location ->
                when (location!!.split("/")[0]) {
                    "ActivityDefinition" ->
                        repository.read(ActivityDefinition::class.java, IdType(location))

                    "Library" -> repository.read(Library::class.java, IdType(location))
                    "Measure" -> repository.read(Measure::class.java, IdType(location))
                    "PlanDefinition" ->
                        repository.read(PlanDefinition::class.java, IdType(location))

                    "ValueSet" -> repository.read(ValueSet::class.java, IdType(location))
                    else -> null
                }
            }
            .forEach(callback)
    }
}
