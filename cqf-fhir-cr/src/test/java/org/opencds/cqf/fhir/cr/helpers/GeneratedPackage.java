package org.opencds.cqf.fhir.cr.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResourceFirstRep;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class GeneratedPackage {
    final IBaseBundle generatedBundle;
    final IParser jsonParser;

    public GeneratedPackage(IBaseBundle generatedBundle, FhirContext fhirContext) {
        this.generatedBundle = generatedBundle;
        jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
    }

    public GeneratedPackage hasEntry(int count) {
        assertEquals(count, getEntry(generatedBundle).size());
        return this;
    }

    public <R extends IBaseResource> GeneratedPackage firstEntryIsType(Class<R> resourceType) {
        assertEquals(resourceType, getEntryResourceFirstRep(generatedBundle).getClass());
        return this;
    }

    public IBaseBundle getBundle() {
        return generatedBundle;
    }
}
