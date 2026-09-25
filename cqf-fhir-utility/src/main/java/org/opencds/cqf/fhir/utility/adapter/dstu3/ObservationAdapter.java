package org.opencds.cqf.fhir.utility.adapter.dstu3;

import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IObservationAdapter;

public class ObservationAdapter extends ResourceAdapter implements IObservationAdapter {

    public ObservationAdapter(IBaseResource observation) {
        super(observation);

        if (!(observation instanceof Observation)) {
            throw new IllegalArgumentException("resource passed as parameters argument is not an Observation resource");
        }
    }

    @Override
    public Observation get() {
        return (Observation) resource;
    }
}
