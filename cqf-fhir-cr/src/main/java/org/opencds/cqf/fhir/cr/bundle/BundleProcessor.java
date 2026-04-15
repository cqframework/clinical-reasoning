package org.opencds.cqf.fhir.cr.bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.opencds.cqf.fhir.cr.common.IValidateProcessor;
import org.opencds.cqf.fhir.cr.common.ValidateProcessor;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public class BundleProcessor {

    protected final FhirVersionEnum fhirVersion;
    protected IValidateProcessor validateProcessor;

    protected IRepository repository;

    public BundleProcessor(IRepository repository, IValidateProcessor validateProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        this.validateProcessor = validateProcessor;
    }

    public IBaseOperationOutcome validate(IBaseBundle resource, String mode, String profile) {
        var processor = validateProcessor != null ? validateProcessor : new ValidateProcessor(
            FhirContext.forVersion(fhirVersion));
        return processor.validate(resource, mode, profile);
    }

}
