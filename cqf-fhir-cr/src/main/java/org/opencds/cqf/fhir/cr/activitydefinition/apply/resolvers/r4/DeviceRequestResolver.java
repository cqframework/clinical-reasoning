package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.DeviceRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyRequest;

public class DeviceRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public DeviceRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public DeviceRequest resolve(IApplyRequest request) {
        var deviceRequest = new DeviceRequest();

        return deviceRequest;
    }
}
