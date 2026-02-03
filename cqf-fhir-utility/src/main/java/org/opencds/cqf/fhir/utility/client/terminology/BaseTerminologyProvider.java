package org.opencds.cqf.fhir.utility.client.terminology;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Resources;

public abstract class BaseTerminologyProvider implements ITerminologyProvider {
    protected final FhirContext fhirContext;

    public BaseTerminologyProvider(FhirContext fhirContext) {
        this.fhirContext = requireNonNull(fhirContext);
    }

    protected String getAddressBase(String address) {
        return ITerminologyServerClient.getAddressBase(address, this.fhirContext);
    }

    @Override
    public Class<? extends IBaseResource> getCodeSystemClass() {
        return Resources.getClassForTypeAndVersion(
                "CodeSystem", fhirContext.getVersion().getVersion());
    }

    @Override
    public Class<? extends IBaseResource> getValueSetClass() {
        return Resources.getClassForTypeAndVersion(
                "ValueSet", fhirContext.getVersion().getVersion());
    }
}
