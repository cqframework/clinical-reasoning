package org.opencds.cqf.fhir.cr.questionnaire.generate;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateProcessor implements IGenerateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(GenerateProcessor.class);
    protected final Repository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final ItemGenerator itemGenerator;

    public GenerateProcessor(Repository repository) {
        this.repository = repository;
        this.fhirVersion = repository.fhirContext().getVersion().getVersion();
        itemGenerator = new ItemGenerator(repository);
    }

    @Override
    public IBaseResource generate(String id) {
        var questionnaire = createQuestionnaire();
        if (id != null) {
            var newId = Ids.newId(fhirVersion, Ids.ensureIdType(id, "Questionnaire"));
            questionnaire.setId(newId);
        }
        return questionnaire;
    }

    @Override
    public IBaseResource generate(GenerateRequest request, IBaseResource profile, String id) {
        request.setQuestionnaire(generate(id == null ? profile.getIdElement().getIdPart() : id));
        request.addQuestionnaireItem(generateItem(request, profile));
        return request.getQuestionnaire();
    }

    @Override
    public IBaseBackboneElement generateItem(GenerateRequest request, IBaseResource profile) {
        return itemGenerator.generate(request, profile);
    }

    protected IBaseResource createQuestionnaire() {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Questionnaire();
            case R4:
                return new org.hl7.fhir.r4.model.Questionnaire();
            case R5:
                return new org.hl7.fhir.r5.model.Questionnaire();

            default:
                return null;
        }
    }
}
