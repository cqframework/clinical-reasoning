package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.utility.Resources.newBaseForVersion;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class GenerateProcessor implements IGenerateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(GenerateProcessor.class);
    protected static final String NO_BASE_DEFINITION_ERROR =
            "An error occurred searching for base definition with url ({}): {}";
    protected final IRepository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final IAdapterFactory adapterFactory;
    protected final ItemGenerator itemGenerator;

    public GenerateProcessor(IRepository repository) {
        this.repository = repository;
        fhirVersion = repository.fhirContext().getVersion().getVersion();
        adapterFactory = IAdapterFactory.forFhirVersion(fhirVersion);
        itemGenerator = new ItemGenerator(repository);
    }

    @Override
    public IBaseResource generate(String id) {
        var questionnaire = createQuestionnaire();
        if (id != null) {
            var newId = Ids.newId(fhirVersion, Ids.ensureIdType(id, "Questionnaire"));
            questionnaire.setId(newId);
        }
        return questionnaire.get();
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
            item.getLeft();
            request.addQuestionnaireItem(item.getLeft());
            if (!item.getRight().isEmpty()) {
                request.addCqlLibraryExtension();
                request.addLaunchContextExtensions(item.getRight());
            }
        }
        return request.getQuestionnaire();
    }

    @Override
    public <T extends IBaseExtension<?, ?>> Pair<IQuestionnaireItemComponentAdapter, List<T>> generateItem(
            GenerateRequest request) {
        logger.info(
                "Generating Questionnaire Item for StructureDefinition/{}",
                request.getProfile().getIdElement().getIdPart());
        request.setDifferentialElements(request.getProfileAdapter().getDifferentialElements());
        request.setSnapshotElements(getSnapshotElements(request));
        return itemGenerator.generate(request);
    }

    protected List<IElementDefinitionAdapter> getSnapshotElements(GenerateRequest request) {
        if (!request.getProfileAdapter().hasSnapshot()) {
            var baseUrl = request.getProfileAdapter().getBaseDefinition();
            if (baseUrl != null) {
                IStructureDefinitionAdapter baseProfile = null;
                try {
                    baseProfile = request.getAdapterFactory()
                            .createStructureDefinition(searchRepositoryByCanonical(repository, baseUrl));
                } catch (Exception e) {
                    logger.debug(NO_BASE_DEFINITION_ERROR, baseUrl.getValueAsString(), e.getMessage());
                }
                if (baseProfile != null && baseProfile.hasSnapshot()) {
                    return baseProfile.getSnapshotElements();
                }
            }
            return Collections.emptyList();
        } else {
            return request.getProfileAdapter().getSnapshotElements();
        }
    }

    protected IQuestionnaireAdapter createQuestionnaire() {
        var questionnaire =
                adapterFactory.createQuestionnaire((IBaseResource) newBaseForVersion("Questionnaire", fhirVersion));
        questionnaire.setStatus("active");
        return questionnaire;
    }
}
