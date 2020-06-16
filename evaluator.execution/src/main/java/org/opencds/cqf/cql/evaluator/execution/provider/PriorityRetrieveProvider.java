
package org.opencds.cqf.cql.evaluator.execution.provider;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class PriorityRetrieveProvider implements RetrieveProvider {

    private List<RetrieveProvider> retrieveProviders;
;
    public PriorityRetrieveProvider(List<RetrieveProvider> retrieveProviders) {
        Objects.requireNonNull(retrieveProviders, "retrieveProviders can not be null.");

        this.retrieveProviders = retrieveProviders;
    }

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
            String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
            String dateLowPath, String dateHighPath, Interval dateRange) {

        for (RetrieveProvider rp : retrieveProviders){
            Iterable<Object> result = rp.retrieve(context, contextPath, contextValue, dataType, templateId, codePath, codes, 
                valueSet, datePath, dateLowPath, dateHighPath, dateRange);

            if (result == null) {
                throw new IllegalStateException("retrieveProvider unexpectedly returned null. Should be an empty set.");
            }

            List<Object> asList = Lists.newArrayList(result);
            if (asList.size() > 0) {
                return asList;
            }
        }

        return Collections.emptySet();
    }
}