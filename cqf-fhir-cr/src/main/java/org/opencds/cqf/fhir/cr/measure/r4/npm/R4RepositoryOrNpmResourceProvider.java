package org.opencds.cqf.fhir.cr.measure.r4.npm;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.fhir.cql.Engines.EngineInitializationContext;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.npm.MeasureOrNpmResourceHolder;
import org.opencds.cqf.fhir.utility.npm.MeasureOrNpmResourceHolderList;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoaderWithCache;
import org.opencds.cqf.fhir.utility.npm.NpmResourceHolder;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * Combined readonly operations on Repository and NPM resources for R4 Measures and Libraries, and possibly
 * other resources such as PlanDefinition, ValueSet, etc in the future.
 */
public class R4RepositoryOrNpmResourceProvider {

    public static final String QUERIES_BY_MEASURE_IDS_ARE_NOT_SUPPORTED_BY_NPM_RESOURCES =
            "Queries by measure ID: %s are not supported by NPM resources";

    public static final String QUERIES_BY_MEASURE_IDENTIFIERS_ARE_NOT_SUPPORTED_BY_NPM_RESOURCES =
            "Queries by measure identifiers: %s are not supported by NPM resources";

    private final IRepository repository;
    private final NpmPackageLoader npmPackageLoader;
    private final EvaluationSettings evaluationSettings;

    public R4RepositoryOrNpmResourceProvider(
            IRepository repository, NpmPackageLoader npmPackageLoader, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.npmPackageLoader = npmPackageLoader;
        this.evaluationSettings = evaluationSettings;
    }

    public NpmPackageLoaderWithCache npmPackageLoaderWithCache(MeasureOrNpmResourceHolder measureOrNpmResourceHolder) {
        return NpmPackageLoaderWithCache.of(measureOrNpmResourceHolder.npmResourceHolder(), npmPackageLoader);
    }

    public NpmPackageLoaderWithCache npmPackageLoaderWithCache(
            MeasureOrNpmResourceHolderList measureOrNpmResourceHolderList) {
        return NpmPackageLoaderWithCache.of(measureOrNpmResourceHolderList.npmResourceHolders(), npmPackageLoader);
    }

    public EngineInitializationContext getEngineInitializationContext() {
        return new EngineInitializationContext(repository, npmPackageLoader, evaluationSettings);
    }

    public IRepository getRepository() {
        return repository;
    }

    public NpmPackageLoader getNpmPackageLoader() {
        return npmPackageLoader;
    }

    public EvaluationSettings getEvaluationSettings() {
        return evaluationSettings;
    }

    /**
     * method to extract Library version defined on the Measure in question
     * <p/>
     * @param measureOrNpmResourceHolder FHIR or NPM Measure that has desired Library
     * @return version identifier of Library
     */
    public VersionedIdentifier getLibraryVersionIdentifier(MeasureOrNpmResourceHolder measureOrNpmResourceHolder) {
        var url = measureOrNpmResourceHolder
                .getMainLibraryUrl()
                .orElseThrow(() -> new InvalidRequestException("Measure %s does not have a primary library specified"
                        .formatted(measureOrNpmResourceHolder.getMeasureUrl())));

        // Check to see if this Library exists in an NPM Package.  If not, search the Repository
        if (!measureOrNpmResourceHolder.hasNpmLibrary()) {
            Bundle b = this.repository.search(Bundle.class, Library.class, Searches.byCanonical(url), null);
            if (b.getEntry().isEmpty()) {
                var errorMsg = "Unable to find Library with url: %s".formatted(url);
                throw new ResourceNotFoundException(errorMsg);
            }
        }
        return VersionedIdentifiers.forUrl(url);
    }

    private static Measure foldMeasureFromRepository(
            Either3<CanonicalType, IdType, Measure> measureEither, IRepository repository) {

        return measureEither.fold(
                measureUrl -> resolveByUrlFromRepository(measureUrl, repository),
                measureIdType -> resolveMeasureById(measureIdType, repository),
                Function.identity());
    }

