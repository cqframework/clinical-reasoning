package com.alphora.cql.service.resolver;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;

public class DefaultParameterResolver implements ParameterResolver {
    public Map<String, Object>  resolvecontextParameters(Map<String, String> parameters) {
        return parameters.entrySet().stream().collect(Collectors.toMap(x -> x.getKey(), y -> (Object)y.getValue()));
    }
    
    public Map<VersionedIdentifier, Map<String, Object>> resolveParameters(Map<VersionedIdentifier, Library> libraries, Map<VersionedIdentifier, Map<String, String>> parameters) {
        // TODO: Model specific paramters
        if (!parameters.isEmpty()) {
            throw new NotImplementedException("Parameters are not yet implemented.");
        }

        return null;
    }

}