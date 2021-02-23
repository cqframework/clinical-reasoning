package org.opencds.cqf.cql.evaluator.dagger.fhir.adapter;

import javax.inject.Singleton;

import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

import dagger.Module;
import dagger.Provides;

@Module
public class AdapterModule {

  @Provides
  @Singleton
  protected AdapterFactory providesAdapterFactory(FhirContext fhirContext) {
    switch (fhirContext.getVersion().getVersion()) {
        case DSTU3:
                return new org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory();
        case R4:
                return new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();
        case R5:
                return new org.opencds.cqf.cql.evaluator.fhir.adapter.r5.AdapterFactory();
        case DSTU2:
        case DSTU2_1:
        case DSTU2_HL7ORG:
        default:
                throw new UnsupportedOperationException(
                                String.format("FHIR version %s is not supported.",
                                                fhirContext.getVersion().getVersion().toString()));
    }
  }
}
