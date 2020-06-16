package org.opencds.cqf.cql.evaluator.builder.implementation.remote;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.translation.provider.stu3.Stu3ServerLibrarySourceProvider;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class RemoteLibraryLoaderBuilder {

    public LibraryLoader build(URL url, Map<String, Pair<String, String>> models, ClientFactory clientFactory)
            throws IOException, InterruptedException, URISyntaxException {
        if (clientFactory == null) {
            throw new IllegalArgumentException(String.format("Needed to access remote url %s and ClientFactory was null."));
        }
        IGenericClient client = clientFactory.create(url);
        ModelVersionHelper.setModelVersionFromServer(models, client);
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new LibraryManager(modelManager);
        FhirVersionEnum versionEnum = client.getFhirContext().getVersion().getVersion();
        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no Remote Library Loader implementation for anything older than DSTU3 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            throw new NotImplementedException("Sorry there is no Remote Library Loader implementation for anything older than DSTU3 as of now.");      
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            libraryManager.getLibrarySourceLoader().registerProvider(new Stu3ServerLibrarySourceProvider(client));
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no Remote Library Loader implementation for anything newer than or equal to R4 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no Remote Library Loader implementation for anything newer than or equal to R4 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum");
        }
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager, CqlTranslatorOptions.defaultOptions());
        return libraryLoader;
    }
}