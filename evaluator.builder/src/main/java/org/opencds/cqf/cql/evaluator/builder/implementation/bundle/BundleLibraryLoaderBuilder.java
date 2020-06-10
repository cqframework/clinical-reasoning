package org.opencds.cqf.cql.evaluator.builder.implementation.bundle;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.translation.provider.stu3.Stu3BundleLibrarySourceProvider;

import ca.uhn.fhir.context.FhirVersionEnum;

public class BundleLibraryLoaderBuilder {

	public LibraryLoader build(IBaseBundle bundle, Map<String, Pair<String, String>> models,  CqlTranslatorOptions cqlTranslatorOptions) {
        ModelVersionHelper.setModelVersionFromBundle(models, bundle);
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new LibraryManager(modelManager);
        populateLibraryManager(libraryManager, bundle, cqlTranslatorOptions);
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager, CqlTranslatorOptions.defaultOptions());
        return libraryLoader;
    }
    
    public void populateLibraryManager(LibraryManager libraryManager, IBaseBundle bundle, CqlTranslatorOptions cqlTranslatorOptions) {
        FhirVersionEnum versionEnum = bundle.getStructureFhirVersionEnum();

        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no bundle implementation for anything older than DSTU2 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            throw new NotImplementedException("Sorry there is no bundle implementation for anything older than Dstu3 as of now.");        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            libraryManager.getLibrarySourceLoader().registerProvider(new Stu3BundleLibrarySourceProvider((org.hl7.fhir.dstu3.model.Bundle)bundle));       
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no bundle implementation for anything newer or equal to R4 as of now."); 
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no bundle implementation for anything newer or equal to R4 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum");
        }

        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
    }
}