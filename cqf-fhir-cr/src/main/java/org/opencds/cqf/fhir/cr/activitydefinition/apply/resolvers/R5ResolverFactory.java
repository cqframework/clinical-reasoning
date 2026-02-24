package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.ResourceType;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.AppointmentResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.AppointmentResponseResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.CarePlanResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.ClaimResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.CommunicationRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.CommunicationResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.CoverageEligibilityRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.DeviceRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.DiagnosticReportResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.EnrollmentRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.ImmunizationRecommendationResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.MedicationRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.NutritionOrderResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.ProcedureResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.RequestOrchestrationResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.ServiceRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.SupplyRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.TaskResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.TransportResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5.VisionPrescriptionResolver;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R5ResolverFactory implements IRequestResolverFactory {
    private static final Logger logger = LoggerFactory.getLogger(R5ResolverFactory.class);

    @Override
    public BaseRequestResourceResolver create(IBaseResource baseActivityDefinition) {
        var activityDefinition = (ActivityDefinition) baseActivityDefinition;
        var kind = activityDefinition.hasExtension(Constants.CPG_ACTIVITY_KIND)
                ? activityDefinition
                        .getExtensionByUrl(Constants.CPG_ACTIVITY_KIND)
                        .getValueAsPrimitive()
                        .getValueAsString()
                : activityDefinition.getKind().toCode();
        var resourceType = ResourceType.fromCode(kind);
        switch (resourceType) {
            case Appointment:
                return new AppointmentResolver(activityDefinition);
            case AppointmentResponse:
                return new AppointmentResponseResolver(activityDefinition);
            case CarePlan:
                return new CarePlanResolver(activityDefinition);
            case Claim:
                return new ClaimResolver(activityDefinition);
            /* Communication is not included in the list of RequestResourceTypes */
            case Communication:
                return new CommunicationResolver(activityDefinition);
            case CommunicationRequest:
                return new CommunicationRequestResolver(activityDefinition);
            case CoverageEligibilityRequest:
                return new CoverageEligibilityRequestResolver(activityDefinition);
            case DeviceRequest:
                return new DeviceRequestResolver(activityDefinition);
            /* DiagnosticReport is not included in the list of RequestResourceTypes */
            case DiagnosticReport:
                return new DiagnosticReportResolver(activityDefinition);
            case EnrollmentRequest:
                return new EnrollmentRequestResolver(activityDefinition);
            case ImmunizationRecommendation:
                return new ImmunizationRecommendationResolver(activityDefinition);
            case MedicationRequest:
                return new MedicationRequestResolver(activityDefinition);
            case NutritionOrder:
                return new NutritionOrderResolver(activityDefinition);
            /* Procedure is not included in the list of RequestResourceTypes */
            case Procedure:
                return new ProcedureResolver(activityDefinition);
            case RequestOrchestration:
                return new RequestOrchestrationResolver(activityDefinition);
            case ServiceRequest:
                return new ServiceRequestResolver(activityDefinition);
            case SupplyRequest:
                return new SupplyRequestResolver(activityDefinition);
            case Task:
                return new TaskResolver(activityDefinition);
            case Transport:
                return new TransportResolver(activityDefinition);
            case VisionPrescription:
                return new VisionPrescriptionResolver(activityDefinition);
            default:
                var msg = "Unsupported activity type: " + resourceType.name();
                logger.error(msg);
                throw new FHIRException(msg);
        }
    }
}
