package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4SubmitDataService {

    private static final Logger ourLogger = LoggerFactory.getLogger(R4SubmitDataService.class);

    private final Repository myRepository;

    public R4SubmitDataService(Repository theRepository) {
        this.myRepository = theRepository;
    }

    /**
     * Save measure report and resources to the local repository
     *
     * @param theId
     * @param theReport
     * @param theResources
     * @return Bundle transaction result
     */
    public Bundle submitData(IdType theId, MeasureReport theReport, List<IBaseResource> theResources) {
        /*
         * TODO - resource validation using $data-requirements operation (params are the provided id and
         * the measurement period from the MeasureReport)
         *
         * TODO - profile validation ... not sure how that would work ... (get StructureDefinition from
         * URL or must it be stored in Ruler?)
         */

        Bundle transactionBundle =
                new Bundle().setType(Bundle.BundleType.TRANSACTION).addEntry(createEntry(theReport));

        if (theResources != null) {
            for (IBaseResource res : theResources) {
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
        return myRepository.transaction(transactionBundle);
    }

    private Bundle.BundleEntryComponent createEntry(IBaseResource theResource) {
        return new Bundle.BundleEntryComponent()
                .setResource((Resource) theResource)
                .setRequest(createRequest(theResource));
    }

    private Bundle.BundleEntryRequestComponent createRequest(IBaseResource theResource) {
        Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
        if (theResource.getIdElement().hasValue()) {
            request.setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl(theResource.getIdElement().getValue());
        } else {
            request.setMethod(Bundle.HTTPVerb.POST).setUrl(theResource.fhirType());
        }

        return request;
    }
}
