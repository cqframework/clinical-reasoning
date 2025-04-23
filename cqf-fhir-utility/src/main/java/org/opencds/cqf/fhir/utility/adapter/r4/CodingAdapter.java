package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.Coding;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class CodingAdapter implements ICodingAdapter {

    private final Coding coding;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public CodingAdapter(ICompositeType coding) {
        if (!(coding instanceof Coding)) {
            throw new IllegalArgumentException(
                    "object passed as codeableConcept argument is not a CodeableConcept data type");
        }
        this.coding = (Coding) coding;
        fhirContext = FhirContext.forR4Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
    }

    @Override
    public Coding get() {
        return coding;
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
