package com.alphora.cql.service.factory;

import java.util.HashMap;
import java.util.Map;

import com.alphora.cql.service.Helpers;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.UsingDef;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.data.CompositeDataProvider;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.data.ModelResolver;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.type.*;

import ca.uhn.fhir.context.FhirContext;

// TODO: Dyanamic data provider registration
public class DefaultDataProviderFactory implements DataProviderFactory {

    @SuppressWarnings("serial")
    private static final Map<String, String> shorthandMap = new HashMap<String, String>() {
        {
            put("FHIR", "http://hl7.org/fhir");
            put("QUICK", "http://hl7.org/fhir");
            put("QDM", "urn:healthit-gov:qdm:v5_4");
        }
    };

    @Override
    public Map<String, DataProvider> create(Map<VersionedIdentifier, Library> libraries, Map<String, String> modelUris,
            TerminologyProvider terminologyProvider) {
        Map<String, Pair<String, String>> versions = this.getVersions(libraries, modelUris);
        return this.getProviders(versions, terminologyProvider);
    }

    private Map<String, Pair<String, String>> getVersions(Map<VersionedIdentifier, Library> libraries,
            Map<String, String> modelUris) {
        Map<String, Pair<String, String>> versions = new HashMap<>();
        for (Map.Entry<String, String> modelUri : modelUris.entrySet()) {
            String uri = shorthandMap.containsKey(modelUri.getKey()) ? shorthandMap.get(modelUri.getKey())
                    : modelUri.getKey();

            String version = null;
            for (Library library : libraries.values()) {
                if (version != null) {
                    break;
                }

                if (library.getUsings() != null && library.getUsings().getDef() != null) {
                    for (UsingDef u : library.getUsings().getDef()) {
                        if (u.getUri().equals(uri)) {
                            version = u.getVersion();
                            break;
                        }
                    }
                }
            }

            if (version == null) {
                throw new IllegalArgumentException(
                        String.format("A uri was specified for %s but is not used.", modelUri.getKey()));
            }

            if (versions.containsKey(uri)) {
                if (!versions.get(uri).getKey().equals(version)) {
                    throw new IllegalArgumentException(String.format(
                            "Libraries are using multiple versions of %s. Only one version is supported at a time.",
                            modelUri.getKey()));
                }

            } else {
                versions.put(uri, Pair.of(version, modelUri.getValue()));
            }

        }

        return versions;
    }

    private Map<String, DataProvider> getProviders(Map<String, Pair<String, String>> versions,
            TerminologyProvider terminologyProvider) {
        Map<String, DataProvider> providers = new HashMap<>();
        for (Map.Entry<String, Pair<String, String>> m : versions.entrySet()) {
            providers.put(m.getKey(),
                    this.getProvider(m.getKey(), m.getValue().getLeft(), m.getValue().getRight(), terminologyProvider));
        }

        return providers;
    }

    private DataProvider getProvider(String model, String version, String uri,
            TerminologyProvider terminologyProvider) {
        switch (model) {
        case "http://hl7.org/fhir":
            return this.getFhirProvider(version, uri, terminologyProvider);

        case "urn:healthit-gov:qdm:v5_4":
            return this.getQdmProvider(version, uri, terminologyProvider);

        default:
            throw new IllegalArgumentException(String.format("Unknown data provider uri: %s", model));
        }
    }

    private DataProvider getFhirProvider(String version, String uri, TerminologyProvider terminologyProvider) {
        switch (version) {
        case "3.0.0":
            if (Helpers.isFileUri(uri)) {
                throw new NotImplementedException("TODO");
            } else {
                FhirContext context = FhirContext.forDstu3();
                ModelResolver model = new Dstu3FhirModelResolver(context);

                FhirRetrieveProvider retrieve = new Dstu3RestFhirRetrieveProvider(context, uri);
                retrieve.setTerminologyProvider(terminologyProvider);
                retrieve.setExpandValueSets(true);

                DataProvider provider = new CompositeDataProvider(model, retrieve);
                return provider;
            }
        case "4.0.0":
            throw new NotImplementedException("TODO");

        default:
            throw new IllegalArgumentException(String.format("Unknown FHIR data provider vesion: %s", version));
        }
    }

    private DataProvider getQdmProvider(String version, String uri, TerminologyProvider terminologyProvider) {
        throw new NotImplementedException("QDM data providers are not yet implemented");
    }

   
}