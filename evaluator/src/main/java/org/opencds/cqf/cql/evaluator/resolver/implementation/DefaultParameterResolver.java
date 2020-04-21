package org.opencds.cqf.cql.evaluator.resolver.implementation;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.evaluator.resolver.ParameterResolver;
import org.opencds.cqf.cql.execution.LibraryLoader;

public class DefaultParameterResolver implements ParameterResolver {

    LibraryLoader libraryLoader;

    public DefaultParameterResolver(LibraryLoader libraryLoader/*, TODO: ParserLoader / Provder... */) {
        if (libraryLoader == null) {
            throw new IllegalArgumentException("libraryLoader can not be null.");
        }
    }

    @Override
    public Pair<String, Object>  resolveContextParameters(VersionedIdentifier libraryIdentifier, Pair<String, String> contextParameter) {
        if (contextParameter == null) {
            return null;
        }

        // TODO:
        // 1. Load all libraries recursively based on Primary library
        // 2. Fpr each Parameter name, validate that all libraries are using the same type
        // 3. For that parameter type, load the appropriate parser
        // 4. Parse the value.
        
        return Pair.of(contextParameter.getLeft(), contextParameter.getRight());
    }

    @Override
    public Map<String, Object> resolveParameters(VersionedIdentifier libraryIdentifier, Map<String, String> parameters) {
        if (parameters == null) {
            return null;
        }

        // TODO:
        // 1. Load all libraries recursively based on Primary library
        // 2. Fpr each Parameter name, validate that all libraries are using the same type
        // 3. For that parameter type, load the appropriate parser
        // 4. Parse the value.

        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            resolved.put(entry.getKey(), entry.getValue());
        }
        return resolved;
    }
}