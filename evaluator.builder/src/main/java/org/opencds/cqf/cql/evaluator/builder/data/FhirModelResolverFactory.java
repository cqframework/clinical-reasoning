package org.opencds.cqf.cql.evaluator.builder.data;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.cql.evaluator.fhir.util.VersionUtilities;

import ca.uhn.fhir.context.FhirVersionEnum;

@Named
public class FhirModelResolverFactory implements org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory {

    @Inject
    public FhirModelResolverFactory() {}

    private static Map<FhirVersionEnum, ModelResolver> cache = new HashMap<>();

    @Override
    public ModelResolver create(String version) {
       requireNonNull(version, "version can not be null");

       FhirVersionEnum fhirVersionEnum = VersionUtilities.enumForVersion(version);
       return this.fhirModelResolverForVersion(fhirVersionEnum);
    }

    protected ModelResolver fhirModelResolverForVersion(FhirVersionEnum fhirVersionEnum) {
        requireNonNull(fhirVersionEnum, "fhirVersionEnum can not be null");
        if (!cache.containsKey(fhirVersionEnum)) {
            ModelResolver resolver = null;
            switch (fhirVersionEnum) {
                case DSTU2:
                    resolver = new CachingModelResolverDecorator(new Dstu2FhirModelResolver());
                    break;
                case DSTU3:
                    resolver = new CachingModelResolverDecorator(new Dstu3FhirModelResolver());
                    break;
                case R4:
                    resolver = new CachingModelResolverDecorator(new R4FhirModelResolver());
                    break;
                default:
                    throw new IllegalArgumentException("unknown or unsupported FHIR version");
            }

            cache.put(fhirVersionEnum, resolver);
        }

        return cache.get(fhirVersionEnum);
    }

    @Override
    public String getModelUri() {
        return Constants.FHIR_MODEL_URI;
    }    
}