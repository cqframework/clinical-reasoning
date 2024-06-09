package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.pertainToGoalExtension;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;
import static org.opencds.cqf.fhir.utility.VersionUtilities.stringTypeForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.uriTypeForVersion;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.QuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplyProcessor implements IApplyProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ApplyProcessor.class);
    protected static final List<String> EXCLUDED_EXTENSION_LIST = Arrays.asList(
            Constants.CPG_KNOWLEDGE_CAPABILITY,
            Constants.CPG_KNOWLEDGE_REPRESENTATION_LEVEL,
            Constants.CQFM_SOFTWARE_SYSTEM,
            Constants.CPG_QUESTIONNAIRE_GENERATE,
            Constants.CQFM_LOGIC_DEFINITION,
            Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS);

    protected final Repository repository;
    protected final ModelResolver modelResolver;
    protected final ExtensionProcessor extensionProcessor;
    protected final GenerateProcessor generateProcessor;
    protected final PopulateProcessor populateProcessor;
    protected final QuestionnaireResponseProcessor extractProcessor;
    protected final ProcessRequest processRequest;
    protected final ProcessGoal processGoal;
    protected final ProcessAction processAction;
    protected final org.opencds.cqf.fhir.cr.activitydefinition.apply.IApplyProcessor activityProcessor;

    public ApplyProcessor(
            Repository repository,
            ModelResolver modelResolver,
            org.opencds.cqf.fhir.cr.activitydefinition.apply.IApplyProcessor activityProcessor) {
        this.repository = repository;
        this.modelResolver = modelResolver;
        this.activityProcessor = activityProcessor;
        extensionProcessor = new ExtensionProcessor();
        generateProcessor = new GenerateProcessor(this.repository);
        populateProcessor = new PopulateProcessor();
        extractProcessor = new QuestionnaireResponseProcessor(this.repository);
        processRequest = new ProcessRequest(populateProcessor);
        processGoal = new ProcessGoal();
        processAction = new ProcessAction(this.repository, this, generateProcessor);
    }

    /*
     * Steps
     *  extract QuestionnaireResponse from request and add resources to bundle
     *  generate Questionnaire for request
     *
     *  generate RequestOrchestration
     *  resolve PlanDefinition.extension and copy to RequestOrchestration
     *  resolve Goals and add to requestResources
     *  resolve actions
     *      generate Questionnaire items for action.input
     *      if condition is met
     *          generate requestAction
     *          resolve requestAction extensions
     *          resolve child actions
     *          resolve definition and add resource to requestResources
     *          resolve dynamicValues
     */

    @Override
    public IBaseResource apply(ApplyRequest request) {
        request.setContainResources(true);
        initApply(request);
        var requestOrchestration = applyPlanDefinition(request);
        request.resolveOperationOutcome(requestOrchestration);
        var carePlan = processRequest.generateCarePlan(request, requestOrchestration);

        return liftContainedResourcesToParent(request, carePlan);
    }

    @Override
    public IBaseBundle applyR5(ApplyRequest request) {
        initApply(request);
        var requestOrchestration = applyPlanDefinition(request);
        request.resolveOperationOutcome(requestOrchestration);
        var resultBundle = newBundle(
                request.getFhirVersion(), requestOrchestration.getIdElement().getIdPart(), null);
        addEntry(resultBundle, newEntryWithResource(request.getFhirVersion(), requestOrchestration));
        for (var resource : request.getRequestResources()) {
            addEntry(resultBundle, newEntryWithResource(request.getFhirVersion(), resource));
        }
        // for (var resource : request.getExtractedResources()) {
        //     addEntry(resultBundle, newEntryWithResource(request.getFhirVersion(), resource));
        // }
        if (!request.getItems(request.getQuestionnaire()).isEmpty()) {
            // var populateRequest = request.toPopulateRequest();
            // var questionnaireResponse = populateProcessor.processResponse(populateRequest,
            // request.getItems(request.getQuestionnaire()));
            // var responseItems = populateProcessor.processResponseItems(populateRequest,
            // request.getItems(request.getQuestionnaire()));
            addEntry(
                    resultBundle,
                    newEntryWithResource(
                            request.getFhirVersion(),
                            populateProcessor.processResponse(
                                    request.toPopulateRequest(), request.getItems(request.getQuestionnaire()))));
        }

        return resultBundle;
    }

    protected void initApply(ApplyRequest request) {
        var questionnaire = generateProcessor.generate(
                request.getPlanDefinition().getIdElement().getIdPart());
        var url = request.resolvePathString(request.getPlanDefinition(), "url");
        if (url != null) {
            request.getModelResolver().setValue(questionnaire, "url", uriTypeForVersion(request.getFhirVersion(), url));
        }
        var version = request.resolvePathString(request.getPlanDefinition(), "version");
        if (version != null) {
            var subject = request.getSubjectId().getValue();
            subject = subject.contains("/") ? subject.split("/")[1] : subject;
            var formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ssZ");
            request.getModelResolver()
                    .setValue(
                            questionnaire,
                            "version",
                            stringTypeForVersion(
                                    request.getFhirVersion(),
                                    version.concat(String.format("-%s-%s", subject, formatter.format(new Date())))));
        }
        request.setQuestionnaire(questionnaire);
        extractQuestionnaireResponse(request);
    }

    protected void extractQuestionnaireResponse(ApplyRequest request) {
        if (request.getData() == null) {
            return;
        }

        var questionnaireResponses = getEntryResources(request.getData()).stream()
                .filter(r -> r.fhirType().equals("QuestionnaireResponse"))
                .collect(Collectors.toList());
        if (questionnaireResponses != null && !questionnaireResponses.isEmpty()) {
            for (var questionnaireResponse : questionnaireResponses) {
                try {
                    var extractBundle = extractProcessor.extract(
                            Eithers.forRight(questionnaireResponse),
                            request.getParameters(),
                            request.getData(),
                            request.getLibraryEngine());
                    // request.getExtractedResources().add(questionnaireResponse);
                    for (var entry : getEntry(extractBundle)) {
                        addEntry(request.getData(), entry);
                        // Not adding extracted resources back into the response to reduce size of payload
                        // $extract can be called on the QuestionnaireResponse if these are desired
                        // request.getExtractedResources().add(getEntryResource(request.getFhirVersion(), entry));
                    }
                } catch (Exception e) {
                    request.logException(String.format(
                            "Error encountered extracting %s: %s",
                            questionnaireResponse.getIdElement().getIdPart(), e.getMessage()));
                }
            }
        }
    }

    public IBaseResource applyPlanDefinition(ApplyRequest request) {
        logger.info(
                "Performing $apply operation on PlanDefinition/{}",
                request.getPlanDefinition().getIdElement().getIdPart());

        var requestOrchestration = processRequest.generateRequestOrchestration(request);
        extensionProcessor.processExtensions(
                request, requestOrchestration, request.getPlanDefinition(), EXCLUDED_EXTENSION_LIST);
        processGoals(request, requestOrchestration);
        var metConditions = new HashMap<String, IBaseBackboneElement>();
        for (var action : request.resolvePathList(request.getPlanDefinition(), "action", IBaseBackboneElement.class)) {
            // TODO - Apply input/output dataRequirements?
            request.getModelResolver()
                    .setValue(
                            requestOrchestration,
                            "action",
                            Collections.singletonList(
                                    processAction.processAction(request, requestOrchestration, metConditions, action)));
        }

        return Boolean.TRUE.equals(request.getContainResources())
                ? liftContainedResourcesToParent(request, requestOrchestration)
                : requestOrchestration;
    }

    public IBaseResource applyActivityDefinition(
            org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyRequest request) {
        return liftContainedResourcesToParent(request, activityProcessor.apply(request));
    }

    protected void processGoals(ApplyRequest request, IBaseResource requestOrchestration) {
        var goals = request.resolvePathList(request.getPlanDefinition(), "goal", IBaseBackboneElement.class);
        for (int i = 0; i < goals.size(); i++) {
            var goal = processGoal.convertGoal(request, goals.get(i));
            if (Boolean.TRUE.equals(request.getContainResources())) {
                request.getModelResolver().setValue(requestOrchestration, "contained", Collections.singletonList(goal));
            } else {
                goal.setId((IIdType) Ids.newId(request.getFhirVersion(), "Goal", String.valueOf(i + 1)));
                request.getModelResolver()
                        .setValue(
                                requestOrchestration,
                                "extension",
                                Collections.singletonList(buildReferenceExt(
                                        request.getFhirVersion(),
                                        pertainToGoalExtension(
                                                goal.getIdElement().getValue()),
                                        request.getContainResources())));
            }
            // Always add goals to the resource list so they can be added to the CarePlan if needed
            request.getRequestResources().add(goal);
        }
    }

    protected IBaseResource liftContainedResourcesToParent(ICpgRequest request, IBaseResource resource) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return org.opencds.cqf.fhir.utility.dstu3.ContainedHelper.liftContainedResourcesToParent(
                        (org.hl7.fhir.dstu3.model.DomainResource) resource);
            case R4:
                return org.opencds.cqf.fhir.utility.r4.ContainedHelper.liftContainedResourcesToParent(
                        (org.hl7.fhir.r4.model.DomainResource) resource);
            case R5:
                return org.opencds.cqf.fhir.utility.r5.ContainedHelper.liftContainedResourcesToParent(
                        (org.hl7.fhir.r5.model.DomainResource) resource);

            default:
                break;
        }

        return resource;
    }
}
