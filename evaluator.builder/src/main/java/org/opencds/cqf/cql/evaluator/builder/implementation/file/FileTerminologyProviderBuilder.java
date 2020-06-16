package org.opencds.cqf.cql.evaluator.builder.implementation.file;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.execution.util.DirectoryBundler;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;

public class FileTerminologyProviderBuilder {
	private FhirContext fhirContext;

	public TerminologyProvider build(Map<String, Pair<String, String>> models, String terminologyUri) {
		if (terminologyUri == null || terminologyUri.isEmpty()) {
            // throw return null;
		}
		Pair<String, String> modelVersionPair = Pair.of("http://hl7.org/fhir", models.get("http://hl7.org/fhir").getLeft());
        if (modelVersionPair == null) {
            // Assume FHIR 3.0.0 Not sure if this is desired in production
            modelVersionPair = Pair.of("http://hl7.org/fhir", "3.0.0");
        }
		setFhirContext(modelVersionPair.getLeft(), modelVersionPair.getRight());

        return new BundleTerminologyProvider(fhirContext, new DirectoryBundler(fhirContext).bundle(terminologyUri));
	}

	private void setFhirContext(String model, String version) {
        if(model.equals("http://hl7.org/fhir") || model == null) {
            setFhirContext(version);
        }
        else {
            throw new IllegalArgumentException(String.format("We currently only support FHIR-based terminology, Unknown Model: %s", model));
        }
    }

    private void setFhirContext(String version) {
        FhirVersionEnum versionEnum = ModelVersionHelper.forVersionString(version);

        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no File Terminology Provider implementation for anything older than DSTU2 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            this.fhirContext = FhirContext.forDstu2();
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            this.fhirContext = FhirContext.forDstu3();
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            this.fhirContext = FhirContext.forR4();
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no File Terminology Provider implementation for anything newer or equal to R4 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum");
        }
    }
}