package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;

public abstract class BaseElementAdapter extends BaseAdapter implements IAdapter<IBase> {
    protected final IBase element;

    protected BaseElementAdapter(FhirVersionEnum fhirVersion, IBase element) {
        super(FhirContext.forCached(fhirVersion));
        if (element == null) {
            throw new IllegalArgumentException("element can not be null");
        }
        this.element = element;
    }

    public IAdapter<?> setId(String id) {
        setValue(get(), "id", id);
        return this;
    }
}
