package org.opencds.cqf.fhir.utility.adapter.r5;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.InstantType;
import org.hl7.fhir.r5.model.Observation;
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

    @Override
    public ObservationAdapter setEffective(String effective) {
        return setEffective(new DateTimeType(effective));
    }

    @Override
    public ObservationAdapter setEffective(IBaseDatatype effective) {
        if (effective instanceof DateTimeType dateTime) {
            get().setEffective(dateTime);
        }
        return this;
    }

    @Override
    public ObservationAdapter setIssued(String issued) {
        return setIssued(new InstantType(new DateTimeType(issued)));
    }

    @Override
    public ObservationAdapter setIssued(IBaseDatatype issued) {
        if (issued instanceof InstantType instantType) {
            get().setIssuedElement(instantType);
        }
        return this;
    }
}
