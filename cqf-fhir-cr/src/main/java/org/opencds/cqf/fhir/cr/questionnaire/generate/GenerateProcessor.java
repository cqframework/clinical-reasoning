package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateProcessor implements IGenerateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(GenerateProcessor.class);
    protected static final String NO_BASE_DEFINITION_ERROR =
            "An error occurred searching for base definition with url (%s): %s";
    protected final IRepository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final ItemGenerator itemGenerator;

    public GenerateProcessor(IRepository repository) {
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
        var formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        request.getQuestionnaireAdapter()
                .setVersion("%s-%s".formatted(request.getProfileAdapter().getVersion(), formatter.format(new Date())));
        var item = generateItem(request);
        if (item != null) {
            request.addQuestionnaireItem(item.getLeft());
            if (!item.getRight().isEmpty()) {
                request.addCqlLibraryExtension();
                request.addLaunchContextExtensions(item.getRight());
            }
        }
        return request.getQuestionnaire();
    }

    @Override
    public <T extends IBaseExtension<?, ?>> Pair<IBaseBackboneElement, List<T>> generateItem(GenerateRequest request) {
        logger.info(
                "Generating Questionnaire Item for StructureDefinition/{}",
                request.getProfile().getIdElement().getIdPart());
        request.setDifferentialElements(request.getProfileAdapter().getDifferentialElements().stream()
                .filter(e -> e.getId().split("\\.").length > 1)
                .collect(Collectors.toList()));
        request.setSnapshotElements(getElements(request, getProfileSnapshot(request)));
        return itemGenerator.generate(request);
    }

    protected List<IElementDefinitionAdapter> getElements(GenerateRequest request, IBase baseElement) {
        var adapterFactory = IAdapterFactory.forFhirVersion(fhirVersion);
        return baseElement == null
                ? null
                : request.resolvePathList(baseElement, "element").stream()
                        .filter(e -> request.resolvePathString(e, "path").split("\\.").length > 1)
                        .filter(ICompositeType.class::isInstance)
                        .map(ICompositeType.class::cast)
                        .map(adapterFactory::createElementDefinition)
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
        return snapshot;
    }

    protected IBaseResource createQuestionnaire() {
        switch (fhirVersion) {
            case R4:
                return new org.hl7.fhir.r4.model.Questionnaire()
                        .setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
            case R5:
                return new org.hl7.fhir.r5.model.Questionnaire()
                        .setStatus(org.hl7.fhir.r5.model.Enumerations.PublicationStatus.ACTIVE);

            default:
                return null;
        }
    }
}
