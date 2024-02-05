package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Claim;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class ClaimResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ClaimResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Claim resolve(ICpgRequest request) {
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        var claim = new Claim();

        return claim;
    }
}
