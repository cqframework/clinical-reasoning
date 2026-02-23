package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.BundleHelper;

public class PublishProcessor implements IPublishProcessor {
    protected final IRepository repository;
    protected final FhirVersionEnum fhirVersion;

    public PublishProcessor(IRepository repository) {
        this.repository = repository;
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
    }

    @Override
    public IBaseBundle publishBundle(IBaseBundle bundle) {
        validateBundle(bundle);
        return repository.transaction(bundle);
    }

    /**
     * Validates that the bundle conforms to CRMIPublishableBundle profile:
     * - Bundle type must be "transaction"
     * - First entry must contain an ImplementationGuide resource
     */
    private void validateBundle(IBaseBundle bundle) {
        if (bundle == null) {
            throw new UnprocessableEntityException("Bundle is required");
        }

        // Validate bundle type is "transaction"
        validateBundleType(bundle);

        // Validate first entry is ImplementationGuide
        validateFirstEntry(bundle);
    }

    private void validateBundleType(IBaseBundle bundle) {
        String bundleType = null;

        switch (fhirVersion) {
            case DSTU3:
                var dstu3Bundle = (org.hl7.fhir.dstu3.model.Bundle) bundle;
                if (dstu3Bundle.hasType()) {
                    bundleType = dstu3Bundle.getType().toCode();
                }
                break;
            case R4:
                var r4Bundle = (org.hl7.fhir.r4.model.Bundle) bundle;
                if (r4Bundle.hasType()) {
                    bundleType = r4Bundle.getType().toCode();
                }
                break;
            case R5:
                var r5Bundle = (org.hl7.fhir.r5.model.Bundle) bundle;
                if (r5Bundle.hasType()) {
                    bundleType = r5Bundle.getType().toCode();
                }
                break;
            default:
                throw new UnprocessableEntityException(
                        "Unsupported FHIR version: " + fhirVersion.getFhirVersionString());
        }

        if (bundleType == null || !bundleType.equals("transaction")) {
            throw new UnprocessableEntityException(
                    "Bundle type must be 'transaction' per CRMIPublishableBundle profile, found: " + bundleType);
        }
    }

    private void validateFirstEntry(IBaseBundle bundle) {
        IBaseResource firstResource = BundleHelper.getEntryResourceFirstRep(bundle);

        if (firstResource == null) {
            throw new UnprocessableEntityException(
                    "Bundle must contain at least one entry per CRMIPublishableBundle profile");
        }

        if (!"ImplementationGuide".equals(firstResource.fhirType())) {
            throw new UnprocessableEntityException(
                    "First entry in Bundle must be an ImplementationGuide per CRMIPublishableBundle profile, found: "
                            + firstResource.fhirType());
        }
    }
}
