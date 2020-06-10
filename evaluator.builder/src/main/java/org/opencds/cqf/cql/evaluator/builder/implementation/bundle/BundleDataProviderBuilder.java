package org.opencds.cqf.cql.evaluator.builder.implementation.bundle;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.execution.provider.BundleRetrieveProvider;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;

import ca.uhn.fhir.context.FhirVersionEnum;

public class BundleDataProviderBuilder {
    private TerminologyProvider terminologyProvider;

    public BundleDataProviderBuilder(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

    //Should this compare to the Library Models and is there any chance of multiple bundles for different Model versions?
	public Map<String, DataProvider> build(IBaseBundle bundle) {
        Map<String, DataProvider> providers = new HashMap<>();
        providers.put(bundle.getStructureFhirVersionEnum().getFhirVersionString(), getFhirProvider(bundle));
        return providers;
    }

    @SuppressWarnings("rawtypes")
    private DataProvider getFhirProvider(IBaseBundle bundle) {
        FhirModelResolver modelResolver;
        RetrieveProvider retrieveProvider;
        FhirVersionEnum versionEnum = bundle.getStructureFhirVersionEnum();
        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no implementation for anything older than DSTU2 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            modelResolver = new Dstu2FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(bundle, modelResolver);        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            modelResolver = new Dstu3FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(bundle, modelResolver);        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            modelResolver = new R4FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(bundle, modelResolver);        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no implementation for anything newer than or equal to R5 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum");
        }
        return new CompositeDataProvider(modelResolver, retrieveProvider);
    }

    @SuppressWarnings("rawtypes")
    private RetrieveProvider buildRetrieveProvider(IBaseBundle bundle, FhirModelResolver modelResolver) {
        BundleRetrieveProvider bundleRetrieveProvider = new BundleRetrieveProvider(modelResolver, bundle, terminologyProvider);
        return bundleRetrieveProvider;
    }
    
}