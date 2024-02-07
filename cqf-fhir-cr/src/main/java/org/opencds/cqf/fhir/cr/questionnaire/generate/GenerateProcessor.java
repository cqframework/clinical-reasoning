package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateProcessor implements IGenerateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(GenerateProcessor.class);
    protected static final String NO_BASE_DEFINITION_ERROR =
            "An error occurred searching for base definition with url (%s): %s";
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
    public IBaseResource generate(GenerateRequest request, String id) {
        request.setQuestionnaire(
                generate(id == null ? request.getProfile().getIdElement().getIdPart() : id));
        request.addQuestionnaireItem(generateItem(request));
        return request.getQuestionnaire();
    }

    @Override
    public IBaseBackboneElement generateItem(GenerateRequest request) {
        request.setDifferentialElements(
                getElements(request, request.resolvePath(request.getProfile(), "differential")));
        request.setSnapshotElements(getElements(request, getProfileSnapshot(request)));
        return itemGenerator.generate(request);
    }

    @SuppressWarnings("unchecked")
    protected <E extends ICompositeType> List<E> getElements(GenerateRequest request, IBase baseElement) {
        return baseElement == null
                ? null
                : request.resolvePathList(baseElement, "element").stream()
                        .filter(e -> request.resolvePathString(e, "path").split("\\.").length > 1)
                        .map(e -> (E) e)
                        .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected IBase getProfileSnapshot(GenerateRequest request) {
        var snapshot = request.resolvePath(request.getProfile(), "snapshot");
        if (snapshot == null) {
            // Grab the snapshot from the baseDefinition
            var baseUrl = request.resolvePath(request.getProfile(), "baseDefinition", IPrimitiveType.class);
            if (baseUrl != null) {
                IBaseResource baseProfile = null;
                try {
                    baseProfile = searchRepositoryByCanonical(repository, baseUrl);
                } catch (Exception e) {
                    logger.debug(NO_BASE_DEFINITION_ERROR, baseUrl.getValueAsString(), e);
                }
                if (baseProfile != null) {
                    snapshot = request.resolvePath(baseProfile, "snapshot");
                }
            }
        }
        // generateProfile in the implementations of IFhirVersion does not create a snapshot and hapi has no
        // implementation of the $snapshot operation.
        // We can use the definition to construct a snapshot, but that should be done in hapi-fhir
        // if (snapshot == null) {
        //     var type = request.resolvePathString(profile, "type");
        //     var definition = repository.fhirContext().getResourceDefinition(request.getFhirVersion(), type);
        //     var typeProfile = definition == null ? null : definition.toProfile(null);
        //     if (typeProfile != null) {
        //         snapshot = request.resolvePath(typeProfile, "snapshot");
        //     }
        // }
        return snapshot;
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
