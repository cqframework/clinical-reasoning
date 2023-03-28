package org.opencds.cqf.cql.evaluator.activitydefinition.r4;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.SearchHelper.searchRepositoryByCanonical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.evaluator.activitydefinition.BaseActivityDefinitionProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityDefinitionProcessor
    extends BaseActivityDefinitionProcessor<ActivityDefinition> {
  private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionProcessor.class);

  public ActivityDefinitionProcessor(Repository repository) {
    super(repository);
  }

  @Override
  public <C extends IPrimitiveType<String>> ActivityDefinition resolveActivityDefinition(
      IIdType theId, C theCanonical, IBaseResource theActivityDefinition) throws FHIRException {
    var baseActivityDefinition = theActivityDefinition;
    if (baseActivityDefinition == null) {
      baseActivityDefinition = theId != null ? this.repository.read(ActivityDefinition.class, theId)
          : (ActivityDefinition) searchRepositoryByCanonical(repository, theCanonical);
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
      result =
          (Resource) Class.forName("org.hl7.fhir.r4.model." + activityDefinition.getKind().toCode())
              .getConstructor().newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      throw new FHIRException(
          "Could not find org.hl7.fhir.r4.model." + activityDefinition.getKind().toCode());
    }

    switch (result.fhirType()) {
      case "ServiceRequest":
        result = resolveServiceRequest(activityDefinition);
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

    String subjectCode = null;
    if (activityDefinition.hasSubjectCodeableConcept()) {
      var concept = activityDefinition.getSubjectCodeableConcept();
      if (concept.hasCoding()) {
        subjectCode = concept.getCoding().get(0).getCode();
      }
    }
    var subjectType = subjectCode != null ? subjectCode : "Patient";
    for (ActivityDefinition.ActivityDefinitionDynamicValueComponent dynamicValue : activityDefinition
        .getDynamicValue()) {
      if (dynamicValue.hasExpression()) {
        resolveDynamicValue(dynamicValue.getExpression().getLanguage(),
            dynamicValue.getExpression().getExpression(),
            activityDefinition.getLibrary().get(0).getValueAsString(), dynamicValue.getPath(),
            result, subjectType);
      }
    }

    return result;
  }

  private Task resolveTask(ActivityDefinition activityDefinition) {
    Task task = new Task();
    if (activityDefinition.hasExtension(BaseActivityDefinitionProcessor.TARGET_STATUS_URL)) {
      Type value = activityDefinition
          .getExtensionByUrl(BaseActivityDefinitionProcessor.TARGET_STATUS_URL).getValue();
      if (value instanceof StringType) {
        task.setStatus(Task.TaskStatus.valueOf(((StringType) value).asStringValue().toUpperCase()));
      } else {
        logger.debug("Extension {} should have a value of type {}",
            BaseActivityDefinitionProcessor.TARGET_STATUS_URL, StringType.class.getName());
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

  private ServiceRequest resolveServiceRequest(ActivityDefinition activityDefinition) {
    // status, intent, code, and subject are required
    ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
    serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
    serviceRequest.setSubject(new Reference(subjectId));

    if (practitionerId != null) {
      serviceRequest.setRequester(new Reference(practitionerId));
    }

    else if (organizationId != null) {
      serviceRequest.setRequester(new Reference(organizationId));
    }

    if (activityDefinition.hasExtension()) {
      serviceRequest.setExtension(activityDefinition.getExtension());
    }

    if (activityDefinition.hasCode()) {
      serviceRequest.setCode(activityDefinition.getCode());
    }

    // code can be set as a dynamicValue
    else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
      throw new FHIRException(MISSING_CODE_PROPERTY);
    }

    if (activityDefinition.hasBodySite()) {
      serviceRequest.setBodySite(activityDefinition.getBodySite());
    }

    if (activityDefinition.hasProduct()) {
      throw new FHIRException(PRODUCT_ERROR_PREAMBLE + activityDefinition.getKind());
    }

    if (activityDefinition.hasDosage()) {
      throw new FHIRException(DOSAGE_ERROR_PREAMBLE + activityDefinition.getKind());
    }

    return serviceRequest;
  }

  private MedicationRequest resolveMedicationRequest(ActivityDefinition activityDefinition) {
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

  private SupplyRequest resolveSupplyRequest(ActivityDefinition activityDefinition) {
    SupplyRequest supplyRequest = new SupplyRequest();

    if (practitionerId != null) {
      supplyRequest.setRequester(new Reference(practitionerId));
    }

    if (organizationId != null) {
      supplyRequest.setRequester(new Reference(organizationId));
    }

    if (activityDefinition.hasQuantity()) {
      supplyRequest.setQuantity(activityDefinition.getQuantity());
    }

    else {
      throw new FHIRException("Missing required orderedItem.quantity property");
    }

    if (activityDefinition.hasCode()) {
      supplyRequest.setItem(activityDefinition.getCode());
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
