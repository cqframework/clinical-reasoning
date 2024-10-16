package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerOptions;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.fhir.exception.FhirVersionMisMatchException;
import org.opencds.cqf.cql.engine.fhir.retrieve.BaseFhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.R4FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.cql.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.helper.DateHelper;
import org.opencds.cqf.fhir.cr.measure.helper.SubjectContext;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Canonicals.CanonicalParts;
import org.opencds.cqf.fhir.utility.Libraries;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4DataRequirementsService {
    private static final Logger ourLog = LoggerFactory.getLogger(R4DataRequirementsService.class);
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private static final String EXTENSION_URL_FHIR_QUERY_PATTERN =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern";

    public R4DataRequirementsService(Repository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
    }

    public Library dataRequirements(IdType measureId, String periodStart, String periodEnd) {

        Measure measure = repository.read(Measure.class, measureId);
        Library library = getLibraryFromMeasure(measure);

        if (library == null) {
            throw new ResourceNotFoundException(measure.getLibrary().get(0).asStringValue());
        }

        Map<String, Object> parameters = new HashMap<>();

        Interval measurementPeriod;
        if (StringUtils.isNotBlank(periodStart) && StringUtils.isNotBlank(periodEnd)) {
            measurementPeriod = new Interval(
                    DateHelper.resolveRequestDate(periodStart, true),
                    true,
                    DateHelper.resolveRequestDate(periodEnd, false),
                    true);
            parameters.put("MeasurementPeriod", measurementPeriod);

            return processDataRequirements(measure, library, parameters);
        } else {
            ourLog.warn("periodStart & periodEnd was defaulted to empty");

            return processDataRequirements(library, parameters);
        }
    }

    public Library getLibraryFromMeasure(Measure measure) {
        Iterator<CanonicalType> libraryIter = measure.getLibrary().iterator();

        String libraryIdOrCanonical = null;
        // use the first library
        while (libraryIter.hasNext() && libraryIdOrCanonical == null) {
            CanonicalType ref = libraryIter.next();

            if (ref != null) {
                libraryIdOrCanonical = ref.getValue();
            }
        }

        Library library = null;

        try {
            library = repository.read(Library.class, new IdType(libraryIdOrCanonical));
        } catch (Exception e) {
            ourLog.info("Library read failed as measure.getLibrary() is not an ID, fall back to search as canonical");
        }
        if (library == null) {
            library = fetchDependencyLibrary(libraryIdOrCanonical);
        }
        return library;
    }

    private static LibrarySourceProvider buildLibrarySource(Repository repository) {
        AdapterFactory adapterFactory = new AdapterFactory();
        return new RepositoryFhirLibrarySourceProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
    }

    private LibraryManager createLibraryManager(Library library) {
        var librarySourceProvider = buildLibrarySource(repository);

        Bundle libraryBundle = new Bundle();
        List<Library> listLib = fetchDependencyLibraries(library);
        listLib.add(library);

        listLib.forEach(lib -> {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            component.setResource(lib);
            libraryBundle.addEntry(component);
        });

        List<LibrarySourceProvider> sourceProviders =
                new ArrayList<>(Arrays.asList(librarySourceProvider, librarySourceProvider));
        LibraryManager ll = new LibraryManager(new ModelManager());
        for (LibrarySourceProvider lsp : sourceProviders) {
            ll.getLibrarySourceLoader().registerProvider(lsp);
        }
        return ll;
    }

    public static CqlTranslator getTranslator(InputStream cqlStream, LibraryManager libraryManager) {
        CqlTranslator translator;
        try {
            translator = CqlTranslator.fromStream(cqlStream, libraryManager);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Errors occurred translating library: %s", e.getMessage()));
        }

        return translator;
    }

    private CqlTranslator translateLibrary(Library library, LibraryManager libraryManager) {
        CqlTranslator translator = getTranslator(
                new ByteArrayInputStream(
                        Libraries.getContent(library, "text/cql").get()),
                libraryManager);
        if (!translator.getErrors().isEmpty()) {
            throw new RuntimeException(translator.getErrors().get(0).getMessage());
        }
        return translator;
    }

    private Library processDataRequirements(Library library, Map<String, Object> parameters) {
        LibraryManager libraryManager = createLibraryManager(library);
        CqlTranslator translator = translateLibrary(library, libraryManager);

        ModelResolver modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
        var searchParameterResolver = new SearchParameterResolver(repository.fhirContext());
        var terminologyProvider = new RepositoryTerminologyProvider(
                repository,
                measureEvaluationOptions.getEvaluationSettings().getValueSetCache(),
                measureEvaluationOptions.getEvaluationSettings().getTerminologySettings());

        // TODO: Enable passing a capability statement as a parameter to the operation
        return getModuleDefinitionLibraryR4(
                libraryManager,
                translator.getTranslatedLibrary(),
                searchParameterResolver,
                terminologyProvider,
                modelResolver,
                null,
                parameters,
                measureEvaluationOptions);
    }

    public static Library getModuleDefinitionLibraryR4(
            LibraryManager libraryManager,
            CompiledLibrary translatedLibrary,
            SearchParameterResolver searchParameterResolver,
            TerminologyProvider terminologyProvider,
            ModelResolver modelResolver,
            IBaseConformance capStatement,
            Map<String, Object> parameters,
            MeasureEvaluationOptions measureEvaluationOptions) {

        org.hl7.fhir.r5.model.Library libraryR5 = getModuleDefinitionLibraryR5(
                libraryManager,
                translatedLibrary,
                measureEvaluationOptions.getEvaluationSettings().getCqlOptions().getCqlCompilerOptions(),
                parameters);

        VersionConvertor_40_50 versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());

        org.hl7.fhir.r4.model.Library libraryR4 =
                (org.hl7.fhir.r4.model.Library) versionConvertor_40_50.convertResource(libraryR5);

        libraryR4 = addDataRequirementFhirQueries(
                libraryR4,
                searchParameterResolver,
                terminologyProvider,
                modelResolver,
                measureEvaluationOptions,
                capStatement);

        return libraryR4;
    }

    private Library processDataRequirements(Measure measure, Library library, Map<String, Object> parameters) {
        LibraryManager libraryManager = createLibraryManager(library);
        CqlTranslator translator = translateLibrary(library, libraryManager);

        ModelResolver modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
        var searchParameterResolver = new SearchParameterResolver(repository.fhirContext());
        var terminologyProvider = new RepositoryTerminologyProvider(
                repository,
                measureEvaluationOptions.getEvaluationSettings().getValueSetCache(),
                measureEvaluationOptions.getEvaluationSettings().getTerminologySettings());

        // TODO: Enable passing a capability statement as a parameter to the operation
        return getModuleDefinitionLibraryR4(
                measure,
                libraryManager,
                translator.getTranslatedLibrary(),
                measureEvaluationOptions,
                searchParameterResolver,
                terminologyProvider,
                modelResolver,
                null,
                parameters);
    }

    private List<Library> fetchDependencyLibraries(Library library) {
        Map<String, Library> resources = new HashMap<>();
        List<Library> queue = new ArrayList<>();
        queue.add(library);

        while (!queue.isEmpty()) {
            Library current = queue.get(0);
            queue.remove(0);
            visitLibrary(current, queue, resources);
        }
        return new ArrayList<>(resources.values());
    }

    private void visitLibrary(Library library, List<Library> queue, Map<String, Library> resources) {
        for (RelatedArtifact relatedArtifact : library.getRelatedArtifact()) {
            if (relatedArtifact.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                    && relatedArtifact.hasResource()) {

                // FHIR R4+, resource is defined as a canonical
                String resourceCanonical = relatedArtifact.getResource();
                Library lib = fetchDependencyLibrary(resourceCanonical);
                if (lib != null) {
                    resources.putIfAbsent(lib.getId(), lib);
                    queue.add(lib);
                }
            }
        }
    }

    public List<IBaseResource> fetchbundleEntries(List<BundleEntryComponent> bundleList) {
        List<IBaseResource> resources = new ArrayList<>();
        for (BundleEntryComponent drq : bundleList) {
            resources.add(drq.getResource());
        }
        return resources;
    }

    private Library fetchDependencyLibrary(String resourceCanonical) {

        Library library = null;
        CanonicalParts parts = Canonicals.getParts(resourceCanonical);

        if (parts.resourceType().equals("Library")) {
            List<BundleEntryComponent> bundleList = repository
                    .search(Bundle.class, Library.class, Searches.byCanonical(resourceCanonical))
                    .getEntry();

            if (bundleList != null && !bundleList.isEmpty()) {

                if (bundleList.size() == 1) {
                    library = (Library) bundleList.get(0).getResource();
                } else {
                    AdapterFactory adapterFactory = new AdapterFactory();
                    LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
                    ILibraryAdapter libAdapter =
                            adapterFactory.createLibrary(bundleList.get(0).getResource());
                    VersionedIdentifier identifier = new VersionedIdentifier();
                    if (StringUtils.isNotBlank(libAdapter.getName())) {
                        identifier.setId(libAdapter.getName());
                    }
                    if (StringUtils.isNotBlank(parts.version())) {
                        identifier.setVersion(parts.version());
                    }
                    library = (Library) libraryVersionSelector.select(identifier, fetchbundleEntries(bundleList));
                }
            }
        }
        return library;
    }

    public static Library getModuleDefinitionLibraryR4(
            Measure measureToUse,
            LibraryManager libraryManager,
            CompiledLibrary translatedLibrary,
            MeasureEvaluationOptions measureEvaluationOptions,
            SearchParameterResolver searchParameterResolver,
            TerminologyProvider terminologyProvider,
            ModelResolver modelResolver,
            IBaseConformance capStatement,
            Map<String, Object> parameters) {

        VersionConvertor_40_50 versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
        org.hl7.fhir.r5.model.Measure r5Measure =
                (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measureToUse);

        org.hl7.fhir.r5.model.Library effectiveDataRequirements = getModuleDefinitionLibraryR5(
                r5Measure,
                libraryManager,
                translatedLibrary,
                measureEvaluationOptions.getEvaluationSettings().getCqlOptions().getCqlCompilerOptions(),
                parameters);

        org.hl7.fhir.r4.model.Library r4EffectiveDataRequirements =
                (org.hl7.fhir.r4.model.Library) versionConvertor_40_50.convertResource(effectiveDataRequirements);
        r4EffectiveDataRequirements = addDataRequirementFhirQueries(
                r4EffectiveDataRequirements,
                searchParameterResolver,
                terminologyProvider,
                modelResolver,
                measureEvaluationOptions,
                capStatement);
        return r4EffectiveDataRequirements;
    }

    public static org.hl7.fhir.r5.model.Library getModuleDefinitionLibraryR5(
            LibraryManager libraryManager,
            CompiledLibrary translatedLibrary,
            CqlCompilerOptions options,
            Map<String, Object> parameters) {
        DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();

        return dqReqTrans.gatherDataRequirements(
                libraryManager, translatedLibrary, options, null, parameters, true, true);
    }

    public static org.hl7.fhir.r5.model.Library getModuleDefinitionLibraryR5(
            org.hl7.fhir.r5.model.Measure measureToUse,
            LibraryManager libraryManager,
            CompiledLibrary translatedLibrary,
            CqlCompilerOptions options,
            Map<String, Object> parameters) {
        Set<String> expressionList = getExpressions(measureToUse);
        DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();

        return dqReqTrans.gatherDataRequirements(
                libraryManager, translatedLibrary, options, expressionList, parameters, true, true);
    }

    private static Set<String> getExpressions(org.hl7.fhir.r5.model.Measure measureToUse) {
        Set<String> expressionSet = new HashSet<>();
        measureToUse
                .getSupplementalData()
                .forEach(supData -> expressionSet.add(supData.getCriteria().getExpression()));
        measureToUse.getGroup().forEach(groupMember -> {
            groupMember
                    .getPopulation()
                    .forEach(population ->
                            expressionSet.add(population.getCriteria().getExpression()));
            groupMember
                    .getStratifier()
                    .forEach(stratifier ->
                            expressionSet.add(stratifier.getCriteria().getExpression()));
        });
        return expressionSet;
    }

    private static org.hl7.fhir.r4.model.Library addDataRequirementFhirQueries(
            org.hl7.fhir.r4.model.Library library,
            SearchParameterResolver searchParameterResolver,
            TerminologyProvider terminologyProvider,
            ModelResolver modelResolver,
            MeasureEvaluationOptions measureEvaluationOptions,
            IBaseConformance capStatement) {
        List<org.hl7.fhir.r4.model.DataRequirement> dataReqs = library.getDataRequirement();

        try {
            BaseFhirQueryGenerator fhirQueryGenerator =
                    new R4FhirQueryGenerator(searchParameterResolver, terminologyProvider, modelResolver);

            if (measureEvaluationOptions
                            .getEvaluationSettings()
                            .getCqlOptions()
                            .getCqlEngineOptions()
                            .getPageSize()
                    != null) {
                fhirQueryGenerator.setPageSize(measureEvaluationOptions
                        .getEvaluationSettings()
                        .getCqlOptions()
                        .getCqlEngineOptions()
                        .getPageSize());
            }
            fhirQueryGenerator.setExpandValueSets(measureEvaluationOptions
                    .getEvaluationSettings()
                    .getCqlOptions()
                    .getCqlEngineOptions()
                    .shouldExpandValueSets());

            Integer maxCodesPerQuery = measureEvaluationOptions
                    .getEvaluationSettings()
                    .getCqlOptions()
                    .getCqlEngineOptions()
                    .getMaxCodesPerQuery();
            if (maxCodesPerQuery != null && maxCodesPerQuery > 0) {
                fhirQueryGenerator.setMaxCodesPerQuery(measureEvaluationOptions
                        .getEvaluationSettings()
                        .getCqlOptions()
                        .getCqlEngineOptions()
                        .getMaxCodesPerQuery());
            }

            Integer queryBatchThreshold = measureEvaluationOptions
                    .getEvaluationSettings()
                    .getCqlOptions()
                    .getCqlEngineOptions()
                    .getQueryBatchThreshold();
            if (queryBatchThreshold != null && queryBatchThreshold > 0) {
                fhirQueryGenerator.setQueryBatchThreshold(measureEvaluationOptions
                        .getEvaluationSettings()
                        .getCqlOptions()
                        .getCqlEngineOptions()
                        .getQueryBatchThreshold());
            }

            Map<String, Object> contextValues = new HashMap<>();
            SubjectContext contextValue = getContextForSubject(library.getSubject());
            contextValues.put(contextValue.getContextType(), contextValue.getContextValue());

            for (org.hl7.fhir.r4.model.DataRequirement drq : dataReqs) {
                // TODO: Support DataRequirement-level subject overrides
                List<String> queries =
                        fhirQueryGenerator.generateFhirQueries(drq, null, contextValues, null, capStatement);
                for (String query : queries) {
                    org.hl7.fhir.r4.model.Extension ext = new org.hl7.fhir.r4.model.Extension();
                    ext.setUrl(EXTENSION_URL_FHIR_QUERY_PATTERN);
                    ext.setValue(new org.hl7.fhir.r4.model.StringType(query));
                    drq.getExtension().add(ext);
                }
            }
        } catch (FhirVersionMisMatchException e) {
            ourLog.debug("Error attempting to generate FHIR queries: {}", e.getMessage());
        }

        return library;
    }

    private static SubjectContext getContextForSubject(Type subject) {
        String contextType = "Patient";

        if (subject instanceof CodeableConcept) {
            for (Coding c : ((CodeableConcept) subject).getCoding()) {
                if ("http://hl7.org/fhir/resource-types".equals(c.getSystem())) {
                    contextType = c.getCode();
                }
            }
        }
        return new SubjectContext(contextType, String.format("{{context.%sId}}", contextType.toLowerCase()));
    }
}
