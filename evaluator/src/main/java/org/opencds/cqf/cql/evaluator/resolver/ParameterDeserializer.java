package org.opencds.cqf.cql.evaluator.resolver;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public interface ParameterDeserializer {
    Pair<String, Object>  deserializeContextParameter(Pair<String, String> contextParameter);
    Map<String, Object> deserializeParameters(Map<String, String> parameters);

    Object deserializeContextParameter(String context, String contextValue);
    Object deserializeParameter(String parameterName, String parameterValue);

    // TODO: We probably want serialization of the same as well.
}