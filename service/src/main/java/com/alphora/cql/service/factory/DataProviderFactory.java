package com.alphora.cql.service.factory;

import java.util.Map;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

public interface DataProviderFactory {
    Map<String, DataProvider> create(Map<VersionedIdentifier, Library> libraries, Map<String,String> modelUris, TerminologyProvider terminologyProvider);
}