package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

public class BackboneElementAdapter extends BaseAdapter {

    private final IBaseBackboneElement backboneElement;

    public BackboneElementAdapter(FhirVersionEnum fhirVersion, IBase element) {
        super(fhirVersion, element);
        if (!(element instanceof IBaseBackboneElement)) {
            throw new IllegalArgumentException("object passed as element argument is not of type IBaseBackboneElement");
        }
        backboneElement = (IBaseBackboneElement) element;
    }

    @Override
    public IBaseBackboneElement get() {
        return backboneElement;
    }
}
