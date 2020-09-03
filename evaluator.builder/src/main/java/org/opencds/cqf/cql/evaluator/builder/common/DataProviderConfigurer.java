package org.opencds.cqf.cql.evaluator.builder.common;

import java.lang.reflect.Field;

import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.retrieve.SearchParamFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.evaluator.builder.DataProviderConfig;
import org.opencds.cqf.cql.evaluator.engine.data.ExtensibleDataProvider;

/**
 * This class is used to apply configuration to a DataProvider
 */
public class DataProviderConfigurer implements org.opencds.cqf.cql.evaluator.builder.DataProviderConfigurer {

    // TODO: Consider extending the engine class
    private static Field compositeProviderField;

    protected Field getCompositeProviderField() {
        if (compositeProviderField == null) {
            try {
                compositeProviderField = CompositeDataProvider.class.getField("retrieveProvider");
                compositeProviderField.setAccessible(true);
            } catch (Exception e) {

            }
        }

        return compositeProviderField;
    }

    @Override
    // TODO: Consider making an interface for a "Configurable" DataProvider
    // Or if justified pushing that up to the base class
    public void configure(DataProvider dataProvider, DataProviderConfig dataProviderConfig) {
        if (dataProvider instanceof ExtensibleDataProvider) {
            configure(((ExtensibleDataProvider)dataProvider), dataProviderConfig);
        }
        else if (dataProvider instanceof CompositeDataProvider) {
            configure(((CompositeDataProvider)dataProvider), dataProviderConfig);
        }
    }

    protected void configure(CompositeDataProvider dataProvider, DataProviderConfig dataProviderConfig) {
        try {
            RetrieveProvider retrieveProvider = (RetrieveProvider) this.getCompositeProviderField().get(dataProvider);
            configure(retrieveProvider, dataProviderConfig);
        } 
        catch (Exception e) {}
    }

    protected void configure(ExtensibleDataProvider dataProvider, DataProviderConfig dataProviderConfig) {
        for (RetrieveProvider retrieveProvider : dataProvider.getRetrieveProviders()) {
            configure(retrieveProvider, dataProviderConfig);
        }
    }

    protected void configure(RetrieveProvider retrieveProvider, DataProviderConfig dataProviderConfig) {
        if (retrieveProvider instanceof TerminologyAwareRetrieveProvider) {
            ((TerminologyAwareRetrieveProvider) retrieveProvider)
            .setTerminologyProvider(dataProviderConfig.getTerminologyProvider());
            ((TerminologyAwareRetrieveProvider) retrieveProvider)
                    .setExpandValueSets(dataProviderConfig.getExpandValueSets());
        }

        if (retrieveProvider instanceof SearchParamFhirRetrieveProvider) {
            ((SearchParamFhirRetrieveProvider) retrieveProvider)
                    .setMaxCodesPerQuery(dataProviderConfig.getMaxCodesPerQuery());
        }

        if (retrieveProvider instanceof RestFhirRetrieveProvider) {
            ((RestFhirRetrieveProvider) retrieveProvider).setSearchStyle(dataProviderConfig.getSearchStyle());
        }
    }
}