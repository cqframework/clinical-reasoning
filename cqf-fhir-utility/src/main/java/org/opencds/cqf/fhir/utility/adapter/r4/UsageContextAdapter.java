package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.UsageContext;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IUsageContextAdapter;

public class UsageContextAdapter extends BaseAdapter implements IUsageContextAdapter {

    private final UsageContext usageContext;

    public UsageContextAdapter(IBase usageContext) {
        super(FhirVersionEnum.R4, usageContext);
        if (!(usageContext instanceof UsageContext)) {
            throw new IllegalArgumentException("object passed as coding argument is not a UsageContext data type");
        }
        this.usageContext = (UsageContext) usageContext;
    }

    @Override
    public UsageContext get() {
        return usageContext;
    }

    @Override
    public boolean hasCode() {
        return usageContext.hasCode();
    }

    @Override
    public ICodingAdapter getCode() {
        if (usageContext == null || usageContext.getCode() == null) return null;
        return new CodingAdapter(get().getCode());
    }

    @Override
    public IUsageContextAdapter setCode(ICodingAdapter code) {
        get().setCode((Coding) code.get());
        return this;
    }

    @Override
    public boolean hasValue() {
        return usageContext.hasValue();
    }

    @Override
    public boolean hasValueCodeableConcept() {
        return usageContext != null && usageContext.hasValue() && usageContext.getValue() instanceof CodeableConcept;
    }

    @Override
    public ICodeableConceptAdapter getValueCodeableConcept() {
        if (!hasValueCodeableConcept()) return null;
        CodeableConcept valueCodeableConcept = usageContext.getValueCodeableConcept();
        return new CodeableConceptAdapter(valueCodeableConcept);
    }

    @Override
    public boolean equalsDeep(IBase obj) {
        if (!(obj instanceof UsageContextAdapter usageContextAdapter)) return false;
        return get().equalsDeep(usageContextAdapter.get());
    }
}
