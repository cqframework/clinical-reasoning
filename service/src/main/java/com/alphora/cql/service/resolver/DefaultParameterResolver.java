package com.alphora.cql.service.resolver;

import java.util.Map;
import java.util.stream.Collectors;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;

public class DefaultParameterResolver implements ParameterResolver {
    public Map<String, Object>  resolvecontextParameters(Map<String, String> parameters) {
        if (parameters == null) {
            return null;
        }
        
        return parameters.entrySet().stream().collect(Collectors.toMap(x -> x.getKey(), y -> (Object)y.getValue()));
    }

    // The job of this function to to match up input parameter libraries.
    // In the future, it'll parse strings according the the type that's defined
    // on the library.
    // If a parameter has a "null" versioned identifier, it applied to every library.
    // If the various libraries have disparate types for the parameter, we should throw
    // An exception.
    public Map<VersionedIdentifier, Map<String, Object>> resolveParameters(Map<VersionedIdentifier, Library> libraries,
            Map<VersionedIdentifier, Map<String, Object>> parameters) {
        return parameters;
    }
}