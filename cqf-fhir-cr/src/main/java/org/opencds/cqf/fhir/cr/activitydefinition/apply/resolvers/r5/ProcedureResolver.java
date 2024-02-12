package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Enumerations.EventStatus;
import org.hl7.fhir.r5.model.Procedure;
import org.hl7.fhir.r5.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class ProcedureResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ProcedureResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Procedure resolve(ICpgRequest request) {
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        var procedure = new Procedure();

        procedure.setStatus(EventStatus.UNKNOWN);
        procedure.setSubject(new Reference(request.getSubjectId()));

        if (activityDefinition.hasUrl()) {
            procedure.setInstantiatesCanonical(Collections.singletonList(new CanonicalType(activityDefinition.getUrl()
                    + (activityDefinition.hasVersion() ? String.format("|%s", activityDefinition.getVersion()) : ""))));
        }

        if (activityDefinition.hasCode()) {
            procedure.setCode(activityDefinition.getCode());
        }

        if (activityDefinition.hasBodySite()) {
            procedure.setBodySite(activityDefinition.getBodySite());
        }

        return procedure;
    }
}
