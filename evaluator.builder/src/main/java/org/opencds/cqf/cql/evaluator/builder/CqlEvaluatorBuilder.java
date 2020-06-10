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

    public static void testFileR4() {
        String primaryLibrary = "EXM104_FHIR4";
        String terminologyUri = "C:\\src\\GitHub\\connectathon\\fhir4\\input\\vocabulary";
        List<String> libraries = new ArrayList<String>();
        String librariesDirectory = "C:\\src\\GitHub\\connectathon\\fhir4\\input\\pagecontent\\cql";
        libraries.add(librariesDirectory);
        Map<String, String> modelUriMap = new HashMap<String, String>();
        modelUriMap.put("http://hl7.org/fhir",
                "C:\\src\\GitHub\\connectathon\\fhir4\\input\\tests\\EXM104_FHIR4-8.1.000");
        Pair<String, Object> contextParameter = Pair.of("Patient", "denom-EXM104-FHIR4");
        // Map<Pair<String, String>, Object> parametersMap = new HashMap<Pair<String,
        // String>, Object>();
        // Interval measurementPeriod = new
        // Interval(DateHelper.resolveRequestDate(periodStart, true), true,
        // DateHelper.resolveRequestDate(periodEnd, false), true);

        // parametersMap.put(Pair.of(null, "Measurement Period"),
        // new Interval(DateTime.fromJavaDate((Date) measurementPeriod.getStart()),
        // true,
        // DateTime.fromJavaDate((Date) measurementPeriod.getEnd()), true));
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        EvaluationResult evaluationResult = cqlEvaluatorBuilder.withLibraryLoader(libraries)
                .withFileTerminologyProvider(terminologyUri).withFileDataProvider(modelUriMap).build(primaryLibrary)
                .evaluate(contextParameter);
        evaluationResult.expressionResults.entrySet().forEach(entry -> {
            System.out.println(entry.getKey() + ":\n");
            System.out.println(entry.getValue().getClass().getName());
        });
        System.out.println();
    }

    // "http://localhost:8080/cqf-ruler-r4/fhir/"
    public static void testRemoteDstu3(String libraryURLHost, String terminologyURLHost, String dataProviderURLHost)
            throws MalformedURLException {
        String primaryLibrary = "EXM161_FHIR3";
        String primaryLibraryVersion = "8.0.0";
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        versionedIdentifier.setId(primaryLibrary);
        versionedIdentifier.setVersion(primaryLibraryVersion);
        URL terminologyURL = new URL(terminologyURLHost);
        URL libraryURL = new URL(libraryURLHost);
        List<URL> dataProviderURLs = new ArrayList<URL>();
        dataProviderURLs.add(new URL(dataProviderURLHost));
        ClientFactory clientFactory = new DefaultClientFactory();
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        Pair<String, Object> contextParameter = Pair.of("Patient", "denom-EXM161-FHIR3");
        cqlEvaluatorBuilder.setClientFactory(clientFactory);
        try {
            EvaluationResult evaluationResult = cqlEvaluatorBuilder.withRemoteLibraryLoader(libraryURL)
                    .withRemoteTerminologyProvider(terminologyURL).withRemoteDataProvider(dataProviderURLs)
                    .build(versionedIdentifier).evaluate(contextParameter);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (URISyntaxException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        System.out.println("WHoa");
    }

    public static void main(String[] args) {
        String remoteR4Host = "http://localhost:8080/cqf-ruler-dstu3/fhir/";
        try {
            testRemoteDstu3(remoteR4Host, remoteR4Host, remoteR4Host);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        testFileR4();
        System.out.println("HurraY!");
    }
}