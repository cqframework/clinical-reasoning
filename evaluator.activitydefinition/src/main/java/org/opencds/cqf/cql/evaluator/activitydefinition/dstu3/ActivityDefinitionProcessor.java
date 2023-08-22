package org.opencds.cqf.cql.evaluator.activitydefinition.dstu3;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.CommunicationRequest;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.SupplyRequest;
import org.hl7.fhir.dstu3.model.SupplyRequest.SupplyRequestOrderedItemComponent;
import org.hl7.fhir.dstu3.model.SupplyRequest.SupplyRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.evaluator.activitydefinition.BaseActivityDefinitionProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.dstu3.SearchHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityDefinitionProcessor
    extends BaseActivityDefinitionProcessor<ActivityDefinition> {
  private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionProcessor.class);

  public ActivityDefinitionProcessor(Repository repository) {
    this(repository, EvaluationSettings.getDefault());
  }

  public ActivityDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
    super(repository, evaluationSettings);
  }

  @Override
  public <C extends IPrimitiveType<String>> ActivityDefinition resolveActivityDefinition(
      IIdType theId, C theCanonical, IBaseResource theActivityDefinition) throws FHIRException {
    var baseActivityDefinition = theActivityDefinition;
    if (baseActivityDefinition == null) {
      baseActivityDefinition = theId != null ? this.repository.read(ActivityDefinition.class, theId)
          : (ActivityDefinition) SearchHelper.searchRepositoryByCanonical(repository, theCanonical);
    }

    requireNonNull(baseActivityDefinition, "Couldn't find ActivityDefinition " + theId);

    var activityDefinition = castOrThrow(baseActivityDefinition, ActivityDefinition.class,
        "The activityDefinition passed to Repository was not a valid instance of ActivityDefinition.class")
            .orElse(null);

    logger.info("Performing $apply operation on {}", theId);

    return activityDefinition;
  }

  @Override
  public IBaseResource applyActivityDefinition(ActivityDefinition activityDefinition) {
    Resource result;
    try {
      result = (Resource) Class
          .forName("org.hl7.fhir.dstu3.model." + activityDefinition.getKind().toCode())
          .getConstructor().newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      throw new FHIRException(
          "Could not find org.hl7.fhir.dstu3.model." + activityDefinition.getKind().toCode());
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

    var defaultLibraryUrl =
        activityDefinition.hasLibrary() ? activityDefinition.getLibrary().get(0).getReference()
            : null;
    for (var dynamicValue : activityDefinition.getDynamicValue()) {
      if (dynamicValue.hasExpression()) {
        resolveDynamicValue(dynamicValue.getLanguage(), dynamicValue.getExpression(),
            defaultLibraryUrl, dynamicValue.getPath(), result,
            "Patient");
      }
    }

    return result;
  }

  private Task resolveTask(ActivityDefinition activityDefinition) throws FHIRException {
    Task task = new Task();
    if (activityDefinition.hasExtension()) {
      Type value = activityDefinition.getExtensionsByUrl(TARGET_STATUS_URL).get(0).getValue();
      if (value instanceof StringType) {
        task.setStatus(Task.TaskStatus.valueOf(((StringType) value).asStringValue().toUpperCase()));
      } else {
        logger.debug("Extension {} should have a value of type {}", TARGET_STATUS_URL,
            StringType.class.getName());
      }
    } else {
      task.setStatus(Task.TaskStatus.DRAFT);
    }

    task.setIntent(Task.TaskIntent.PROPOSAL);

    if (activityDefinition.hasCode()) {
      task.setCode(activityDefinition.getCode());
    }

    if (activityDefinition.hasExtension()) {
      task.setExtension(activityDefinition.getExtension());
    }

    if (activityDefinition.hasDescription()) {
      task.setDescription(activityDefinition.getDescription());
    }
    return task;
  }

  private ReferralRequest resolveReferralRequest(ActivityDefinition activityDefinition)
      throws FHIRException {
    // status, intent, code, and subject are required
    var referralRequest = new ReferralRequest();
    referralRequest.setStatus(ReferralRequest.ReferralRequestStatus.DRAFT);
    referralRequest.setIntent(ReferralRequest.ReferralCategory.ORDER);
    referralRequest.setSubject(new Reference(subjectId));

    if (practitionerId != null) {
      referralRequest
          .setRequester(new ReferralRequestRequesterComponent(new Reference(practitionerId)));
    }

    else if (organizationId != null) {
      referralRequest
          .setRequester(new ReferralRequestRequesterComponent(new Reference(organizationId)));
    }

    if (activityDefinition.hasExtension()) {
      referralRequest.setExtension(activityDefinition.getExtension());
    }

    if (activityDefinition.hasCode()) {
      referralRequest.setServiceRequested(Collections.singletonList(activityDefinition.getCode()));
    }

    // code can be set as a dynamicValue
    else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
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

  private ProcedureRequest resolveProcedureRequest(ActivityDefinition activityDefinition)
      throws FHIRException {
    // status, intent, code, and subject are required
    var procedureRequest = new ProcedureRequest();
    procedureRequest.setStatus(ProcedureRequest.ProcedureRequestStatus.DRAFT);
    procedureRequest.setIntent(ProcedureRequest.ProcedureRequestIntent.PROPOSAL);
    procedureRequest.setSubject(new Reference(subjectId));

    if (practitionerId != null) {
      procedureRequest.setRequester(new ProcedureRequest.ProcedureRequestRequesterComponent()
          .setAgent(new Reference(practitionerId)));
    }

    else if (organizationId != null) {
      procedureRequest.setRequester(new ProcedureRequest.ProcedureRequestRequesterComponent()
          .setAgent(new Reference(organizationId)));
    }

    if (activityDefinition.hasExtension()) {
      procedureRequest.setExtension(activityDefinition.getExtension());
    }

    if (activityDefinition.hasCode()) {
      procedureRequest.setCode(activityDefinition.getCode());
    }

    // code can be set as a dynamicValue
    else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
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

  private MedicationRequest resolveMedicationRequest(ActivityDefinition activityDefinition)
      throws FHIRException {
    // intent, medication, and subject are required
    MedicationRequest medicationRequest = new MedicationRequest();
    medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
    medicationRequest.setSubject(new Reference(subjectId));

    if (activityDefinition.hasProduct()) {
      medicationRequest.setMedication(activityDefinition.getProduct());
    }

    else {
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

  private SupplyRequest resolveSupplyRequest(ActivityDefinition activityDefinition)
      throws FHIRException {
    SupplyRequest supplyRequest = new SupplyRequest();

    if (practitionerId != null) {
      supplyRequest
          .setRequester(new SupplyRequestRequesterComponent(new Reference(practitionerId)));
    }

    if (organizationId != null) {
      supplyRequest
          .setRequester(new SupplyRequestRequesterComponent(new Reference(organizationId)));
    }

    if (activityDefinition.hasCode()) {
      if (!activityDefinition.hasQuantity()) {
        throw new FHIRException("Missing required orderedItem.quantity property");
      }
      supplyRequest
          .setOrderedItem(new SupplyRequestOrderedItemComponent(activityDefinition.getQuantity())
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
    Procedure procedure = new Procedure();

    procedure.setStatus(Procedure.ProcedureStatus.UNKNOWN);
    procedure.setSubject(new Reference(subjectId));

    if (activityDefinition.hasCode()) {
      procedure.setCode(activityDefinition.getCode());
    }

    if (activityDefinition.hasBodySite()) {
      procedure.setBodySite(activityDefinition.getBodySite());
    }

    return procedure;
  }

  private DiagnosticReport resolveDiagnosticReport(ActivityDefinition activityDefinition) {
    DiagnosticReport diagnosticReport = new DiagnosticReport();

    diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.UNKNOWN);
    diagnosticReport.setSubject(new Reference(subjectId));

    if (activityDefinition.hasCode()) {
      diagnosticReport.setCode(activityDefinition.getCode());
    }

    else {
      throw new FHIRException(
          "Missing required ActivityDefinition.code property for DiagnosticReport");
    }

    if (activityDefinition.hasRelatedArtifact()) {
      List<Attachment> presentedFormAttachments = new ArrayList<>();
      for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
        Attachment attachment = new Attachment();

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
    Communication communication = new Communication();

    communication.setStatus(Communication.CommunicationStatus.UNKNOWN);
    communication.setSubject(new Reference(subjectId));

    if (activityDefinition.hasCode()) {
      communication.setReasonCode(Collections.singletonList(activityDefinition.getCode()));
    }

    if (activityDefinition.hasRelatedArtifact()) {
      for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
        if (artifact.hasUrl()) {
          Attachment attachment = new Attachment().setUrl(artifact.getUrl());
          if (artifact.hasDisplay()) {
            attachment.setTitle(artifact.getDisplay());
          }

          Communication.CommunicationPayloadComponent payload =
              new Communication.CommunicationPayloadComponent();
          payload.setContent(
              artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
          communication.setPayload(Collections.singletonList(payload));
        }

      }
    }

    return communication;
  }

  private CommunicationRequest resolveCommunicationRequest(ActivityDefinition activityDefinition) {
    CommunicationRequest communicationRequest = new CommunicationRequest();

    communicationRequest.setStatus(CommunicationRequest.CommunicationRequestStatus.UNKNOWN);
    communicationRequest.setSubject(new Reference(subjectId));

    if (activityDefinition.hasCode() && activityDefinition.getCode().hasText()) {
      communicationRequest.addPayload()
          .setContent(new StringType(activityDefinition.getCode().getText()));
    }

    return communicationRequest;
  }
}
