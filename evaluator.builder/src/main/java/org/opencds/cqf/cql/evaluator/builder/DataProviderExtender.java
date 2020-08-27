package org.opencds.cqf.cql.evaluator.builder;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.data.ExtensibleDataProvider;

/**
 * This class is used to extend a DataProvider with additional data sources
 */
public class DataProviderExtender implements org.opencds.cqf.cql.evaluator.builder.api.DataProviderExtender {

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

    // TODO: Consider extending based on the underlying RetrieveProviders first
    // Or by making the base DataProvider extensible
    @Override
    /**
     * Extends the target DataProvider with the data sources present in the
     * otherProviders collection The extended DataProvider evaluates the data
     * sources in the order they are present in the collection
     */
    public void extend(DataProvider target, Collection<DataProvider> otherProviders) {
        Objects.requireNonNull(target, "target can not be null");
        Objects.requireNonNull(otherProviders, "otherProviders can not be null");
        if (!(target instanceof ExtensibleDataProvider)) {
            throw new IllegalArgumentException("target must be an ExtensibleDataProvider");
        }

        ExtensibleDataProvider extensible = (ExtensibleDataProvider) target;

        for (DataProvider other : otherProviders) {
            if (!extensible.getPackageName().equals(other.getPackageName())) {
                throw new IllegalArgumentException(String.format(
                        "can not extend target DataProvider of packageName: %s with other DataProvider of packageName: %s",
                        target.getPackageName(), other.getPackageName()));
            }

            List<RetrieveProvider> providers = getProviders(other);
            providers.forEach(x -> extensible.registerRetrieveProvider(x));
        }
    }

    protected List<RetrieveProvider> getProviders(DataProvider dataProvider) {
        if (dataProvider instanceof CompositeDataProvider) {
            try {
                RetrieveProvider retrieveProvider = (RetrieveProvider) this.getCompositeProviderField()
                        .get(dataProvider);
                return Collections.singletonList(retrieveProvider);
            } catch (Exception e) {
                return Collections.emptyList();
            }
        } else if (dataProvider instanceof ExtensibleDataProvider) {
            return ((ExtensibleDataProvider) dataProvider).getRetrieveProviders();
        }

        return Collections.emptyList();
    }
}