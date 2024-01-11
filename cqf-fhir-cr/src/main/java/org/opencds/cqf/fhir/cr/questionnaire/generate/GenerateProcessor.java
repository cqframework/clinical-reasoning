package org.opencds.cqf.fhir.cr.questionnaire.generate;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;
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
    public IBaseResource generate(ICpgRequest request, IBaseResource profile, String id) {
        var questionnaire = generate(id == null ? profile.getIdElement().getIdPart() : id);
        request.getModelResolver()
                .setValue(questionnaire, "item", Collections.singletonList(generateItem(request, profile, 0)));
        return questionnaire;
    }

    @Override
    public IBaseBackboneElement generateItem(ICpgRequest request, IBaseResource profile, int itemCount) {
        return itemGenerator.generate(request, profile, itemCount);
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
