package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetExpansionContainsAdapter;

public class ValueSetExpansionContainsAdapter extends BaseAdapter implements IValueSetExpansionContainsAdapter {

    private final ValueSetExpansionContainsComponent contains;

    public ValueSetExpansionContainsAdapter(IBase contains) {
        super(FhirVersionEnum.DSTU3, contains);
        if (!(contains instanceof ValueSetExpansionContainsComponent)) {
            throw new IllegalArgumentException(
                    "element passed as contains argument is not a ValueSetExpansionContainsComponent element");
        }
        this.contains = (ValueSetExpansionContainsComponent) contains;
    }

    @Override
    public ValueSetExpansionContainsComponent get() {
        return contains;
    }

    @Override
    public boolean hasCode() {
        return get().hasCode();
    }

    @Override
    public String getCode() {
        return get().getCode();
    }

    @Override
    public boolean hasSystem() {
        return get().hasSystem();
    }

    @Override
    public String getSystem() {
        return get().getSystem();
    }

    @Override
    public boolean hasDisplay() {
        return get().hasDisplay();
    }

    @Override
    public String getDisplay() {
        return get().getDisplay();
    }
}
