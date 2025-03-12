package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class CodeableConceptAdapter implements ICodeableConceptAdapter {

    private final CodeableConcept codeableConcept;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;
    private final AdapterFactory adapterFactory;

    public CodeableConceptAdapter(ICompositeType codeableConcept) {
        if (!(codeableConcept instanceof CodeableConcept)) {
            throw new IllegalArgumentException(
                    "object passed as codeableConcept argument is not a CodeableConcept data type");
        }
        this.codeableConcept = (CodeableConcept) codeableConcept;
        fhirContext = FhirContext.forR4Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
        adapterFactory = new AdapterFactory();
    }

    @Override
    public CodeableConcept get() {
        return codeableConcept;
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
    public List<ICodingAdapter> getCoding() {
        return get().getCoding().stream().map(adapterFactory::createCoding).toList();
    }

    @Override
    public boolean hasCoding(String code) {
        return get().getCoding().stream().anyMatch(coding -> coding.getCode().equals(code));
    }
}
