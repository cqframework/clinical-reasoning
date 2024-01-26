package org.opencds.cqf.fhir.cql.engine.retrieve;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class FederatedDataProvider extends CompositeDataProvider {
    protected final List<RetrieveProvider> retrieveProviders;

    public FederatedDataProvider(ModelResolver modelResolver, List<RetrieveProvider> retrieveProviders) {
        super(modelResolver, null);
        this.retrieveProviders = retrieveProviders;
    }

    @Override
    public Iterable<Object> retrieve(
            String context,
            String contextPath,
            Object contextValue,
            String dataType,
            String templateId,
            String codePath,
            Iterable<Code> codes,
            String valueSet,
            String datePath,
            String dateLowPath,
            String dateHighPath,
            Interval dateRange) {
        List<Iterable<Object>> results = new ArrayList<>();
        for (var provider : this.retrieveProviders) {
            results.add(provider.retrieve(
                    context,
                    contextPath,
                    contextValue,
                    dataType,
                    templateId,
                    codePath,
                    codes,
                    valueSet,
                    datePath,
                    dateLowPath,
                    dateHighPath,
                    dateRange));
        }

        return Iterables.concat(results);
    }
}
