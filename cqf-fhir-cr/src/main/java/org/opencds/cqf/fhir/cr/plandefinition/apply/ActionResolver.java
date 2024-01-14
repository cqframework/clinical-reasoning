package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;

import java.util.Collections;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Ids;

public class ActionResolver {

    public void resolveAction(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            IBaseResource result,
            IBaseBackboneElement action) {
        if ("Task".equals(result.fhirType())) {
            resolveTask(request, requestOrchestration, result, action);
        }
    }

    protected void resolveTask(
            ApplyRequest request, IBaseResource requestGroup, IBaseResource task, IBaseBackboneElement action) {
        var actionId = request.resolvePathString(action, "id");
        if (actionId != null) {
            var taskId = Ids.newId(request.getFhirVersion(), task.fhirType(), actionId);
            task.setId(taskId);
        }
        request.getModelResolver()
                .setValue(
                        task,
                        "basedOn",
                        Collections.singletonList(buildReference(
                                request.getFhirVersion(),
                                requestGroup.getIdElement().getValue(),
                                false)));
        request.getModelResolver().setValue(task, "for", request.resolvePath(requestGroup, "subject"));

        // if (action.hasRelatedAction()) {
        //     var relatedActions = action.getRelatedAction();
        //     for (var relatedAction : relatedActions) {
        //         var next = new Extension();
        //         next.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/next");
        //         if (relatedAction.hasOffset()) {
        //             var offsetExtension = new Extension();
        //             offsetExtension.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/offset");
        //             offsetExtension.setValue(relatedAction.getOffset());
        //             next.addExtension(offsetExtension);
        //         }
        //         var target = new Extension();
        //         var targetRef = new Reference(new IdType(task.fhirType(), relatedAction.getActionId()));
        //         target.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/target");
        //         target.setValue(targetRef);
        //         next.addExtension(target);
        //         task.addExtension(next);
        //     }
        // }

        // if (action.hasCondition()) {
        //     var conditionComponents = action.getCondition();
        //     for (var conditionComponent : conditionComponents) {
        //         var condition = new Extension();
        //         condition.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/condition");
        //         condition.setValue(conditionComponent.getExpression());
        //         if (conditionComponent.hasExtension(Constants.ALT_EXPRESSION_EXT)) {
        //             condition.addExtension(conditionComponent.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT));
        //         }
        //         task.addExtension(condition);
        //     }
        // }

        // if (action.hasInput()) {
        //     var dataRequirements = action.getInput();
        //     for (var dataRequirement : dataRequirements) {
        //         var input = new Extension();
        //         input.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/input");
        //         input.setValue(dataRequirement);
        //         task.addExtension(input);
        //     }
        // }

        // resolvePrepopulateAction(action, requestGroup, task);
    }

    // protected void resolvePrepopulateAction(
    //         PlanDefinition.PlanDefinitionActionComponent action, RequestGroup requestGroup, Task task) {
    //     if (action.hasExtension(Constants.SDC_QUESTIONNAIRE_PREPOPULATE)) {
    //         var questionnaireBundles =
    //                 getQuestionnairePackage(action.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_PREPOPULATE));
    //         for (var questionnaireBundle : questionnaireBundles) {
    //             var toPopulate =
    //                     (Questionnaire) questionnaireBundle.getEntryFirstRep().getResource();
    //             // Bundle should contain a Questionnaire and supporting Library and ValueSet
    //             // resources
    //             var libraries = questionnaireBundle.getEntry().stream()
    //                     .filter(e -> e.hasResource()
    //                             && (e.getResource().fhirType().equals(Enumerations.FHIRAllTypes.LIBRARY.toCode())))
    //                     .map(e -> (Library) e.getResource())
    //                     .collect(Collectors.toList());
    //             var valueSets = questionnaireBundle.getEntry().stream()
    //                     .filter(e -> e.hasResource()
    //                             && (e.getResource().fhirType().equals(Enumerations.FHIRAllTypes.VALUESET.toCode())))
    //                     .map(e -> (ValueSet) e.getResource())
    //                     .collect(Collectors.toList());
    //             var additionalData =
    //                     bundle == null ? new Bundle().setType(BundleType.COLLECTION) : ((Bundle) bundle).copy();
    //             libraries.forEach(
    //                     library -> additionalData.addEntry(new Bundle.BundleEntryComponent().setResource(library)));
    //             valueSets.forEach(
    //                     valueSet -> additionalData.addEntry(new
    // Bundle.BundleEntryComponent().setResource(valueSet)));

