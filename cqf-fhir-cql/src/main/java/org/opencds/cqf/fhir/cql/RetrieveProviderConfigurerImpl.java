package org.opencds.cqf.fhir.cql;

import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.retrieve.SearchParamFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

/**
 * This class is used to apply configuration to a RetrieveProvider
 */
public class RetrieveProviderConfigurerImpl
    implements RetrieveProviderConfigurer {

  RetrieveProviderConfig retrieveProviderConfig;

  public RetrieveProviderConfigurerImpl(RetrieveProviderConfig dataProviderConfig) {
    this.retrieveProviderConfig = dataProviderConfig;
  }

  // TODO: Consider making an interface for a "Configurable" DataProvider
  // Maybe it "accepts" a DataProvider config
  // Or if justified pushing that up to the base class
  @Override
  public void configure(RetrieveProvider retrieveProvider,
      TerminologyProvider terminologyProvider) {
    if (retrieveProvider instanceof TerminologyAwareRetrieveProvider) {
      ((TerminologyAwareRetrieveProvider) retrieveProvider)
          .setTerminologyProvider(terminologyProvider);
      ((TerminologyAwareRetrieveProvider) retrieveProvider)
          .setExpandValueSets(retrieveProviderConfig.getExpandValueSets());
    }

    if (retrieveProvider instanceof SearchParamFhirRetrieveProvider) {
      ((SearchParamFhirRetrieveProvider) retrieveProvider)
          .setMaxCodesPerQuery(retrieveProviderConfig.getMaxCodesPerQuery());
    }

    if (retrieveProvider instanceof RestFhirRetrieveProvider) {
      ((RestFhirRetrieveProvider) retrieveProvider)
          .setSearchStyle(retrieveProviderConfig.getSearchStyle());
    }
  }
}
