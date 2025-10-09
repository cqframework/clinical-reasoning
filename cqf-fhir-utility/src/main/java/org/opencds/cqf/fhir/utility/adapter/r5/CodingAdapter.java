package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.Coding;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;

public class CodingAdapter extends BaseAdapter implements ICodingAdapter {

    private final Coding coding;

    public CodingAdapter(IBase coding) {
        super(FhirVersionEnum.R5, coding);
        if (!(coding instanceof Coding)) {
            throw new IllegalArgumentException("object passed as coding argument is not a Coding data type");
        }
        this.coding = (Coding) coding;
    }

    @Override
    public Coding get() {
        return coding;
    }

    @Override
    public String getCode() {
        return get().getCode();
    }

    @Override
    public boolean hasCode() {
        return get().hasCode();
    }

    @Override
    public ICodingAdapter setCode(String code) {
        get().setCode(code);
        return this;
    }

    @Override
    public String getDisplay() {
        return get().getDisplay();
    }

    @Override
    public boolean hasDisplay() {
        return get().hasDisplay();
    }

    @Override
    public ICodingAdapter setDisplay(String display) {
        get().setDisplay(display);
        return this;
    }

    @Override
    public String getSystem() {
        return get().getSystem();
    }

    @Override
    public boolean hasSystem() {
        return get().hasSystem();
    }

    @Override
    public ICodingAdapter setSystem(String system) {
        get().setSystem(system);
        return this;
    }
}
