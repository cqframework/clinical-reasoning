package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

public class AdditionalDatas {
    public static Repository addAdditionalData(Repository repository, IBaseBundle additionalData) {
        if (additionalData == null || !hasEntry(repository.fhirContext(), additionalData)) {
            return repository;
        }

        var bundleRepo = new InMemoryFhirRepository(repository.fhirContext(), additionalData);
        return new FederatedRepository(repository, bundleRepo);
    }

    private static boolean hasEntry(FhirContext fhirContext, IBaseBundle bundle) {
        var count = BundleUtil.getTotal(fhirContext, bundle);
        if (count == null || count == 0) {
            return false;
        }

        return true;
    }
}
