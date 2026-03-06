package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptReferenceAdapter;

public class ValueSetConceptReferenceAdapter extends BaseAdapter implements IValueSetConceptReferenceAdapter {

    private final ConceptReferenceComponent conceptReference;

    public ValueSetConceptReferenceAdapter(IBase conceptReference) {
        super(FhirVersionEnum.DSTU3, conceptReference);
        if (!(conceptReference instanceof ConceptReferenceComponent)) {
            throw new IllegalArgumentException(
                    "element passed as conceptReference argument is not a ConceptReferenceComponent element");
        }
        this.conceptReference = (ConceptReferenceComponent) conceptReference;
    }

    @Override
    public ConceptReferenceComponent get() {
        return conceptReference;
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
    public String getDisplay() {
        return get().getDisplay();
    }
}
