package org.opencds.cqf.fhir.utility.adapter.r4

import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Observation
import org.opencds.cqf.fhir.utility.adapter.IObservationAdapter

class ObservationAdapter(observation: IBaseResource) :
    ResourceAdapter(observation), IObservationAdapter {
    init {
        require(observation is Observation) {
            "resource passed as parameters argument is not an Observation resource"
        }
    }

    override fun get(): Observation {
        return resource as Observation
    }
}
