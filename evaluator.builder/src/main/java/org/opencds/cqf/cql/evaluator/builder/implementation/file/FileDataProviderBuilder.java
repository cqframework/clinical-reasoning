package org.opencds.cqf.cql.evaluator.builder.implementation.file;

import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.execution.provider.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.execution.provider.NoOpRetrieveProvider;
import org.opencds.cqf.cql.evaluator.execution.util.DirectoryBundler;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;

import ca.uhn.fhir.context.FhirVersionEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

public class FileDataProviderBuilder {
    private TerminologyProvider terminologyProvider;

    public FileDataProviderBuilder(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

    public Map<String, DataProvider> build(Map<String, Pair<String, String>> models, Map<String, String> modelUrlMap) {
        Map<String, DataProvider> providers = new HashMap<>();
        for (Entry<String, String> entry : modelUrlMap.entrySet()) {
            String version = models.get(entry.getKey()).getLeft();
            providers.put(entry.getKey(),
                    getProvider(entry.getKey(), version, entry.getValue()));
        }
        return providers;
    }

    private DataProvider getProvider(String model, String version, String uri) {
        switch (model) {
            case "http://hl7.org/fhir":
                return getFhirProvider(version, uri);

            case "urn:healthit-gov:qdm:v5_4":
                return getQdmProvider(version, uri);

            default:
                throw new IllegalArgumentException(String.format("Unknown data provider uri: %s", model));
        }
    }

    @SuppressWarnings("rawtypes")
    private DataProvider getFhirProvider(String version, String uri) {
        FhirModelResolver modelResolver;
        RetrieveProvider retrieveProvider;
        FhirVersionEnum versionEnum = ModelVersionHelper.forVersionString(version);
        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no implementation for anything older than DSTU2 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            modelResolver = new Dstu2FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(uri, modelResolver);        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            modelResolver = new Dstu3FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(uri, modelResolver);        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            modelResolver = new R4FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(uri, modelResolver);        
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
    private RetrieveProvider buildRetrieveProvider(String uri, FhirModelResolver modelResolver) {
        if (uri == null) {
            return new NoOpRetrieveProvider();
        } else {
            return new BundleRetrieveProvider(modelResolver, new DirectoryBundler(modelResolver.getFhirContext()).bundle(uri), terminologyProvider);
        }
    }

    private static DataProvider getQdmProvider(String version, String uri) {
        throw new NotImplementedException("QDM data providers are not yet implemented");
    }
}