package org.opencds.cqf.cql.evaluator.activitydefinition.r4;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.hl7.fhir.r4.model.Task;
import org.opencds.cqf.cql.evaluator.activitydefinition.BaseActivityDefinitionProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.ExtensionResolver;
import org.opencds.cqf.fhir.utility.r4.InputParameterResolver;
import org.opencds.cqf.fhir.utility.r4.SearchHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityDefinitionProcessor
    extends BaseActivityDefinitionProcessor<ActivityDefinition> {
  private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionProcessor.class);

  protected InputParameterResolver inputParameterResolver;

  public ActivityDefinitionProcessor(Repository repository) {
    this(repository, EvaluationSettings.getDefault());
  }

  public ActivityDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
    super(repository, evaluationSettings);
  }

  @Override
  protected <C extends IPrimitiveType<String>> ActivityDefinition resolveActivityDefinition(
      IIdType d, C canonical, IBaseResource activityDefinition) throws FHIRException {
    var baseActivityDefinition = activityDefinition;
    if (baseActivityDefinition == null) {
      baseActivityDefinition = d != null ? this.repository.read(ActivityDefinition.class, d)
          : (ActivityDefinition) SearchHelper.searchRepositoryByCanonical(repository, canonical);
    }

    requireNonNull(baseActivityDefinition, "Couldn't find ActivityDefinition " + d);

    return castOrThrow(baseActivityDefinition, ActivityDefinition.class,
        "The activityDefinition passed in was not a valid instance of ActivityDefinition.class")
            .orElse(null);
  }

  @Override
  protected ActivityDefinition initApply(ActivityDefinition activityDefinition) {
    logger.info("Performing $apply operation on {}", activityDefinition.getId());

    this.inputParameterResolver =
        new InputParameterResolver(subjectId, encounterId, practitionerId, parameters,
            useServerData, bundle, repository);
    this.extensionResolver = new ExtensionResolver(subjectId,
        inputParameterResolver.getParameters(), bundle, libraryEngine);

    return activityDefinition;
  }

  @Override
  protected IBaseResource applyActivityDefinition(ActivityDefinition activityDefinition) {
    DomainResource result;
    try {
      result =
          (DomainResource) Class
              .forName("org.hl7.fhir.r4.model." + activityDefinition.getKind().toCode())
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

    resolveMeta(result, activityDefinition);
    var defaultLibraryUrl =
        activityDefinition.hasLibrary() ? activityDefinition.getLibrary().get(0).getValueAsString()
            : null;
    resolveExtensions(result, activityDefinition, defaultLibraryUrl);
    var inputParams = inputParameterResolver.getParameters();
    for (var dynamicValue : activityDefinition.getDynamicValue()) {
      if (dynamicValue.hasExpression()) {
        var expression = dynamicValue.getExpression();
        var expressionResult = libraryEngine.resolveExpression(subjectId,
            new CqfExpression(expression.getLanguage(), expression.getExpression(),
                expression.hasReference() ? expression.getReference() : defaultLibraryUrl),
            inputParams, bundle);
        resolveDynamicValue(expressionResult, expression.getExpression(),
            dynamicValue.getPath(), result);
      }
    }

    return result;
  }

  private void resolveMeta(DomainResource resource, ActivityDefinition activityDefinition) {
    var meta = new Meta();
    // Consider setting source and lastUpdated here?
    // .setLastUpdated(new Date());
    if (activityDefinition.hasProfile()) {
      meta.addProfile(activityDefinition.getProfile());
      resource.setMeta(meta);
    }
  }

  private void resolveExtensions(DomainResource resource, ActivityDefinition activityDefinition,
      String defaultLibraryUrl) {
    if (activityDefinition.hasExtension()) {
      resource.setExtension(activityDefinition.getExtension().stream()
          .filter(e -> !EXCLUDED_EXTENSION_LIST.contains(e.getUrl())).collect(Collectors.toList()));
      extensionResolver.resolveExtensions(resource.getExtension(), defaultLibraryUrl);
    }
  }

  private Task resolveTask(ActivityDefinition activityDefinition) {
    var task = new Task();
    if (activityDefinition.hasExtension(BaseActivityDefinitionProcessor.TARGET_STATUS_URL)) {
      var value = activityDefinition
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

    task.setIntent(activityDefinition.hasIntent()
        ? Task.TaskIntent.fromCode(activityDefinition.getIntent().toCode())
        : Task.TaskIntent.PROPOSAL);

    if (activityDefinition.hasUrl()) {
      task.setInstantiatesCanonical(activityDefinition.getUrl());
    }

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
    var serviceRequest = new ServiceRequest();
    serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
    serviceRequest.setIntent(activityDefinition.hasIntent()
        ? ServiceRequest.ServiceRequestIntent.fromCode(activityDefinition.getIntent().toCode())
        : ServiceRequest.ServiceRequestIntent.ORDER);
    serviceRequest.setSubject(new Reference(subjectId));

    if (activityDefinition.hasUrl()) {
      serviceRequest.setInstantiatesCanonical(
          Collections.singletonList(new CanonicalType(activityDefinition.getUrl())));
    }

    if (practitionerId != null) {
      serviceRequest.setRequester(new Reference(practitionerId));
    } else if (organizationId != null) {
      serviceRequest.setRequester(new Reference(organizationId));
    }

    if (activityDefinition.hasExtension()) {
      serviceRequest.setExtension(activityDefinition.getExtension());
    }

    // code can be set as a dynamicValue
    if (activityDefinition.hasCode()) {
      serviceRequest.setCode(activityDefinition.getCode());
    } else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
      throw new FHIRException(MISSING_CODE_PROPERTY);
    }

    if (activityDefinition.hasBodySite()) {
      serviceRequest.setBodySite(activityDefinition.getBodySite());
    }

    if (activityDefinition.hasDoNotPerform()) {
      serviceRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
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
    var medicationRequest = new MedicationRequest();
    medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.DRAFT);
    medicationRequest.setIntent(activityDefinition.hasIntent()
        ? MedicationRequest.MedicationRequestIntent
            .fromCode(activityDefinition.getIntent().toCode())
        : MedicationRequest.MedicationRequestIntent.ORDER);
    medicationRequest.setSubject(new Reference(subjectId));

    if (activityDefinition.hasUrl()) {
      medicationRequest.setInstantiatesCanonical(
          Collections.singletonList(new CanonicalType(activityDefinition.getUrl())));
    }

    if (activityDefinition.hasProduct()) {
      medicationRequest.setMedication(activityDefinition.getProduct());
    } else {
      throw new FHIRException(MISSING_CODE_PROPERTY);
    }

    if (activityDefinition.hasDosage()) {
      medicationRequest.setDosageInstruction(activityDefinition.getDosage());
    }

    if (activityDefinition.hasDoNotPerform()) {
      medicationRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
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
    var supplyRequest = new SupplyRequest();

    supplyRequest.setStatus(SupplyRequest.SupplyRequestStatus.DRAFT);

    if (practitionerId != null) {
      supplyRequest.setRequester(new Reference(practitionerId));
    }

    if (organizationId != null) {
      supplyRequest.setRequester(new Reference(organizationId));
    }

    if (activityDefinition.hasQuantity()) {
      supplyRequest.setQuantity(activityDefinition.getQuantity());
    } else {
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
    var procedure = new Procedure();

    procedure.setStatus(Procedure.ProcedureStatus.UNKNOWN);
    procedure.setSubject(new Reference(subjectId));

    if (activityDefinition.hasUrl()) {
      procedure.setInstantiatesCanonical(
          Collections.singletonList(new CanonicalType(activityDefinition.getUrl())));
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
      throw new FHIRException(
          "Missing required ActivityDefinition.code property for DiagnosticReport");
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
          payload.setContent(
              artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
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
      communicationRequest.setEncounter(new Reference(encounterId));
    }

    if (practitionerId != null && !practitionerId.isEmpty()) {
      communicationRequest.setRequester(new Reference(practitionerId));
    }

    if (activityDefinition.hasDoNotPerform()) {
      communicationRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
    }

    if (activityDefinition.hasCode() && activityDefinition.getCode().hasText()) {
      communicationRequest.addPayload()
          .setContent(new StringType(activityDefinition.getCode().getText()));
    }

    return communicationRequest;
  }
}
