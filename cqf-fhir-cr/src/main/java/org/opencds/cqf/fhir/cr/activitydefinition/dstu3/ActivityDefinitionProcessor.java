package org.opencds.cqf.fhir.cr.activitydefinition.dstu3;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.CommunicationRequest;
import org.hl7.fhir.dstu3.model.CommunicationRequest.CommunicationRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.SupplyRequest;
import org.hl7.fhir.dstu3.model.SupplyRequest.SupplyRequestOrderedItemComponent;
import org.hl7.fhir.dstu3.model.SupplyRequest.SupplyRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.BaseActivityDefinitionProcessor;
import org.opencds.cqf.fhir.utility.dstu3.InputParameterResolver;
import org.opencds.cqf.fhir.utility.dstu3.SearchHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityDefinitionProcessor extends BaseActivityDefinitionProcessor<ActivityDefinition> {
    private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionProcessor.class);

    protected InputParameterResolver inputParameterResolver;

    public ActivityDefinitionProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public ActivityDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        super(repository, evaluationSettings);
    }

    @Override
    public <C extends IPrimitiveType<String>> ActivityDefinition resolveActivityDefinition(
            IIdType id, C canonical, IBaseResource activityDefinition) throws FHIRException {
        var baseActivityDefinition = activityDefinition;
        if (baseActivityDefinition == null) {
            baseActivityDefinition = id != null
                    ? this.repository.read(ActivityDefinition.class, id)
                    : (ActivityDefinition) SearchHelper.searchRepositoryByCanonical(repository, canonical);
        }

        requireNonNull(baseActivityDefinition, "Couldn't find ActivityDefinition " + id);

        return castOrThrow(
                        baseActivityDefinition,
                        ActivityDefinition.class,
                        "The activityDefinition passed in was not a valid instance of ActivityDefinition.class")
                .orElse(null);
    }

    @Override
    protected ActivityDefinition initApply(ActivityDefinition activityDefinition) {
        logger.info("Performing $apply operation on {}", activityDefinition.getId());

        this.inputParameterResolver = new InputParameterResolver(
                subjectId, encounterId, practitionerId, parameters, useServerData, bundle, repository);

        return activityDefinition;
    }

    @Override
    public IBaseResource applyActivityDefinition(ActivityDefinition activityDefinition) {
        DomainResource result;
        try {
            result = (DomainResource) Class.forName("org.hl7.fhir.dstu3.model."
                            + activityDefinition.getKind().toCode())
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new FHIRException("Could not find org.hl7.fhir.dstu3.model."
                    + activityDefinition.getKind().toCode());
        }

        switch (result.fhirType()) {
            case "ReferralRequest":
                result = resolveReferralRequest(activityDefinition);
                break;

            case "ProcedureRequest":
                result = resolveProcedureRequest(activityDefinition);
                break;

            case "MedicationRequest":
                result = resolveMedicationRequest(activityDefinition);
                break;

            case "SupplyRequest":
                result = resolveSupplyRequest(activityDefinition);
                break;

            case "Procedure":
                result = resolveProcedure(activityDefinition);
                break;

            case "DiagnosticReport":
                result = resolveDiagnosticReport(activityDefinition);
                break;

            case "Communication":
                result = resolveCommunication(activityDefinition);
                break;

            case "CommunicationRequest":
                result = resolveCommunicationRequest(activityDefinition);
                break;

            case "Task":
                result = resolveTask(activityDefinition);
                break;

            default:
                var msg = "Unsupported activity type: " + result.fhirType();
                logger.error(msg);
                throw new FHIRException(msg);
        }

        // Dstu3 does not have a profile property on ActivityDefinition so we are not resolving meta
        resolveExtensions(result, activityDefinition);
        var defaultLibraryUrl = activityDefinition.hasLibrary()
                ? activityDefinition.getLibrary().get(0).getReference()
                : null;
        var inputParams = inputParameterResolver.getParameters();
        for (var dynamicValue : activityDefinition.getDynamicValue()) {
            if (dynamicValue.hasExpression()) {
                var expressionResult = libraryEngine.resolveExpression(
                        subjectId,
                        new CqfExpression(dynamicValue.getLanguage(), dynamicValue.getExpression(), defaultLibraryUrl),
                        inputParams,
                        bundle);
                resolveDynamicValue(expressionResult, dynamicValue.getExpression(), dynamicValue.getPath(), result);
            }
        }

        return result;
    }

    private void resolveExtensions(DomainResource resource, ActivityDefinition activityDefinition) {
        if (activityDefinition.hasExtension()) {
            resource.setExtension(activityDefinition.getExtension().stream()
                    .filter(e -> !EXCLUDED_EXTENSION_LIST.contains(e.getUrl()))
                    .collect(Collectors.toList()));
        }
        // Extension resolution is not supported in Dstu3
    }

    private Task resolveTask(ActivityDefinition activityDefinition) throws FHIRException {
        var task = new Task();
        if (activityDefinition.hasExtension()) {
            var value = activityDefinition
                    .getExtensionsByUrl(TARGET_STATUS_URL)
                    .get(0)
                    .getValue();
            if (value instanceof StringType) {
                task.setStatus(Task.TaskStatus.valueOf(
                        ((StringType) value).asStringValue().toUpperCase()));
            } else {
                logger.debug(
                        "Extension {} should have a value of type {}", TARGET_STATUS_URL, StringType.class.getName());
            }
        } else {
            task.setStatus(Task.TaskStatus.DRAFT);
        }

        task.setIntent(Task.TaskIntent.PROPOSAL);

        if (activityDefinition.hasUrl()) {
            task.setDefinition(activityDefinition.getUrlElement());
        }

        if (activityDefinition.hasCode()) {
            task.setCode(activityDefinition.getCode());
        }

        if (activityDefinition.hasDescription()) {
            task.setDescription(activityDefinition.getDescription());
        }
        return task;
    }

    private ReferralRequest resolveReferralRequest(ActivityDefinition activityDefinition) throws FHIRException {
        // status, intent, code, and subject are required
        var referralRequest = new ReferralRequest();
        referralRequest.setStatus(ReferralRequest.ReferralRequestStatus.DRAFT);
        referralRequest.setIntent(ReferralRequest.ReferralCategory.ORDER);
        referralRequest.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            referralRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (practitionerId != null) {
            referralRequest.setRequester(new ReferralRequestRequesterComponent(new Reference(practitionerId)));
        } else if (organizationId != null) {
            referralRequest.setRequester(new ReferralRequestRequesterComponent(new Reference(organizationId)));
        }

        // code can be set as a dynamicValue
        if (activityDefinition.hasCode()) {
            referralRequest.setServiceRequested(Collections.singletonList(activityDefinition.getCode()));
        } else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new FHIRException("Missing required code property");
        }

        if (activityDefinition.hasBodySite()) {
            throw new FHIRException(BODYSITE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasProduct()) {
            throw new FHIRException(PRODUCT_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new FHIRException(DOSAGE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return referralRequest;
    }

    private ProcedureRequest resolveProcedureRequest(ActivityDefinition activityDefinition) throws FHIRException {
        // status, intent, code, and subject are required
        var procedureRequest = new ProcedureRequest();
        procedureRequest.setStatus(ProcedureRequest.ProcedureRequestStatus.DRAFT);
        procedureRequest.setIntent(ProcedureRequest.ProcedureRequestIntent.PROPOSAL);
        procedureRequest.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            procedureRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (practitionerId != null) {
            procedureRequest.setRequester(
                    new ProcedureRequest.ProcedureRequestRequesterComponent().setAgent(new Reference(practitionerId)));
        } else if (organizationId != null) {
            procedureRequest.setRequester(
                    new ProcedureRequest.ProcedureRequestRequesterComponent().setAgent(new Reference(organizationId)));
        }

        // code can be set as a dynamicValue
        if (activityDefinition.hasCode()) {
            procedureRequest.setCode(activityDefinition.getCode());
        } else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new FHIRException(MISSING_CODE_PROPERTY);
        }

        if (activityDefinition.hasBodySite()) {
            procedureRequest.setBodySite(activityDefinition.getBodySite());
        }

        if (activityDefinition.hasProduct()) {
            throw new FHIRException(PRODUCT_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new FHIRException(DOSAGE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return procedureRequest;
    }

    private MedicationRequest resolveMedicationRequest(ActivityDefinition activityDefinition) throws FHIRException {
        // intent, medication, and subject are required
        var medicationRequest = new MedicationRequest();
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            medicationRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication(activityDefinition.getProduct());
        } else {
            throw new FHIRException(MISSING_CODE_PROPERTY);
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction(activityDefinition.getDosage());
        }

        if (activityDefinition.hasBodySite()) {
            throw new FHIRException(BODYSITE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasCode()) {
            throw new FHIRException(CODE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasQuantity()) {
            throw new FHIRException(QUANTITY_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return medicationRequest;
    }

    private SupplyRequest resolveSupplyRequest(ActivityDefinition activityDefinition) throws FHIRException {
        var supplyRequest = new SupplyRequest();

        if (practitionerId != null) {
            supplyRequest.setRequester(new SupplyRequestRequesterComponent(new Reference(practitionerId)));
        }

        if (organizationId != null) {
            supplyRequest.setRequester(new SupplyRequestRequesterComponent(new Reference(organizationId)));
        }

        if (activityDefinition.hasCode()) {
            if (!activityDefinition.hasQuantity()) {
                throw new FHIRException("Missing required orderedItem.quantity property");
            }
            supplyRequest.setOrderedItem(new SupplyRequestOrderedItemComponent(activityDefinition.getQuantity())
                    .setItem(activityDefinition.getCode()));
        }

        if (activityDefinition.hasProduct()) {
            throw new FHIRException(PRODUCT_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new FHIRException(DOSAGE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasBodySite()) {
            throw new FHIRException(BODYSITE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return supplyRequest;
    }

    private Procedure resolveProcedure(ActivityDefinition activityDefinition) {
        var procedure = new Procedure();

        procedure.setStatus(Procedure.ProcedureStatus.UNKNOWN);
        procedure.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            procedure.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (activityDefinition.hasCode()) {
            procedure.setCode(activityDefinition.getCode());
        }

        if (activityDefinition.hasBodySite()) {
            procedure.setBodySite(activityDefinition.getBodySite());
        }

        return procedure;
    }

    private DiagnosticReport resolveDiagnosticReport(ActivityDefinition activityDefinition) {
        var diagnosticReport = new DiagnosticReport();

        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.UNKNOWN);
        diagnosticReport.setSubject(new Reference(subjectId));

        if (activityDefinition.hasCode()) {
            diagnosticReport.setCode(activityDefinition.getCode());
        } else {
            throw new FHIRException("Missing required ActivityDefinition.code property for DiagnosticReport");
        }

        if (activityDefinition.hasRelatedArtifact()) {
            List<Attachment> presentedFormAttachments = new ArrayList<>();
            for (var artifact : activityDefinition.getRelatedArtifact()) {
                var attachment = new Attachment();
                if (artifact.hasUrl()) {
                    attachment.setUrl(artifact.getUrl());
                }

                if (artifact.hasDisplay()) {
                    attachment.setTitle(artifact.getDisplay());
                }
                presentedFormAttachments.add(attachment);
            }
            diagnosticReport.setPresentedForm(presentedFormAttachments);
        }

        return diagnosticReport;
    }

    private Communication resolveCommunication(ActivityDefinition activityDefinition) {
        var communication = new Communication();

        communication.setStatus(Communication.CommunicationStatus.UNKNOWN);
        communication.setSubject(new Reference(subjectId));

        if (activityDefinition.hasCode()) {
            communication.setReasonCode(Collections.singletonList(activityDefinition.getCode()));
        }

        if (activityDefinition.hasRelatedArtifact()) {
            for (var artifact : activityDefinition.getRelatedArtifact()) {
                if (artifact.hasUrl()) {
                    var attachment = new Attachment().setUrl(artifact.getUrl());
                    if (artifact.hasDisplay()) {
                        attachment.setTitle(artifact.getDisplay());
                    }

                    var payload = new Communication.CommunicationPayloadComponent();
                    payload.setContent(artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
                    communication.setPayload(Collections.singletonList(payload));
                }
            }
        }

        return communication;
    }

    private CommunicationRequest resolveCommunicationRequest(ActivityDefinition activityDefinition) {
        var communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(CommunicationRequest.CommunicationRequestStatus.DRAFT);
        communicationRequest.setSubject(new Reference(subjectId));

        if (encounterId != null && !encounterId.isEmpty()) {
            communicationRequest.setContext(new Reference(encounterId));
        }

        if (practitionerId != null && !practitionerId.isEmpty()) {
            communicationRequest.setRequester(
                    new CommunicationRequestRequesterComponent(new Reference(practitionerId)));
        }

        if (activityDefinition.hasCode() && activityDefinition.getCode().hasText()) {
            communicationRequest
                    .addPayload()
                    .setContent(new StringType(activityDefinition.getCode().getText()));
        }

        return communicationRequest;
    }
}
