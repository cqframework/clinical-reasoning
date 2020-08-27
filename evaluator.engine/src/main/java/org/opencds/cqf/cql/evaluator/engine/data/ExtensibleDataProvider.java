package org.opencds.cqf.cql.evaluator.engine.data;

import java.util.ArrayList;
import java.util.List;

import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;

import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;

public class ExtensibleDataProvider extends CompositeDataProvider {


    protected ModelResolver modelResolver;
    protected List<RetrieveProvider> additionalRetrieveProviders = new ArrayList<>();

    protected PriorityRetrieveProvider priorityRetrieveProvider;

    public ExtensibleDataProvider(ModelResolver modelResolver, RetrieveProvider retrieveProvider) {
        super(modelResolver, retrieveProvider);
        this.updatePriorityRetrieveProvider();
    }

    public void reset() {
        this.additionalRetrieveProviders.clear();
        this.updatePriorityRetrieveProvider();
    }

    public void registerRetrieveProvider(RetrieveProvider retrieveProvider) {
        if (!additionalRetrieveProviders.contains(retrieveProvider)) {
            additionalRetrieveProviders.add(retrieveProvider);
            this.updatePriorityRetrieveProvider();
        }
    }

    public List<RetrieveProvider> getRetrieveProviders() {
        List<RetrieveProvider> temp = new ArrayList<>();
        temp.add(this.retrieveProvider);
        temp.addAll(this.additionalRetrieveProviders);
        return temp;
    }

    protected void updatePriorityRetrieveProvider() {
        this.priorityRetrieveProvider = new PriorityRetrieveProvider(this.getRetrieveProviders());
    }

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
            String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
            String dateLowPath, String dateHighPath, Interval dateRange) {
        
        return this.priorityRetrieveProvider.retrieve(context, contextPath, contextValue, dataType, templateId, codePath, codes, valueSet, datePath, dateLowPath, dateHighPath, dateRange);
    }
}