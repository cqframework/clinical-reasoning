package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class CodeableConceptAdapter implements ICodeableConceptAdapter {

    private final CodeableConcept codeableConcept;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public CodeableConceptAdapter(ICompositeType codeableConcept) {
        if (!(codeableConcept instanceof CodeableConcept)) {
            throw new IllegalArgumentException(
                    "object passed as codeableConcept argument is not a CodeableConcept data type");
        }
        this.codeableConcept = (CodeableConcept) codeableConcept;
        this.fhirContext = FhirContext.forDstu3Cached();
        this.modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.DSTU3);
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

    @SuppressWarnings("unchecked")
    @Override
    public List<Coding> getCoding() {
        return get().getCoding();
    }

    @Override
    public boolean hasCoding(String code) {
        return get().getCoding().stream().anyMatch(coding -> coding.getCode().equals(code));
    }
}