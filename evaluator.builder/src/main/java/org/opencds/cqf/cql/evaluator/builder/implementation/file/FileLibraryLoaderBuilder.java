package org.opencds.cqf.cql.evaluator.builder.implementation.file;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;
import org.opencds.cqf.cql.evaluator.execution.util.DirectoryBundler;
import org.opencds.cqf.cql.evaluator.translation.provider.InMemoryLibrarySourceProvider;

public class FileLibraryLoaderBuilder {
    private FhirContext fhirContext;

    public LibraryLoader build(List<String> libraryUris, Map<String, Pair<String, String>> models, CqlTranslatorOptions cqlTranslatorOptions) {
        ModelVersionHelper.setModelVersionFromLibraryPaths(models, libraryUris);       
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new LibraryManager(modelManager);

        // Using BundleLibrarySourceProvider (not sure if we want to do this yet because there is only a bundle provider for stu3)
        // for (Map.Entry<String, Pair<String, String>> m : models.entrySet()) {
        //     setFhirContext(m.getKey(), m.getValue().getLeft());
        //     IBaseBundle bundle = new DirectoryBundler(this.fhirContext).bundle(m.getValue().getRight());
        //     BundleLibraryLoaderBuilder bundleLibraryLoaderBuilder = new BundleLibraryLoaderBuilder();
        //     bundleLibraryLoaderBuilder.populateLibraryManager(libraryManager, bundle, cqlTranslatorOptions);
        // }

        // Using InMemoryLibrarySourceProvider
        List<String> libraries = new LinkedList<String>();
        libraryUris.forEach(uri -> libraries.addAll(ModelVersionHelper.getLibrariesFromPath(uri)));
        // get content text/cql base64 decoding fine for now,
        // TranslatingLibraryLoader is a hack because it always translates 
        // libraries when the optimal behaviour is only when it needs to
        libraryManager.getLibrarySourceLoader().registerProvider(new InMemoryLibrarySourceProvider(libraries));
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager, CqlTranslatorOptions.defaultOptions());
        return libraryLoader;
	}

    private void setFhirContext(String model, String version) {
        if(model.equals("http://hl7.org/fhir") || model == null) {
            setFhirContext(version);
        }
        else {
            throw new IllegalArgumentException(String.format("Libraries contained in a Fhir Bundle must be Fhir Libraries, Unknown Library: %s", model));
        }
    }

    private void setFhirContext(String version) {
        FhirVersionEnum versionEnum = ModelVersionHelper.forVersionString(version);

        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no File Library Loader implementation for anything older than DSTU2 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            throw new NotImplementedException("Sorry there is no File Library Loader implementation for anything older than Dstu3 as of now.");        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            this.fhirContext = FhirContext.forDstu3();
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no File Library Loader implementation for anything newer or equal to R4 as of now."); 
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no File Library Loader implementation for anything newer or equal to R4 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum");
        }
    }
}