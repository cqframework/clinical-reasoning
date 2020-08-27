package org.opencds.cqf.cql.evaluator.api;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;

public interface ParameterDeserializer {
    Pair<String, Object>  deserializeContextParameter(VersionedIdentifier libraryIdentifier, Pair<String, String> contextParameter);
    Map<String, Object> deserializeParameters(VersionedIdentifier libraryIdentifier, Map<String, String> parameters);

    Object deserializeContextParameter(VersionedIdentifier libraryIdentifier, String context, String contextValue);
    Object deserializeParameter(VersionedIdentifier libraryIdentifier, String parameterName, String parameterValue);

    // TODO: We probably want serialization of the same as well.
}