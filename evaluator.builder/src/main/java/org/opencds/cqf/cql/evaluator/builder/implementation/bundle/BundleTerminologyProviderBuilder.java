package org.opencds.cqf.cql.evaluator.builder.implementation.bundle;

import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

import org.apache.commons.lang3.tuple.Pair;

public class BundleTerminologyProviderBuilder {
	private FhirContext fhirContext;

	public TerminologyProvider build(Map<String, Pair<String, String>> models, IBaseBundle bundle) {
		Pair<String, String> modelVersionPair = models.get("http://hl7.org/fhir");
        if (modelVersionPair == null) {
            // Assume FHIR 3.0.0 Not sure if this is desired in production
            modelVersionPair = Pair.of("http://hl7.org/fhir", "3.0.0");
		}
		//Could also use compare to bundle versionEnum? to validate bundle is the same as the library model
		setFhirContext(modelVersionPair.getLeft(), modelVersionPair.getRight());

        return new BundleTerminologyProvider(fhirContext, bundle);
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
            throw new NotImplementedException("Sorry there is no bundle implementation for anything older than DSTU2 as of now.");
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
            throw new NotImplementedException("Sorry there is no bundle implementation for anything newer or equal to R4 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum");
        }
    }  
}