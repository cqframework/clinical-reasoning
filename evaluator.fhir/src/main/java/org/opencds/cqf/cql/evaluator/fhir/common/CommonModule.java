package org.opencds.cqf.cql.evaluator.fhir.common;

import com.google.inject.AbstractModule;

public class CommonModule extends AbstractModule {
    @Override
    protected void configure() {
        this.bind(org.opencds.cqf.cql.evaluator.fhir.ClientFactory.class).to(ClientFactory.class);
        this.bind(org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler.class).to(DirectoryBundler.class);
    }
}
