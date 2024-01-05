package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.build;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.crmiMessagesExtension;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.pertainToGoalExtension;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResource;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;

import java.util.Arrays;
import java.util.Collections;
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
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.QuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ApplyProcessor implements IApplyProcessor {
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
    protected final QuestionnaireResponseProcessor extractProcessor;
    protected final ProcessGoal processGoal;
    protected final ProcessAction processAction;

    public ApplyProcessor(Repository repository, ModelResolver modelResolver) {
        this.repository = repository;
        this.modelResolver = modelResolver;
        extensionProcessor = new ExtensionProcessor();
        generateProcessor = new GenerateProcessor(this.repository, this.modelResolver);
        extractProcessor = new QuestionnaireResponseProcessor(this.repository);
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
        resolveOperationOutcome(request, requestOrchestration);
        var carePlan = generateCarePlan(request, requestOrchestration);

        return liftContainedResourcesToParent(request, carePlan);
    }

    @Override
    public IBaseBundle applyR5(ApplyRequest request) {
        initApply(request);
        var requestOrchestration = applyPlanDefinition(request);
        resolveOperationOutcome(request, requestOrchestration);
        var resultBundle = newBundle(
                request.getFhirVersion(), requestOrchestration.getIdElement().getIdPart(), null);
        addEntry(resultBundle, newEntryWithResource(request.getFhirVersion(), requestOrchestration));
        for (var resource : request.getRequestResources()) {
            addEntry(resultBundle, newEntryWithResource(request.getFhirVersion(), resource));
        }
        for (var resource : request.getExtractedResources()) {
            addEntry(resultBundle, newEntryWithResource(request.getFhirVersion(), resource));
        }
        if (!request.getItems(request.getQuestionnaire()).isEmpty()) {
            addEntry(resultBundle, newEntryWithResource(request.getFhirVersion(), request.getQuestionnaire()));
        }

        return resultBundle;
    }

    protected void initApply(ApplyRequest request) {
        request.setQuestionnaire(generateProcessor.generate(
                request.getPlanDefinition().getIdElement().getIdPart()));
        extractQuestionnaireResponse(request);
    }

    protected void extractQuestionnaireResponse(ApplyRequest request) {
        if (request.getBundle() == null) {
            return;
        }

        var questionnaireResponses = getEntryResources(request.getBundle()).stream()
                .filter(r -> r.fhirType().equals("QuestionnaireResponse"))
                .collect(Collectors.toList());
        if (questionnaireResponses != null && !questionnaireResponses.isEmpty()) {
            for (var questionnaireResponse : questionnaireResponses) {
                try {
                    var extractBundle = extractProcessor.extract(
                            Eithers.forRight(questionnaireResponse),
                            request.getParameters(),
                            request.getBundle(),
                            request.getLibraryEngine());
                    request.getExtractedResources().add(questionnaireResponse);
                    for (var entry : getEntry(extractBundle)) {
                        addEntry(request.getBundle(), entry);
                        request.getExtractedResources().add(getEntryResource(request.getFhirVersion(), entry));
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
        var requestOrchestration = generateRequestOrchestration(request);
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
                                Collections.singletonList(build(
                                        request.getFhirVersion(),
                                        pertainToGoalExtension(
                                                goal.getIdElement().getIdPart()))));
            }
            // Always add goals to the resource list so they can be added to the CarePlan if needed
            request.getRequestResources().add(goal);
        }
    }

    protected void resolveOperationOutcome(ApplyRequest request, IBaseResource resource) {
        var issues = request.resolvePathList(request.getOperationOutcome(), "issue");
        if (issues != null && !issues.isEmpty()) {
            request.getOperationOutcome()
                    .setId("apply-outcome-" + resource.getIdElement().getIdPart());
            request.getModelResolver()
                    .setValue(resource, "contained", Collections.singletonList(request.getOperationOutcome()));
            request.getModelResolver()
                    .setValue(
                            resource,
                            "extension",
                            Collections.singletonList(build(
                                    request.getFhirVersion(),
                                    crmiMessagesExtension(request.getOperationOutcome()
                                            .getIdElement()
                                            .getIdPart()))));
        }
    }

    protected IBaseResource generateRequestOrchestration(ApplyRequest request) {
        return null;
    }

    protected IBaseResource generateCarePlan(ApplyRequest request, IBaseResource requestOrchestration) {
        return null;
    }

    protected IBaseResource liftContainedResourcesToParent(ApplyRequest request, IBaseResource resource) {

        return resource;
    }
}
