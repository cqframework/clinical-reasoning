package org.opencds.cqf.cql.service.factory;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

public interface TerminologyProviderFactory {
    TerminologyProvider create(Map<String, Pair<String, String>> modelVersionsAndUrls, String terminologyUri);
}