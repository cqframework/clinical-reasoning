package org.opencds.cqf.cql.evaluator.guice.cql2elm;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;


public class Cql2ElmModule extends AbstractModule {
    @Provides
    @Singleton
    protected LibraryVersionSelector providesAdapterFactory(AdapterFactory adapterFactory) {
        return new LibraryVersionSelector(adapterFactory);
    }
}
