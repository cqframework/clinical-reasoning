package org.opencds.cqf.cql.evaluator.fhir.adapter;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import ca.uhn.fhir.context.FhirVersionEnum;

public class AdapterModule extends AbstractModule {

    @Override
    protected void configure() {
        MapBinder<FhirVersionEnum, AdapterFactory> adapterBinder = MapBinder.newMapBinder(binder(),
                FhirVersionEnum.class, AdapterFactory.class);
        adapterBinder.addBinding(FhirVersionEnum.DSTU3)
                .to(org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory.class);
        adapterBinder.addBinding(FhirVersionEnum.R4)
                .to(org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory.class);
        adapterBinder.addBinding(FhirVersionEnum.R5)
                .to(org.opencds.cqf.cql.evaluator.fhir.adapter.r5.AdapterFactory.class);
    }

}
