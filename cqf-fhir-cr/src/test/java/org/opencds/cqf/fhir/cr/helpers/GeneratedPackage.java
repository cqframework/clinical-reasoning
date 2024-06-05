package org.opencds.cqf.fhir.cr.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntry;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseBundle;

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
}