    public MeasureOrNpmResourceHolder foldMeasure(Either3<CanonicalType, IdType, Measure> measureEither) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            return foldMeasureEitherForNpm(measureEither);
        }

        return foldMeasureForRepository(measureEither);
    }

    @Nonnull
    private MeasureOrNpmResourceHolder foldMeasureEitherForRepository(
            Either3<IdType, String, CanonicalType> measureEither) {

        var folded = measureEither.fold(
                measureIdType -> resolveMeasureById(measureIdType, repository),
                this::resolveByIdentifier,
                measureUrl -> resolveByUrlFromRepository(measureUrl, repository));

        return MeasureOrNpmResourceHolder.measureOnly(folded);
    }

    @Nonnull
    private MeasureOrNpmResourceHolder foldMeasureForRepository(Either3<CanonicalType, IdType, Measure> measureEither) {

        var folded = measureEither.fold(
                this::resolveByUrlFromRepository,
                measureIdType -> resolveMeasureById(measureIdType, repository),
                Function.identity());

        return MeasureOrNpmResourceHolder.measureOnly(folded);
    }

    public static Measure resolveMeasureById(IIdType id, IRepository repository) {
        if (id.getValueAsString().startsWith("Measure/")) {
            // If the id is a Measure resource, we can use the read method directly
            return repository.read(Measure.class, id);
        }
        // If not, add it to ensure it plays nicely with the InMemoryFhirRepository
        return repository.read(Measure.class, new IdType(ResourceType.Measure.name(), id.getIdPart()));
    }

    private Measure resolveByUrlFromRepository(CanonicalType measureUrl) {
        return resolveByUrlFromRepository(measureUrl, repository);
    }

    private static Measure resolveByUrlFromRepository(CanonicalType measureUrl, IRepository repository) {

        var parts = Canonicals.getParts(measureUrl);
        var result = repository.search(
                Bundle.class, Measure.class, Searches.byNameAndVersion(parts.idPart(), parts.version()));
        var bundleResource = result.getEntryFirstRep().getResource();

        if (!(bundleResource instanceof Measure measure)) {
            throw new InvalidRequestException(
                    "Measure URL: %s, did not resolve to a Measure resource.".formatted(measureUrl.getValue()));
        }

        return measure;
    }

    // If the caller chooses to provide their own IRepository (ex:  federated)
    public MeasureOrNpmResourceHolder foldMeasure(
            Either3<CanonicalType, IdType, Measure> measureEither, IRepository repository) {

        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            return foldMeasureEitherForNpm(measureEither);
        }

        return MeasureOrNpmResourceHolder.measureOnly(foldMeasureFromRepository(measureEither, repository));
    }

    public MeasureOrNpmResourceHolderList foldMeasureEithers(
            List<Either3<IdType, String, CanonicalType>> measureEithers) {
        if (measureEithers == null || measureEithers.isEmpty()) {
            throw new InvalidRequestException("measure IDs or URLs parameter cannot be null or empty.");
        }

        return MeasureOrNpmResourceHolderList.of(
                measureEithers.stream().map(this::foldMeasureEither).toList());
    }

    private MeasureOrNpmResourceHolder foldMeasureEither(Either3<IdType, String, CanonicalType> measureEither) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            return foldMeasureForNpm(measureEither);
        }

        return foldMeasureEitherForRepository(measureEither);
    }

    public MeasureOrNpmResourceHolder foldMeasureEitherForNpm(Either3<CanonicalType, IdType, Measure> measureEither) {

        return measureEither.fold(
                measureUrl -> {
                    var npmResourceHolder = npmPackageLoader.loadNpmResources(measureUrl);
                    if (npmResourceHolder == null || npmResourceHolder == NpmResourceHolder.EMPTY) {
                        throw new InvalidRequestException(
                                "No NPM resources found for Measure URL: %s".formatted(measureUrl.getValue()));
                    }
                    return MeasureOrNpmResourceHolder.npmOnly(npmResourceHolder);
                },
                measureId -> {
                    throw new InvalidRequestException(
                            QUERIES_BY_MEASURE_IDS_ARE_NOT_SUPPORTED_BY_NPM_RESOURCES.formatted(measureId));
                },
                measure -> {
                    throw new InvalidRequestException(
                            "Not sure how we got here, but we have a Measure: %s".formatted(measure));
                });
    }

    private MeasureOrNpmResourceHolder foldMeasureForNpm(Either3<IdType, String, CanonicalType> measureEither) {

        return measureEither.fold(
                measureId -> {
                    throw new InvalidRequestException(
                            QUERIES_BY_MEASURE_IDS_ARE_NOT_SUPPORTED_BY_NPM_RESOURCES.formatted(measureId));
                },
                measureIdentifier -> {
                    throw new InvalidRequestException(
                            QUERIES_BY_MEASURE_IDENTIFIERS_ARE_NOT_SUPPORTED_BY_NPM_RESOURCES.formatted(
                                    measureIdentifier));
                },
                measureUrl -> {
                    var npmResourceHolder = npmPackageLoader.loadNpmResources(measureUrl);
                    if (npmResourceHolder == null || npmResourceHolder == NpmResourceHolder.EMPTY) {
                        throw new InvalidRequestException(
                                "No NPM resources found for Measure URL: %s".formatted(measureUrl.getValue()));
                    }
                    return MeasureOrNpmResourceHolder.npmOnly(npmResourceHolder);
                });
    }

    public MeasureOrNpmResourceHolderList getMeasureOrNpmDetails(
            List<IdType> measureIds, List<String> measureIdentifiers, List<String> measureCanonicals) {

        if ((measureIds == null || measureIds.isEmpty())
                && (measureCanonicals == null || measureCanonicals.isEmpty())
                && (measureIdentifiers == null || measureIdentifiers.isEmpty())) {
            throw new InvalidRequestException("measure IDs, identifiers, or URLs parameter cannot be null or empty.");
        }

        if (measureIds != null && !measureIds.isEmpty()) {
            return withDistinctByKey(getMeasureOrNpmDetailsForMeasureIds(measureIds));
        }

        if (measureCanonicals != null && !measureCanonicals.isEmpty()) {
            return withDistinctByKey(getMeasureOrNpmDetailsForMeasureCanonicals(measureCanonicals));
        }

        return withDistinctByKey(getMeasureOrNpmDetailsForMeasureIdents(measureIdentifiers));
    }

    private MeasureOrNpmResourceHolderList withDistinctByKey(
            List<MeasureOrNpmResourceHolder> measureOrNpmResourceHolders) {
        return MeasureOrNpmResourceHolderList.of(
                distinctByKey(measureOrNpmResourceHolders, MeasureOrNpmResourceHolder::getMeasureUrl));
    }

    private List<MeasureOrNpmResourceHolder> getMeasureOrNpmDetailsForMeasureIds(List<IdType> measureIds) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            throw new InvalidRequestException(
                    "Queries by measure IDs: %s are not supported by NPM resources".formatted(measureIds));
        }

        return measureIds.stream()
                .map(this::resolveMeasureById)
                .map(MeasureOrNpmResourceHolder::measureOnly)
                .toList();
    }

    private List<MeasureOrNpmResourceHolder> getMeasureOrNpmDetailsForMeasureCanonicals(
            List<String> measureCanonicals) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            return measureCanonicals.stream()
                    .map(this::resolveByUrlFromNpm)
                    .map(MeasureOrNpmResourceHolder::npmOnly)
                    .toList();
        }

        return measureCanonicals.stream()
                .map(this::resolveByUrl)
                .map(MeasureOrNpmResourceHolder::measureOnly)
                .toList();
    }

    private List<MeasureOrNpmResourceHolder> getMeasureOrNpmDetailsForMeasureIdents(List<String> measureIdentifiers) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            throw new InvalidRequestException(
                    QUERIES_BY_MEASURE_IDENTIFIERS_ARE_NOT_SUPPORTED_BY_NPM_RESOURCES.formatted(measureIdentifiers));
        }

        return measureIdentifiers.stream()
                .map(measureIdentifier ->
                        MeasureOrNpmResourceHolder.measureOnly(resolveByIdentifier(measureIdentifier)))
                .toList();
    }

    public NpmResourceHolder resolveByUrlFromNpm(String measureCanonical) {
        return this.npmPackageLoader.loadNpmResources(new CanonicalType(measureCanonical));
    }

    public static <T, K> List<T> distinctByKey(List<T> list, Function<T, K> keyExtractor) {
        Set<K> seen = new HashSet<>();
        return list.stream()
                .filter(Objects::nonNull)
                .filter(element -> seen.add(keyExtractor.apply(element)))
                .toList();
    }

    public Measure resolveMeasureById(IdType id) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            throw new InvalidRequestException(QUERIES_BY_MEASURE_IDS_ARE_NOT_SUPPORTED_BY_NPM_RESOURCES.formatted(id));
        }

        return this.repository.read(Measure.class, id);
    }

    public Measure resolveByUrl(String url) {
        Map<String, List<IQueryParameterType>> searchParameters = new HashMap<>();
        if (url.contains("|")) {
            // uri & version
            var splitId = url.split("\\|");
            var uri = splitId[0];
            var version = splitId[1];
            searchParameters.put("url", Collections.singletonList(new UriParam(uri)));
            searchParameters.put("version", Collections.singletonList(new TokenParam(version)));
        } else {
            // uri only
            searchParameters.put("url", Collections.singletonList(new UriParam(url)));
        }

        Bundle result = this.repository.search(Bundle.class, Measure.class, searchParameters);
        return (Measure) result.getEntryFirstRep().getResource();
    }

    public Measure resolveByIdentifier(String identifier) {
        List<IQueryParameterType> params = new ArrayList<>();
        Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
        Bundle bundle;
        if (identifier.contains("|")) {
            // system & value
            var splitId = identifier.split("\\|");
            var system = splitId[0];
            var code = splitId[1];
            params.add(new TokenParam(system, code));
        } else {
            // value only
            params.add(new TokenParam(identifier));
        }
        searchParams.put("identifier", params);
        bundle = this.repository.search(Bundle.class, Measure.class, searchParams);

        if (bundle != null && !bundle.getEntry().isEmpty()) {
            if (bundle.getEntry().size() > 1) {
                var msg = "Measure Identifier: %s, found more than one matching measure resource".formatted(identifier);
                throw new InvalidRequestException(msg);
            }
            return (Measure) bundle.getEntryFirstRep().getResource();
        } else {
            var msg = "Measure Identifier: %s, found no matching measure resources".formatted(identifier);
            throw new InvalidRequestException(msg);
        }
    }
}
