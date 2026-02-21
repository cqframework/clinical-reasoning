package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers;

import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.CommunicationRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.CommunicationResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.DiagnosticReportResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.MedicationRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.ProcedureRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.ProcedureResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.ReferralRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.SupplyRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3.TaskResolver;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dstu3ResolverFactory implements IRequestResolverFactory {
    private static final Logger logger = LoggerFactory.getLogger(Dstu3ResolverFactory.class);

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
            /* Communication is not included in the list of RequestResourceTypes */
            case Communication:
                return new CommunicationResolver(activityDefinition);
            case CommunicationRequest:
                return new CommunicationRequestResolver(activityDefinition);
            /* DiagnosticReport is not included in the list of RequestResourceTypes */
            case DiagnosticReport:
                return new DiagnosticReportResolver(activityDefinition);
            case MedicationRequest:
                return new MedicationRequestResolver(activityDefinition);
            /* Procedure is not included in the list of RequestResourceTypes */
            case Procedure:
                return new ProcedureResolver(activityDefinition);
            case ProcedureRequest:
                return new ProcedureRequestResolver(activityDefinition);
            case ReferralRequest:
                return new ReferralRequestResolver(activityDefinition);
            case SupplyRequest:
                return new SupplyRequestResolver(activityDefinition);
            case Task:
                return new TaskResolver(activityDefinition);
            default:
                var msg = "Unsupported activity type: " + resourceType.name();
                logger.error(msg);
                throw new FHIRException(msg);
        }
    }
}
