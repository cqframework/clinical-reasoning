package org.opencds.cqf.cql.service.factory;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

public interface DataProviderFactory {
    Map<String, DataProvider> create(Map<String, Pair<String, String>> modelVersionsAndUrls, TerminologyProvider terminologyProvider);
}