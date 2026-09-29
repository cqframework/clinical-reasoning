package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;

public class ElementAdapter extends BaseElementAdapter {

    private final IBase base;

    public ElementAdapter(FhirVersionEnum fhirVersion, IBase element) {
        super(fhirVersion, element);
        base = element;
    }

    @Override
    public IBase get() {
        return base;
    }
}
