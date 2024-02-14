package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.hl7.fhir.r5.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class ImmunizationRecommendationResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ImmunizationRecommendationResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ImmunizationRecommendation resolve(ICpgRequest request) {
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        var immunizationRecommendation = new ImmunizationRecommendation();

        immunizationRecommendation.setPatient(new Reference(request.getSubjectId()));
        immunizationRecommendation.setDate(new Date());
        if (activityDefinition.hasProductCodeableConcept()) {
            immunizationRecommendation.addRecommendation(new ImmunizationRecommendationRecommendationComponent()
                    .addVaccineCode(activityDefinition.getProductCodeableConcept())
                    .setForecastStatus(new CodeableConcept(new Coding(
                            "http://terminology.hl7.org/CodeSystem/immunization-recommendation-status",
                            "due",
                            "Due"))));
        } else if (!activityDefinition.hasDynamicValue()) {
            throw new FHIRException(String.format(MISSING_PRODUCT_PROPERTY, "ImmunizationRecommendation"));
        }

        return immunizationRecommendation;
    }
}
