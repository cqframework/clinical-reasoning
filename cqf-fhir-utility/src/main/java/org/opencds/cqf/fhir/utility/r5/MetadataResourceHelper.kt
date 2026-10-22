package org.opencds.cqf.fhir.utility.r5

import ca.uhn.fhir.repository.IRepository
import java.util.function.Consumer
import org.hl7.fhir.r5.model.*

object MetadataResourceHelper {
    @JvmStatic
    fun forEachMetadataResource(
        entries: MutableList<Bundle.BundleEntryComponent?>,
        callback: Consumer<MetadataResource?>?,
        repository: IRepository,
    ) {
        entries
            .map { entry -> entry!!.getResponse().getLocation() }
            .map { location ->
                when (location!!.split("/")[0]) {
                    "ActivityDefinition" ->
                        return@map repository.read(ActivityDefinition::class.java, IdType(location))

                    "Library" -> return@map repository.read(Library::class.java, IdType(location))
                    "Measure" -> return@map repository.read(Measure::class.java, IdType(location))
                    "PlanDefinition" ->
                        return@map repository.read(PlanDefinition::class.java, IdType(location))

                    "ValueSet" -> return@map repository.read(ValueSet::class.java, IdType(location))
                    else -> return@map null
                }
            }
            .forEach(callback)
    }
}
