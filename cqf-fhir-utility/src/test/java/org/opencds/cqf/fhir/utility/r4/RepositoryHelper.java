package org.opencds.cqf.fhir.utility.r4;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.rest.api.MethodOutcome;

public class RepositoryHelper {
    public static final Bundle transactionStub(Bundle transaction, Repository repository) {
        Bundle returnBundle = new Bundle();
        transaction.getEntry().stream().forEach((e) -> {
            HTTPVerb v = e.getRequest().getMethod();
            BundleEntryComponent entry = new BundleEntryComponent();
            BundleEntryResponseComponent resp = new BundleEntryResponseComponent();
            entry.setResponse(resp);
            if (v == HTTPVerb.PUT) {
                MethodOutcome outcome = repository.update(e.getResource());
                String location = outcome.getId().getValue();
                resp.setLocation(location);
                returnBundle.addEntry(entry);
            } else if (v == HTTPVerb.POST) {
                MethodOutcome outcome = repository.create(e.getResource());
                String location = outcome.getId().getValue();
                resp.setLocation(location);
                returnBundle.addEntry(entry);
            }
        });
        return returnBundle;
    }
}
