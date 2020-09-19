package org.opencds.cqf.cql.evaluator.engine.model;

import java.util.HashMap;
import java.util.Map;

import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;

public class ModelResolverCache {
    private static Map<String, ModelResolver> modelResolverByModelAndVersion = new HashMap<>();

    public static ModelResolver getResolver(String model, String version) {
        if (model == null || version == null) {
            throw new IllegalArgumentException("model and  version must not be null");
        }

        if (!modelResolverByModelAndVersion.containsKey(model + "-" + version)) {

            if (!model.equals("FHIR")) {
                throw new IllegalArgumentException("Only FHIR models are supported at this time.");
            }
            ModelResolver modelResolver;
            switch (version) {
                case "2.0.0":
                    modelResolver = new CachingModelResolverDecorator(new Dstu2FhirModelResolver());

                    break;
                case "3.0.0":
                    modelResolver = new CachingModelResolverDecorator(new Dstu3FhirModelResolver());
                    break;
                case "4.0.0":
                    modelResolver = new CachingModelResolverDecorator(new R4FhirModelResolver());
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unknown FHIR data provider version: %s", version));
            }
            
            modelResolverByModelAndVersion.put(model + "-" + version, modelResolver);
        }

        return modelResolverByModelAndVersion.get(model + "-" + version);
    }

}