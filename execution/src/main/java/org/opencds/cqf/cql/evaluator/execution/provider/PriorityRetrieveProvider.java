
package org.opencds.cqf.cql.evaluator.provider;

import org.opencds.cqf.cql.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;

public class PriorityRetrieveProvider implements RetrieveProvider {

    private List<RetrieveProvider> retrieveProviders;

    public PriorityRetrieveProvider(List<RetrieveProvider> retrieveProviders) {
        this.retrieveProviders = retrieveProviders;
    }

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
            String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
            String dateLowPath, String dateHighPath, Interval dateRange) {

        for (RetrieveProvider rp : retrieveProviders){
            var result = rp.retrieve(context, contextPath, contextValue, dataType, templateId, codePath, codes, 
                valueSet, datePath, dateLowPath, dateHighPath, dateRange);

            if (result != null) {
                return result;
            }
        }

        return null;
    }
}