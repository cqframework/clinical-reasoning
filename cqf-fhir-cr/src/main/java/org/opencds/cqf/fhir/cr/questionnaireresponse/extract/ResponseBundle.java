package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class ResponseBundle {
    private ResponseBundle() {}

    public static IBaseBundle createBundleR4(String extractId, List<IBaseResource> resources) {
        var newBundle = new org.hl7.fhir.r4.model.Bundle().setType(org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION);
        newBundle.setId(extractId);
        // ensure entry array
        newBundle.getEntry();
        resources.forEach(resource -> newBundle.addEntry(new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent()
                .setResource((org.hl7.fhir.r4.model.Resource) resource)
                .setRequest(new org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST))));

        return newBundle;
    }

    public static IBaseBundle createBundleR5(String extractId, List<IBaseResource> resources) {
        var newBundle = new org.hl7.fhir.r5.model.Bundle().setType(org.hl7.fhir.r5.model.Bundle.BundleType.TRANSACTION);
        newBundle.setId(extractId);
        // ensure entry array
        newBundle.getEntry();
        resources.forEach(resource -> newBundle.addEntry(new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                .setResource((org.hl7.fhir.r5.model.Resource) resource)
                .setRequest(new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.POST))));

        return newBundle;
    }
}
