package org.opencds.cqf.cql.evaluator.engine.retrieve;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class PriorityRetrieveProvider implements RetrieveProvider {

    private List<RetrieveProvider> retrieveProviders;
;
    public PriorityRetrieveProvider(List<RetrieveProvider> retrieveProviders) {
        requireNonNull(retrieveProviders, "retrieveProviders can not be null.");

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
                // TODO: Change the semantics such that null means unknown while empty set means known empty
                throw new IllegalStateException("retrieveProvider unexpectedly returned null. Should be an empty set.");
            }

            if (result.iterator().hasNext()) {
                return result;
            }
        }

        return Collections.emptySet();
    }
}