    //             var populatedQuestionnaire = questionnaireProcessor.prePopulate(
    //                     toPopulate, subjectId.getIdPart(), this.parameters, additionalData, libraryEngine);
    //             if (Boolean.TRUE.equals(containResources)) {
    //                 requestGroup.addContained((Resource) populatedQuestionnaire);
    //             } else {
    //                 requestResources.add(populatedQuestionnaire);
    //             }
    //             task.setFocus(new Reference(new IdType(
    //                     FHIRAllTypes.QUESTIONNAIRE.toCode(),
    //                     populatedQuestionnaire.getIdElement().getIdPart())));
    //             task.setFor(requestGroup.getSubject());
    //         }
    //     }
    // }

    // protected List<Bundle> getQuestionnairePackage(Extension prepopulateExtension) {
    //     Bundle qpBundle = null;
    //     // PlanDef action should provide endpoint for $questionnaire-for-order operation
    //     // as well as
    //     // the order id to pass
    //     var parameterExtension =
    //             prepopulateExtension.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER);
    //     if (parameterExtension == null) {
    //         throw new IllegalArgumentException(String.format(
    //                 "Required extension for %s not found.", Constants.SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER));
    //     }
    //     var parameterName = parameterExtension.getValue().toString();
    //     var prepopulateParameter =
    //             this.parameters != null ? ((Parameters) this.parameters).getParameter(parameterName) : null;
    //     if (prepopulateParameter == null) {
    //         throw new IllegalArgumentException(String.format("Parameter not found: %s ", parameterName));
    //     }
    //     // var orderId = prepopulateParameter.toString();

    //     var questionnaireExtension =
    //             prepopulateExtension.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE);
    //     if (questionnaireExtension == null) {
    //         throw new IllegalArgumentException(String.format(
    //                 "Required extension for %s not found.", Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE));
    //     }

    //     if (questionnaireExtension.getValue().hasType(FHIRAllTypes.CANONICAL.toCode())) {
    //         var packageQuestionnaire = SearchHelper.searchRepositoryByCanonical(
    //                 repository, (CanonicalType) questionnaireExtension.getValue());
    //         if (packageQuestionnaire != null) {
    //             qpBundle = new Bundle().addEntry(new
    // Bundle.BundleEntryComponent().setResource(packageQuestionnaire));
    //         }
    //     } else if (questionnaireExtension.getValue().hasType(FHIRAllTypes.URL.toCode())) {
    //         // Assuming package operation endpoint if the extension is using valueUrl
    //         // instead of
    //         // valueCanonical
    //         qpBundle =
    //                 callQuestionnairePackageOperation(((UrlType)
    // questionnaireExtension.getValue()).getValueAsString());
    //     }

    //     if (qpBundle == null) {
    //         qpBundle = new Bundle();
    //     }

    //     return Collections.singletonList(qpBundle);
    // }

    // private Bundle callQuestionnairePackageOperation(String url) {
    //     String baseUrl;
    //     String operation;
    //     if (url.contains("$")) {
    //         var urlSplit = url.split("\\$");
    //         baseUrl = urlSplit[0];
    //         operation = urlSplit[1];
    //     } else {
    //         baseUrl = url;
    //         operation = "questionnaire-package";
    //     }

    //     Bundle qpBundle = null;
    //     IGenericClient client = Clients.forUrl(repository.fhirContext(), baseUrl);
    //     // Clients.registerBasicAuth(client, user, password);
    //     try {
    //         // TODO: This is not currently in use, but if it ever is we will need to
    //         // determine how the
    //         // order and coverage resources are passed in
    //         Type order = null;
    //         Type coverage = null;
    //         qpBundle = client.operation()
    //                 .onType(FHIRAllTypes.QUESTIONNAIRE.toCode())
    //                 .named('$' + operation)
    //                 .withParameters(
    //                         new Parameters().addParameter("order", order).addParameter("coverage", coverage))
    //                 .returnResourceType(Bundle.class)
    //                 .execute();
    //     } catch (Exception e) {
    //         logger.error("Error encountered calling $questionnaire-package operation: %s", e);
    //     }

    //     return qpBundle;
    // }
}
