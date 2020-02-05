package com.alphora.cql.service.provider;

import org.opencds.cqf.cql.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;

public class NoOpRetrieveProvider implements RetrieveProvider {

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
            String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
            String dateLowPath, String dateHighPath, Interval dateRange) {
        return null;
    }
}