package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptReferenceAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptSetAdapter;

public class ValueSetConceptSetAdapter extends BaseAdapter implements IValueSetConceptSetAdapter {

    private final ConceptSetComponent conceptSet;

    public ValueSetConceptSetAdapter(IBase conceptSet) {
        super(FhirVersionEnum.DSTU3, conceptSet);
        if (!(conceptSet instanceof ConceptSetComponent)) {
            throw new IllegalArgumentException(
                    "element passed as conceptSet argument is not a ConceptSetComponent element");
        }
        this.conceptSet = (ConceptSetComponent) conceptSet;
    }

    @Override
    public ConceptSetComponent get() {
        return conceptSet;
    }

    @Override
    public boolean hasConcept() {
        return get().hasConcept();
    }

    @Override
    public List<IValueSetConceptReferenceAdapter> getConcept() {
        return get().getConcept().stream()
                .map(ValueSetConceptReferenceAdapter::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasSystem() {
        return get().hasSystem();
    }

    @Override
    public String getSystem() {
        return get().getSystem();
    }
}
