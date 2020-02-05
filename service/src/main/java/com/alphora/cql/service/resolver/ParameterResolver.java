package com.alphora.cql.service.resolver;

import java.util.Map;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;

public interface ParameterResolver {
    Map<String, Object>  resolvecontextParameters(Map<String, String> parameters);
    Map<VersionedIdentifier, Map<String, Object>> resolveParameters(Map<VersionedIdentifier, Library> libraries, Map<VersionedIdentifier, Map<String, Object>> parameters);
}