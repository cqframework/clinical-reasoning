package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;

public class CodeableConceptAdapter extends BaseAdapter implements ICodeableConceptAdapter {

    private final CodeableConcept codeableConcept;

    public CodeableConceptAdapter(IBase codeableConcept) {
        super(FhirVersionEnum.R5, codeableConcept);
        if (!(codeableConcept instanceof CodeableConcept)) {
            throw new IllegalArgumentException(
                    "object passed as codeableConcept argument is not a CodeableConcept data type");
        }
        this.codeableConcept = (CodeableConcept) codeableConcept;
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
    public boolean hasCoding() {
        return get().hasCoding();
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
