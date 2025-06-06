package org.opencds.cqf.fhir.cr.plandefinition.apply;

import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.questionnaire.populate.IPopulateProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;

public class ResponseBuilder {
    protected final IPopulateProcessor populateProcessor;

    public ResponseBuilder(IPopulateProcessor populateProcessor) {
        this.populateProcessor = populateProcessor;
    }

    public IBaseResource generateRequestOrchestration(ApplyRequest request) {
        return switch (request.getFhirVersion()) {
            case DSTU3 -> generateOrchestrationDstu3(request);
            case R4 -> generateOrchestrationR4(request);
            case R5 -> generateOrchestrationR5(request);
            default -> null;
        };
    }

    protected String getCanonical(String url, String version) {
        return StringUtils.isBlank(version) ? url : "%s|%s".formatted(url, version);
    }

    protected IBaseResource generateOrchestrationDstu3(ApplyRequest request) {
        var planDefinition = (org.hl7.fhir.dstu3.model.PlanDefinition) request.getPlanDefinition();
        var canonical = getCanonical(planDefinition.getUrl(), planDefinition.getVersion());
        var requestOrchestration = new org.hl7.fhir.dstu3.model.RequestGroup()
                .setStatus(org.hl7.fhir.dstu3.model.RequestGroup.RequestStatus.ACTIVE)
                .setIntent(org.hl7.fhir.dstu3.model.RequestGroup.RequestIntent.PROPOSAL)
                .addDefinition(new org.hl7.fhir.dstu3.model.Reference(canonical))
                .setSubject(new org.hl7.fhir.dstu3.model.Reference(request.getSubjectId()));
        var requestId = Ids.newId(
                request.getFhirVersion(),
                requestOrchestration.fhirType(),
                planDefinition.getIdElement().getIdPart());
        requestOrchestration.setId(requestId);
        if (request.hasEncounterId()) {
            requestOrchestration.setContext(new org.hl7.fhir.dstu3.model.Reference(request.getEncounterId()));
        }
        if (request.hasPractitionerId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.dstu3.model.Reference(request.getPractitionerId()));
        }
        if (request.hasOrganizationId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.dstu3.model.Reference(request.getOrganizationId()));
        }
        if (request.getUserLanguage() instanceof org.hl7.fhir.dstu3.model.CodeableConcept userLanguage) {
            requestOrchestration.setLanguage(userLanguage.getCodingFirstRep().getCode());
        }
        return requestOrchestration;
    }

    protected IBaseResource generateOrchestrationR4(ApplyRequest request) {
        var planDefinition = (org.hl7.fhir.r4.model.PlanDefinition) request.getPlanDefinition();
        var canonical = getCanonical(planDefinition.getUrl(), planDefinition.getVersion());
        var requestOrchestration = new org.hl7.fhir.r4.model.RequestGroup()
                .setStatus(org.hl7.fhir.r4.model.RequestGroup.RequestStatus.ACTIVE)
                .setIntent(org.hl7.fhir.r4.model.RequestGroup.RequestIntent.PROPOSAL)
                .addInstantiatesCanonical(canonical)
                .setSubject(new org.hl7.fhir.r4.model.Reference(request.getSubjectId()));
        var requestId = Ids.newId(
                request.getFhirVersion(),
                requestOrchestration.fhirType(),
                planDefinition.getIdElement().getIdPart());
        requestOrchestration.setId(requestId);
        if (request.hasEncounterId()) {
            requestOrchestration.setEncounter(new org.hl7.fhir.r4.model.Reference(request.getEncounterId()));
        }
        if (request.hasPractitionerId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.r4.model.Reference(request.getPractitionerId()));
        }
        if (request.hasOrganizationId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.r4.model.Reference(request.getOrganizationId()));
        }
        if (request.getUserLanguage() instanceof org.hl7.fhir.r4.model.CodeableConcept userLanguage) {
            requestOrchestration.setLanguage(userLanguage.getCodingFirstRep().getCode());
        }
        return requestOrchestration;
    }

    protected IBaseResource generateOrchestrationR5(ApplyRequest request) {
        var planDefinition = (org.hl7.fhir.r5.model.PlanDefinition) request.getPlanDefinition();
        var canonical = getCanonical(planDefinition.getUrl(), planDefinition.getVersion());
        var requestOrchestration = new org.hl7.fhir.r5.model.RequestOrchestration()
                .setStatus(org.hl7.fhir.r5.model.Enumerations.RequestStatus.ACTIVE)
                .setIntent(org.hl7.fhir.r5.model.Enumerations.RequestIntent.PROPOSAL)
                .addInstantiatesCanonical(canonical)
                .setSubject(new org.hl7.fhir.r5.model.Reference(request.getSubjectId()));
        var requestId = Ids.newId(
                request.getFhirVersion(),
                requestOrchestration.fhirType(),
                planDefinition.getIdElement().getIdPart());
        requestOrchestration.setId(requestId);
        if (request.hasEncounterId()) {
            requestOrchestration.setEncounter(new org.hl7.fhir.r5.model.Reference(request.getEncounterId()));
        }
        if (request.hasPractitionerId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.r5.model.Reference(request.getPractitionerId()));
        }
        if (request.hasOrganizationId()) {
            requestOrchestration.setAuthor(new org.hl7.fhir.r5.model.Reference(request.getOrganizationId()));
        }
        if (request.getUserLanguage() instanceof org.hl7.fhir.r5.model.CodeableConcept userLanguage) {
            requestOrchestration.setLanguage(userLanguage.getCodingFirstRep().getCode());
        }
        return requestOrchestration;
    }

    public IBaseResource generateCarePlan(ApplyRequest request, IBaseResource requestOrchestration) {
        return switch (request.getFhirVersion()) {
            case DSTU3 -> generateCarePlanDstu3(request, requestOrchestration);
            case R4 -> generateCarePlanR4(request, requestOrchestration);
            case R5 -> generateCarePlanR5(request, requestOrchestration);
            default -> null;
        };
    }

    protected IBaseResource generateCarePlanDstu3(ApplyRequest request, IBaseResource ro) {
        var requestOrchestration = (org.hl7.fhir.dstu3.model.RequestGroup) ro;
        var carePlan = new org.hl7.fhir.dstu3.model.CarePlan()
                .setDefinition(requestOrchestration.getDefinition())
                .setSubject(requestOrchestration.getSubject())
                .setStatus(org.hl7.fhir.dstu3.model.CarePlan.CarePlanStatus.ACTIVE)
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
            if (goal.fhirType().equals(org.hl7.fhir.dstu3.model.ResourceType.Goal.name())) {
                carePlan.addGoal(new org.hl7.fhir.dstu3.model.Reference((org.hl7.fhir.dstu3.model.Resource) goal));
            }
        }

        var operationOutcomes = request.getContained(requestOrchestration).stream()
                .filter(r -> r.fhirType().equals(org.hl7.fhir.dstu3.model.ResourceType.OperationOutcome.name()))
                .toList();
        for (var operationOutcome : operationOutcomes) {
            carePlan.addExtension(
                    Constants.EXT_CRMI_MESSAGES,
                    new org.hl7.fhir.dstu3.model.Reference(
                            "#" + operationOutcome.getIdElement().getIdPart()));
        }

        carePlan.addActivity()
                .setReference(new org.hl7.fhir.dstu3.model.Reference("#" + requestOrchestration.getIdPart()));
        carePlan.addContained(requestOrchestration);

        for (var resource : request.getExtractedResources()) {
            carePlan.addSupportingInfo(
                    new org.hl7.fhir.dstu3.model.Reference((org.hl7.fhir.dstu3.model.Resource) resource));
            carePlan.addContained((org.hl7.fhir.dstu3.model.Resource) resource);
        }

        return carePlan;
    }

    protected IBaseResource generateCarePlanR4(ApplyRequest request, IBaseResource ro) {
        var requestOrchestration = (org.hl7.fhir.r4.model.RequestGroup) ro;
        var carePlan = new org.hl7.fhir.r4.model.CarePlan()
                .setInstantiatesCanonical(requestOrchestration.getInstantiatesCanonical())
                .setSubject(requestOrchestration.getSubject())
                .setStatus(org.hl7.fhir.r4.model.CarePlan.CarePlanStatus.ACTIVE)
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
            if (goal.fhirType().equals(org.hl7.fhir.r4.model.ResourceType.Goal.name())) {
                carePlan.addGoal(new org.hl7.fhir.r4.model.Reference((org.hl7.fhir.r4.model.Resource) goal));
            }
        }

        var operationOutcomes = request.getContained(requestOrchestration).stream()
                .filter(r -> r.fhirType().equals(org.hl7.fhir.r4.model.ResourceType.OperationOutcome.name()))
                .toList();
        for (var operationOutcome : operationOutcomes) {
            carePlan.addExtension(
                    Constants.EXT_CRMI_MESSAGES,
                    new org.hl7.fhir.r4.model.Reference(
                            "#" + operationOutcome.getIdElement().getIdPart()));
        }

        carePlan.addActivity()
                .setReference(new org.hl7.fhir.r4.model.Reference("#" + requestOrchestration.getIdPart()));
        carePlan.addContained(requestOrchestration);

        for (var resource : request.getExtractedResources()) {
            carePlan.addSupportingInfo(new org.hl7.fhir.r4.model.Reference((org.hl7.fhir.r4.model.Resource) resource));
            carePlan.addContained((org.hl7.fhir.r4.model.Resource) resource);
        }

        var questionnaire = (org.hl7.fhir.r4.model.Questionnaire) request.getQuestionnaire();
        if (questionnaire != null && questionnaire.hasItem()) {
            carePlan.addContained(questionnaire);
        }

        return carePlan;
    }

    protected IBaseResource generateCarePlanR5(ApplyRequest request, IBaseResource ro) {
        var requestOrchestration = (org.hl7.fhir.r5.model.RequestOrchestration) ro;
        var carePlan = new org.hl7.fhir.r5.model.CarePlan()
                .setInstantiatesCanonical(requestOrchestration.getInstantiatesCanonical())
                .setSubject(requestOrchestration.getSubject())
                .setStatus(org.hl7.fhir.r5.model.Enumerations.RequestStatus.ACTIVE)
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
            if (goal.fhirType().equals(org.hl7.fhir.r5.model.ResourceType.Goal.name())) {
                carePlan.addGoal(new org.hl7.fhir.r5.model.Reference((org.hl7.fhir.r5.model.Resource) goal));
            }
        }

        var operationOutcomes = request.getContained(requestOrchestration).stream()
                .filter(r -> r.fhirType().equals(org.hl7.fhir.r5.model.ResourceType.OperationOutcome.name()))
                .toList();
        for (var operationOutcome : operationOutcomes) {
            carePlan.addExtension(
                    Constants.EXT_CRMI_MESSAGES,
                    new org.hl7.fhir.r5.model.Reference(
                            "#" + operationOutcome.getIdElement().getIdPart()));
        }

        carePlan.addActivity()
                .setPlannedActivityReference(
                        new org.hl7.fhir.r5.model.Reference("#" + requestOrchestration.getIdPart()));
        carePlan.addContained(requestOrchestration);

        for (var resource : request.getExtractedResources()) {
            carePlan.addSupportingInfo(new org.hl7.fhir.r5.model.Reference((org.hl7.fhir.r5.model.Resource) resource));
            carePlan.addContained((org.hl7.fhir.r5.model.Resource) resource);
        }

        var questionnaire = (org.hl7.fhir.r5.model.Questionnaire) request.getQuestionnaire();
        if (questionnaire != null && questionnaire.hasItem()) {
            carePlan.addContained(questionnaire);
        }

        return carePlan;
    }
}
