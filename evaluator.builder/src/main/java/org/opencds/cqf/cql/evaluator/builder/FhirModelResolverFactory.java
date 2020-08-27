package org.opencds.cqf.cql.evaluator.builder;

import java.util.Objects;

import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.api.Constants;
import org.opencds.cqf.cql.evaluator.fhir.VersionUtilities;

import ca.uhn.fhir.context.FhirVersionEnum;

public class FhirModelResolverFactory implements org.opencds.cqf.cql.evaluator.builder.api.ModelResolverFactory {

    @Override
    public ModelResolver create(String version) {
       Objects.requireNonNull(version, "version can not be null");

       FhirVersionEnum fhirVersionEnum = VersionUtilities.enumForVersion(version);
       return this.fhirModelResolverForVersion(fhirVersionEnum);
    }

    protected ModelResolver fhirModelResolverForVersion(FhirVersionEnum fhirVersionEnum) {
        Objects.requireNonNull(fhirVersionEnum, "fhirVersionEnum can not be null");
        switch (fhirVersionEnum) {
            case DSTU2:
                return new Dstu2FhirModelResolver();
            case DSTU3:
                return new Dstu3FhirModelResolver();
            case R4:
                return new R4FhirModelResolver();
            default:
                throw new IllegalArgumentException("unknown or unsupported FHIR version");
        }
    }

    @Override
    public String getModelUri() {
        return Constants.FHIR_MODEL_URI;
    }    
}