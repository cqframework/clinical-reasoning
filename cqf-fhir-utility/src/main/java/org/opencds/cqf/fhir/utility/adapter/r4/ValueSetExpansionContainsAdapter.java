package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IValueSetExpansionContainsAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class ValueSetExpansionContainsAdapter implements IValueSetExpansionContainsAdapter {

    private final ValueSetExpansionContainsComponent contains;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public ValueSetExpansionContainsAdapter(IBaseBackboneElement contains) {
        if (!(contains instanceof ValueSetExpansionContainsComponent)) {
            throw new IllegalArgumentException(
                    "element passed as contains argument is not a ValueSetExpansionContainsComponent element");
        }
        this.contains = (ValueSetExpansionContainsComponent) contains;
        fhirContext = FhirContext.forR4Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
    }

    @Override
    public ValueSetExpansionContainsComponent get() {
        return contains;
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
