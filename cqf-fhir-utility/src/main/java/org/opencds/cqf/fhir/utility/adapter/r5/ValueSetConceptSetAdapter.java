package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptReferenceAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptSetAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class ValueSetConceptSetAdapter implements IValueSetConceptSetAdapter {

    private final ConceptSetComponent conceptSet;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public ValueSetConceptSetAdapter(IBaseBackboneElement conceptSet) {
        if (!(conceptSet instanceof ConceptSetComponent)) {
            throw new IllegalArgumentException(
                    "element passed as conceptSet argument is not a ConceptSetComponent element");
        }
        this.conceptSet = (ConceptSetComponent) conceptSet;
        fhirContext = FhirContext.forR5Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R5);
    }

    @Override
    public ConceptSetComponent get() {
        return conceptSet;
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
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

    public boolean hasSystem() {
        return get().hasSystem();
    }

    @Override
    public String getSystem() {
        return get().getSystem();
    }
}
