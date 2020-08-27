package org.opencds.cqf.cql.evaluator.library.api;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseParameters;

public interface LibraryProcessor {

    public IBaseParameters evaluate(
        VersionedIdentifier libraryIdentifier,
        Pair<String, Object> contextParameter,
        IBaseParameters parameters,
        Set<String> expressions);
}