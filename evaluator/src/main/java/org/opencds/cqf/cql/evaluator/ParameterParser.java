package org.opencds.cqf.cql.evaluator;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

public interface ParameterParser {
    Pair<String, Object>  parseContextParameter(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier, Pair<String, String> contextParameter);
    Map<String, Object> parseParameters(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier, Map<String, String> parameters);

    Object parseContextParameter(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier, String context, String contextValue);
    Object parseParameter(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier, String parameterName, String parameterValue);

    // TODO: We probably want serialization of the same as well.
}