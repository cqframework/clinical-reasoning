package org.opencds.cqf.cql.evaluator.builder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderLibraryContext;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.builder.factory.DefaultClientFactory;

/**
 * API for Building any Providers or Loaders needed for CQL Evaluation
 */
public class CqlEvaluatorBuilder extends BuilderLibraryContext {

    public CqlEvaluator build(String primaryLibrary) {
        validateBuilderContext();

        // add checks for if loader and providers have not been built yet... (Loader
        // must exist... others maybe warn?
        // Should that live in validateBuilderContext?)
        LibraryLoader libraryLoader = this.getLibraryLoader();

        TerminologyProvider terminologyProvider = this.getTerminologyProvider();

        Map<String, DataProvider> dataProviders = this.getDataProvider();

        return new CqlEvaluator(libraryLoader, primaryLibrary, dataProviders, terminologyProvider,
                this.getEngineOptions(), this.getDefaultParameterDeserializer());
    }

    public CqlEvaluator build(VersionedIdentifier primaryLibrary) {
        validateBuilderContext();

        // add checks for if loader and providers have not been built yet... (Loader
        // must exist... others maybe warn?
        // Should that live in validateBuilderContext?)
        LibraryLoader libraryLoader = this.getLibraryLoader();

        TerminologyProvider terminologyProvider = this.getTerminologyProvider();

        Map<String, DataProvider> dataProviders = this.getDataProvider();

        return new CqlEvaluator(libraryLoader, primaryLibrary, dataProviders, terminologyProvider,
                this.getEngineOptions(), this.getDefaultParameterDeserializer());
    }

    /**
     * 
     */
    public void validateBuilderContext() {

    }
}