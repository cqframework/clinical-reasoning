package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.AppointmentResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.AppointmentResponseResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.CarePlanResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ClaimResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.CommunicationRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.CommunicationResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ContractResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.DeviceRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.DiagnosticReportResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.EnrollmentRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ImmunizationRecommendationResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.MedicationRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.NutritionOrderResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ProcedureResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.RequestGroupResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ServiceRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.SupplyRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.TaskResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.VisionPrescriptionResolver;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4ResolverFactory implements IRequestResolverFactory {
    private static final Logger logger = LoggerFactory.getLogger(R4ResolverFactory.class);

    @Override
    public BaseRequestResourceResolver create(IBaseResource baseActivityDefinition) {
        var activityDefinition = (ActivityDefinition) baseActivityDefinition;
        // if (activityDefinition.hasExtension(Constants.CPG_CUSTOM_ACTIVITY_KIND)) {
        //     return new CustomActivityResolver(activityDefinition);
        // }
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
            case Contract:
                return new ContractResolver(activityDefinition);
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
            case RequestGroup:
                return new RequestGroupResolver(activityDefinition);
            case ServiceRequest:
                return new ServiceRequestResolver(activityDefinition);
            case SupplyRequest:
                return new SupplyRequestResolver(activityDefinition);
            case Task:
                return new TaskResolver(activityDefinition);
            case VisionPrescription:
                return new VisionPrescriptionResolver(activityDefinition);
            default:
                var msg = "Unsupported activity type: " + resourceType;
                logger.error(msg);
                throw new FHIRException(msg);
        }
    }
}
