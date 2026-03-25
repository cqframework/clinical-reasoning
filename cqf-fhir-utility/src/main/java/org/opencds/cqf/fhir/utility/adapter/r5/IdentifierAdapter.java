package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.Identifier;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IIdentifierAdapter;

public class IdentifierAdapter extends BaseAdapter implements IIdentifierAdapter {

    private final Identifier identifier;

    public IdentifierAdapter(IBase identifier) {
        super(FhirVersionEnum.R5, identifier);
        if (!(identifier instanceof Identifier)) {
            throw new IllegalArgumentException("object passed as identifier argument is not an Identifier data type");
        }
        this.identifier = (Identifier) identifier;
    }

    @Override
    public Identifier get() {
        return identifier;
    }

    @Override
    public String getValue() {
        return get().getValue();
    }

    @Override
    public boolean hasValue() {
        return get().hasValue();
    }

    @Override
    public String getSystem() {
        return get().getSystem();
    }

    @Override
    public boolean hasSystem() {
        return get().hasSystem();
    }
}
