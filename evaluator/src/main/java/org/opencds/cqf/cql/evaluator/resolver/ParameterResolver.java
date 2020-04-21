package org.opencds.cqf.cql.evaluator.resolver;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;

public interface ParameterResolver {
    Pair<String, Object>  resolveContextParameters(VersionedIdentifier libraryIdentifier, Pair<String, String> contextParameters);
    Map<String, Object> resolveParameters(VersionedIdentifier libraryIdentifier, Map<String, String> parameters);
}