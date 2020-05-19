package org.opencds.cqf.cql.evaluator.resolver.implementation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.evaluator.resolver.ParameterDeserializer;


/**
 * The default implementation of the ParameterDeserializer is currently just a pass through of the strings.
 * The intention is for it to eventually support CQL-defined types, and provide an interface for supporting
 * other types, such as FHIR resources. NOTE: in order to support picking the correct serializer this class
 * needs the context of which Library the parameters are defined on so that it can pick the correct type.
 */
public class DefaultParameterDeserializer implements ParameterDeserializer {
    @Override
    public Pair<String, Object> deserializeContextParameter(Pair<String, String> contextParameter) {
        Objects.requireNonNull(contextParameter);

        return Pair.of(contextParameter.getLeft(), 
            this.deserializeContextParameter(contextParameter.getLeft(), contextParameter.getRight()));
    }

    @Override
    public Map<String, Object> deserializeParameters(Map<String, String> parameters) {
        Objects.requireNonNull(parameters);

        Map<String, Object> deseralized = new HashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            deseralized.put(entry.getKey(), this.deserializeParameter(entry.getKey(), entry.getValue()));
        }
        return deseralized;
    }

    @Override
    public Object deserializeContextParameter(String context, String contextValue) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(contextValue);

        return contextValue;
    }

    @Override
    public Object deserializeParameter(String parameterName, String parameterValue) {
        Objects.requireNonNull(parameterName);
        Objects.requireNonNull(parameterValue);

        return parameterValue;
    }
}