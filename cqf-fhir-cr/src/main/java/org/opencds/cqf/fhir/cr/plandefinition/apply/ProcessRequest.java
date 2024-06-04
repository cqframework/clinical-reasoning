package org.opencds.cqf.fhir.cr.plandefinition.apply;

import java.util.Collections;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.questionnaire.populate.IPopulateProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;

public class ProcessRequest {
    protected final IPopulateProcessor populateProcessor;

    public ProcessRequest(IPopulateProcessor populateProcessor) {
        this.populateProcessor = populateProcessor;
    }

    public IBaseResource generateRequestOrchestration(ApplyRequest request) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return generateOrchestrationDstu3(request);
            case R4:
                return generateOrchestrationR4(request);
            case R5:
                return generateOrchestrationR5(request);

            default:
                return null;
        }
    }

    protected IBaseResource generateOrchestrationDstu3(ApplyRequest request) {
        var planDefinition = (org.hl7.fhir.dstu3.model.PlanDefinition) request.getPlanDefinition();
        var canonical = planDefinition.getUrl();
        if (planDefinition.hasVersion()) {
            canonical = String.format("%s|%s", canonical, planDefinition.getVersion());
        }
        var requestOrchestration = new org.hl7.fhir.dstu3.model.RequestGroup()
                .setStatus(org.hl7.fhir.dstu3.model.RequestGroup.RequestStatus.DRAFT)
                .setIntent(org.hl7.fhir.dstu3.model.RequestGroup.RequestIntent.PROPOSAL)
                .addDefinition(new org.hl7.fhir.dstu3.model.Reference(canonical))
                .setSubject(new org.hl7.fhir.dstu3.model.Reference(request.getSubjectId()));
        var requestId = Ids.newId(
                request.getFhirVersion(),
                requestOrchestration.fhirType(),
                planDefinition.getIdElement().getIdPart());
        requestOrchestration.setId(requestId);
        // requestGroup.setMeta(new Meta().addProfile(Constants.CPG_STRATEGY));
        if (request.hasEncounterId()) {
            requestOrchestration.setContext(new org.hl7.fhir.dstu3.model.Reference(request.getEncounterId()));
        }
        if (request.hasPractitionerId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.dstu3.model.Reference(request.getPractitionerId()));
        }
        if (request.hasOrganizationId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.dstu3.model.Reference(request.getOrganizationId()));
        }
        if (request.getUserLanguage() instanceof org.hl7.fhir.dstu3.model.CodeableConcept) {
            requestOrchestration.setLanguage(((org.hl7.fhir.dstu3.model.CodeableConcept) request.getUserLanguage())
                    .getCodingFirstRep()
                    .getCode());
        }
        return requestOrchestration;
    }

    protected IBaseResource generateOrchestrationR4(ApplyRequest request) {
        var planDefinition = (org.hl7.fhir.r4.model.PlanDefinition) request.getPlanDefinition();
        var canonical = planDefinition.getUrl();
        if (planDefinition.hasVersion()) {
            canonical = String.format("%s|%s", canonical, planDefinition.getVersion());
        }
        var requestOrchestration = new org.hl7.fhir.r4.model.RequestGroup()
                .setStatus(org.hl7.fhir.r4.model.RequestGroup.RequestStatus.DRAFT)
                .setIntent(org.hl7.fhir.r4.model.RequestGroup.RequestIntent.PROPOSAL)
                .addInstantiatesCanonical(canonical)
                .setSubject(new org.hl7.fhir.r4.model.Reference(request.getSubjectId()));
        var requestId = Ids.newId(
                request.getFhirVersion(),
                requestOrchestration.fhirType(),
                planDefinition.getIdElement().getIdPart());
        requestOrchestration.setId(requestId);
        // requestGroup.setMeta(new Meta().addProfile(Constants.CPG_STRATEGY));
        if (request.hasEncounterId()) {
            requestOrchestration.setEncounter(new org.hl7.fhir.r4.model.Reference(request.getEncounterId()));
        }
        if (request.hasPractitionerId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.r4.model.Reference(request.getPractitionerId()));
        }
        if (request.hasOrganizationId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.r4.model.Reference(request.getOrganizationId()));
        }
        if (request.getUserLanguage() instanceof org.hl7.fhir.r4.model.CodeableConcept) {
            requestOrchestration.setLanguage(((org.hl7.fhir.r4.model.CodeableConcept) request.getUserLanguage())
                    .getCodingFirstRep()
                    .getCode());
        }
        return requestOrchestration;
    }

    protected IBaseResource generateOrchestrationR5(ApplyRequest request) {
        var planDefinition = (org.hl7.fhir.r5.model.PlanDefinition) request.getPlanDefinition();
        var canonical = planDefinition.getUrl();
        if (planDefinition.hasVersion()) {
            canonical = String.format("%s|%s", canonical, planDefinition.getVersion());
        }
        var requestOrchestration = new org.hl7.fhir.r5.model.RequestOrchestration()
                .setStatus(org.hl7.fhir.r5.model.Enumerations.RequestStatus.DRAFT)
                .setIntent(org.hl7.fhir.r5.model.Enumerations.RequestIntent.PROPOSAL)
                .addInstantiatesCanonical(canonical)
                .setSubject(new org.hl7.fhir.r5.model.Reference(request.getSubjectId()));
        var requestId = Ids.newId(
                request.getFhirVersion(),
                requestOrchestration.fhirType(),
                planDefinition.getIdElement().getIdPart());
        requestOrchestration.setId(requestId);
        // requestGroup.setMeta(new Meta().addProfile(Constants.CPG_STRATEGY));
        if (request.hasEncounterId()) {
            requestOrchestration.setEncounter(new org.hl7.fhir.r5.model.Reference(request.getEncounterId()));
        }
        if (request.hasPractitionerId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.r5.model.Reference(request.getPractitionerId()));
        }
        if (request.hasOrganizationId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.r5.model.Reference(request.getOrganizationId()));
        }
        if (request.getUserLanguage() instanceof org.hl7.fhir.r5.model.CodeableConcept) {
            requestOrchestration.setLanguage(((org.hl7.fhir.r5.model.CodeableConcept) request.getUserLanguage())
                    .getCodingFirstRep()
                    .getCode());
        }
        return requestOrchestration;
    }

    public IBaseResource generateCarePlan(ApplyRequest request, IBaseResource requestOrchestration) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return generateCarePlanDstu3(request, requestOrchestration);
            case R4:
                return generateCarePlanR4(request, requestOrchestration);
            case R5:
                return generateCarePlanR5(request, requestOrchestration);

            default:
                return null;
        }
    }

    protected IBaseResource generateCarePlanDstu3(ApplyRequest request, IBaseResource ro) {
        var requestOrchestration = (org.hl7.fhir.dstu3.model.RequestGroup) ro;
        var carePlan = new org.hl7.fhir.dstu3.model.CarePlan()
                .setDefinition(requestOrchestration.getDefinition())
                .setSubject(requestOrchestration.getSubject())
                .setStatus(org.hl7.fhir.dstu3.model.CarePlan.CarePlanStatus.DRAFT)
                .setIntent(org.hl7.fhir.dstu3.model.CarePlan.CarePlanIntent.PROPOSAL);
        var carePlanId = Ids.newId(
                request.getFhirVersion(),
                carePlan.fhirType(),
                requestOrchestration.getIdElement().getIdPart());
        carePlan.setId(carePlanId);

        if (request.hasEncounterId()) {
            carePlan.setContext(requestOrchestration.getContext());
        }
        if (request.hasPractitionerId()) {
            carePlan.setAuthor(Collections.singletonList(requestOrchestration.getAuthor()));
        }
        if (requestOrchestration.getLanguage() != null) {
            carePlan.setLanguage(requestOrchestration.getLanguage());
        }
        for (var goal : request.getRequestResources()) {
            if (goal.fhirType().equals("Goal")) {
                carePlan.addGoal(new org.hl7.fhir.dstu3.model.Reference((org.hl7.fhir.dstu3.model.Resource) goal));
            }
        }

        var operationOutcomes = request.getContained(requestOrchestration).stream()
                .filter(r -> r.fhirType().equals("OperationOutcome"))
                .collect(Collectors.toList());
        for (var operationOutcome : operationOutcomes) {
            carePlan.addExtension(
                    Constants.EXT_CRMI_MESSAGES,
                    new org.hl7.fhir.dstu3.model.Reference(
                            "#" + operationOutcome.getIdElement().getValue()));
        }

        carePlan.addActivity().setReference(new org.hl7.fhir.dstu3.model.Reference(requestOrchestration));
        carePlan.addContained(requestOrchestration);

        for (var resource : request.getExtractedResources()) {
            carePlan.addSupportingInfo(
                    new org.hl7.fhir.dstu3.model.Reference((org.hl7.fhir.dstu3.model.Resource) resource));
            carePlan.addContained((org.hl7.fhir.dstu3.model.Resource) resource);
        }

        var questionnaire = (org.hl7.fhir.dstu3.model.Questionnaire) request.getQuestionnaire();
        if (questionnaire != null && questionnaire.hasItem()) {
            carePlan.addContained((org.hl7.fhir.dstu3.model.Resource)
                    populateProcessor.processResponse(request.toPopulateRequest(), request.getItems(questionnaire)));
        }

        return carePlan;
    }

    protected IBaseResource generateCarePlanR4(ApplyRequest request, IBaseResource ro) {
        var requestOrchestration = (org.hl7.fhir.r4.model.RequestGroup) ro;
        var carePlan = new org.hl7.fhir.r4.model.CarePlan()
                .setInstantiatesCanonical(requestOrchestration.getInstantiatesCanonical())
                .setSubject(requestOrchestration.getSubject())
                .setStatus(org.hl7.fhir.r4.model.CarePlan.CarePlanStatus.DRAFT)
                .setIntent(org.hl7.fhir.r4.model.CarePlan.CarePlanIntent.PROPOSAL);
        var carePlanId = Ids.newId(
                request.getFhirVersion(),
                carePlan.fhirType(),
                requestOrchestration.getIdElement().getIdPart());
        carePlan.setId(carePlanId);

        if (request.hasEncounterId()) {
            carePlan.setEncounter(requestOrchestration.getEncounter());
        }
        if (request.hasPractitionerId()) {
            carePlan.setAuthor(requestOrchestration.getAuthor());
        }
        if (requestOrchestration.getLanguage() != null) {
            carePlan.setLanguage(requestOrchestration.getLanguage());
        }
        for (var goal : request.getRequestResources()) {
            if (goal.fhirType().equals("Goal")) {
                carePlan.addGoal(new org.hl7.fhir.r4.model.Reference((org.hl7.fhir.r4.model.Resource) goal));
            }
        }

        var operationOutcomes = request.getContained(requestOrchestration).stream()
                .filter(r -> r.fhirType().equals("OperationOutcome"))
                .collect(Collectors.toList());
        for (var operationOutcome : operationOutcomes) {
            carePlan.addExtension(
                    Constants.EXT_CRMI_MESSAGES,
                    new org.hl7.fhir.r4.model.Reference(
                            "#" + operationOutcome.getIdElement().getValue()));
        }

        carePlan.addActivity().setReference(new org.hl7.fhir.r4.model.Reference(requestOrchestration));
        carePlan.addContained(requestOrchestration);

        for (var resource : request.getExtractedResources()) {
            carePlan.addSupportingInfo(new org.hl7.fhir.r4.model.Reference((org.hl7.fhir.r4.model.Resource) resource));
            carePlan.addContained((org.hl7.fhir.r4.model.Resource) resource);
        }

        var questionnaire = (org.hl7.fhir.r4.model.Questionnaire) request.getQuestionnaire();
        if (questionnaire != null && questionnaire.hasItem()) {
            carePlan.addContained((org.hl7.fhir.r4.model.Resource)
                    populateProcessor.processResponse(request.toPopulateRequest(), request.getItems(questionnaire)));
        }

        return carePlan;
    }

    protected IBaseResource generateCarePlanR5(ApplyRequest request, IBaseResource ro) {
        var requestOrchestration = (org.hl7.fhir.r5.model.RequestOrchestration) ro;
        var carePlan = new org.hl7.fhir.r5.model.CarePlan()
                .setInstantiatesCanonical(requestOrchestration.getInstantiatesCanonical())
                .setSubject(requestOrchestration.getSubject())
                .setStatus(org.hl7.fhir.r5.model.Enumerations.RequestStatus.DRAFT)
                .setIntent(org.hl7.fhir.r5.model.CarePlan.CarePlanIntent.PROPOSAL);
        var carePlanId = Ids.newId(
                request.getFhirVersion(),
                carePlan.fhirType(),
                requestOrchestration.getIdElement().getIdPart());
        carePlan.setId(carePlanId);

        if (request.hasEncounterId()) {
            carePlan.setEncounter(requestOrchestration.getEncounter());
        }
        if (request.hasPractitionerId()) {
            carePlan.setCustodian(requestOrchestration.getAuthor());
        }
        if (requestOrchestration.getLanguage() != null) {
            carePlan.setLanguage(requestOrchestration.getLanguage());
        }
        for (var goal : request.getRequestResources()) {
            if (goal.fhirType().equals("Goal")) {
                carePlan.addGoal(new org.hl7.fhir.r5.model.Reference((org.hl7.fhir.r5.model.Resource) goal));
            }
        }

        var operationOutcomes = request.getContained(requestOrchestration).stream()
                .filter(r -> r.fhirType().equals("OperationOutcome"))
                .collect(Collectors.toList());
        for (var operationOutcome : operationOutcomes) {
            carePlan.addExtension(
                    Constants.EXT_CRMI_MESSAGES,
                    new org.hl7.fhir.r5.model.Reference(
                            "#" + operationOutcome.getIdElement().getValue()));
        }

        carePlan.addActivity().setPlannedActivityReference(new org.hl7.fhir.r5.model.Reference(requestOrchestration));
        carePlan.addContained(requestOrchestration);

        for (var resource : request.getExtractedResources()) {
            carePlan.addSupportingInfo(new org.hl7.fhir.r5.model.Reference((org.hl7.fhir.r5.model.Resource) resource));
            carePlan.addContained((org.hl7.fhir.r5.model.Resource) resource);
        }

        var questionnaire = (org.hl7.fhir.r5.model.Questionnaire) request.getQuestionnaire();
        if (questionnaire != null && questionnaire.hasItem()) {
            carePlan.addContained((org.hl7.fhir.r5.model.Resource)
                    populateProcessor.processResponse(request.toPopulateRequest(), request.getItems(questionnaire)));
        }

        return carePlan;
    }
}
