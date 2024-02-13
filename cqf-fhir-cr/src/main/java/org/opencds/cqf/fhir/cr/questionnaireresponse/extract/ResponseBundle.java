package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Ids;

public class ResponseBundle {
    public static IBaseBundle createBundleDstu3(String extractId, List<IBaseResource> resources) {
        var newBundle = new org.hl7.fhir.dstu3.model.Bundle();
        newBundle.setId(Ids.ensureIdType(extractId, "Bundle"));
        newBundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.TRANSACTION);
        // ensure entry array
        newBundle.getEntry();
        resources.forEach(resource -> {
            var entryRequest = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent();
            entryRequest.setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.PUT);
            entryRequest.setUrl(
                    resource.fhirType() + "/" + resource.getIdElement().getIdPart());

            var entry = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
            entry.setResource((org.hl7.fhir.dstu3.model.Resource) resource);
            entry.setRequest(entryRequest);
            newBundle.addEntry(entry);
        });

        return newBundle;
    }

    public static IBaseBundle createBundleR4(String extractId, List<IBaseResource> resources) {
        var newBundle = new org.hl7.fhir.r4.model.Bundle();
        newBundle.setId(Ids.ensureIdType(extractId, "Bundle"));
        newBundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION);
        // ensure entry array
        newBundle.getEntry();
        resources.forEach(resource -> {
            var entryRequest = new org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent();
            entryRequest.setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.PUT);
            entryRequest.setUrl(
                    resource.fhirType() + "/" + resource.getIdElement().getIdPart());

            var entry = new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
            entry.setResource((org.hl7.fhir.r4.model.Resource) resource);
            entry.setRequest(entryRequest);
            newBundle.addEntry(entry);
        });

        return newBundle;
    }

    public static IBaseBundle createBundleR5(String extractId, List<IBaseResource> resources) {
        var newBundle = new org.hl7.fhir.r5.model.Bundle();
        newBundle.setId(Ids.ensureIdType(extractId, "Bundle"));
        newBundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.TRANSACTION);
        // ensure entry array
        newBundle.getEntry();
        resources.forEach(resource -> {
            var entryRequest = new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent();
            entryRequest.setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.PUT);
            entryRequest.setUrl(
                    resource.fhirType() + "/" + resource.getIdElement().getIdPart());

            var entry = new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent();
            entry.setResource((org.hl7.fhir.r5.model.Resource) resource);
            entry.setRequest(entryRequest);
            newBundle.addEntry(entry);
        });

        return newBundle;
    }
}
