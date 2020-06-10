package org.opencds.cqf.cql.evaluator.builder.implementation.string;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;
import org.opencds.cqf.cql.evaluator.translation.provider.InMemoryLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;

public class StringLibraryLoaderBuilder {

	public LibraryLoader build(List<String> libraries, Map<String, Pair<String, String>> models,  CqlTranslatorOptions cqlTranslatorOptions) {
		ModelVersionHelper.setModelVersionFromLibraryStrings(models, libraries);
		ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().registerProvider(new InMemoryLibrarySourceProvider(libraries));
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager, CqlTranslatorOptions.defaultOptions());
        return libraryLoader;
	}
}