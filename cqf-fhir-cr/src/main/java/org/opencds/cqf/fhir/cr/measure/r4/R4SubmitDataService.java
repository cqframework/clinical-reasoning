package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;

public class R4SubmitDataService {

    private final Repository repository;

    public R4SubmitDataService(Repository repository) {
        this.repository = repository;
    }

    /**
     * Save measure report and resources to the local repository
     *
     * @param id
     * @param report
     * @param resources
     * @return Bundle transaction result
     */
    public Bundle submitData(IdType id, MeasureReport report, List<IBaseResource> resources) {
        /*
         * TODO - resource validation using $data-requirements operation (params are the provided id and
         * the measurement period from the MeasureReport)
         *
         * TODO - profile validation ... not sure how that would work ... (get StructureDefinition from
         * URL or must it be stored in Ruler?)
         */

        Bundle transactionBundle =
                new Bundle().setType(Bundle.BundleType.TRANSACTION).addEntry(createEntry(report));

        if (resources != null) {
            for (IBaseResource res : resources) {
                // Unpack nested Bundles
                if (res instanceof Bundle) {
                    Bundle nestedBundle = (Bundle) res;
                    for (Bundle.BundleEntryComponent entry : nestedBundle.getEntry()) {
                        transactionBundle.addEntry(createEntry(entry.getResource()));
                    }
                } else {
                    transactionBundle.addEntry(createEntry(res));
                }
            }
        }
        return repository.transaction(transactionBundle);
    }

    private Bundle.BundleEntryComponent createEntry(IBaseResource resource) {
        return new Bundle.BundleEntryComponent()
                .setResource((Resource) resource)
                .setRequest(createRequest(resource));
    }

    private Bundle.BundleEntryRequestComponent createRequest(IBaseResource resource) {
        Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
        if (resource.getIdElement().hasValue()) {
            request.setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl(resource.getIdElement().getValue());
        } else {
            request.setMethod(Bundle.HTTPVerb.POST).setUrl(resource.fhirType());
        }

        return request;
    }
}
