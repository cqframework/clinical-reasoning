package org.opencds.cqf.fhir.cr.measure.r4.npm;

import static org.slf4j.LoggerFactory.getLogger;

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
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.cql.npm.FhirOrNpmThingee;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.npm.MeasureOrNpmResourceHolder;
import org.opencds.cqf.fhir.utility.npm.MeasureOrNpmResourceHolderList;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoaderWithCache;
import org.opencds.cqf.fhir.utility.npm.NpmResourceHolder;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;

/**
 * Combined readonly operations on Repository and NPM resources for R4 Measures and Libraries, and possibly
 * other resources such as PlanDefinition, ValueSet, etc in the future.
 */
public class R4FhirOrNpmResourceProvider implements FhirOrNpmThingee {

    private static final Logger log = getLogger(R4FhirOrNpmResourceProvider.class);

    private final IRepository repository;
    private final NpmPackageLoader npmPackageLoader;
    private final EvaluationSettings evaluationSettings;

    public R4FhirOrNpmResourceProvider withRepositoryIfNonNpm(IRepository repository) {
        if (this.evaluationSettings.isUseNpmForQualifyingResources() || repository == this.repository) {
            return this;
        }

        return new R4FhirOrNpmResourceProvider(repository, this.npmPackageLoader, this.evaluationSettings);
    }

    public R4FhirOrNpmResourceProvider(
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

    @Override
    public IRepository getRepository() {
        return repository;
    }

    @Override
    public NpmPackageLoader getNpmPackageLoader() {
        return npmPackageLoader;
    }

    @Override
    public EvaluationSettings getEvaluationSettings() {
        return evaluationSettings;
    }

    public List<Either3<CanonicalType, IdType, Measure>> getMeasureEithers(
            List<String> measureIds, List<String> measureUrls) {
        if (measureIds != null && !measureIds.isEmpty()) {
            return measureIds.stream()
                    .map(measureId -> Eithers.<CanonicalType, IdType, Measure>forMiddle3(new IdType(measureId)))
                    .toList();
        }

        if (measureUrls != null && !measureUrls.isEmpty()) {
            return measureUrls.stream()
                    .map(measureUrl -> Eithers.<CanonicalType, IdType, Measure>forLeft3(new CanonicalType(measureUrl)))
                    .toList();
        }

        // LUKETODO:  not sure if this is right, but this is what the step2 test expects
        return List.of();
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
            return foldMeasureForNpm(measureEither);
        }

        return foldMeasureForRepository(measureEither);
    }

    public MeasureOrNpmResourceHolder foldWithCustomIdTypeHandler(
            Either3<CanonicalType, IdType, Measure> measureEither, Function<? super IdType, Measure> foldMiddle) {

        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            return foldMeasureForNpm(measureEither);
        }

        return measureEither.fold(
                measureUrl -> {
                    throw new InvalidRequestException(
                            "Queries by measure URL: %s are not supported by NPM resources".formatted(measureUrl));
                },
                foldMiddle.andThen(MeasureOrNpmResourceHolder::measureOnly),
                measureInput -> {
                    throw new InvalidRequestException(
                            "Not sure how we got here, but we have a Measure: %s".formatted(measureInput));
                });
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

    // LUKETODO:  do we still need this???
    // Wrap in MeasureOrNpmResourceHolderList for convenience
    public MeasureOrNpmResourceHolderList resolveByIdsToMeasuresOrNpms(List<? extends IIdType> ids) {
        return MeasureOrNpmResourceHolderList.ofMeasures(resolveByIds(ids));
    }

    public List<Measure> resolveByIds(List<? extends IIdType> ids) {
        return resolveMeasuresFromRepository(ids, repository);
    }

    private static List<Measure> resolveMeasuresFromRepository(List<? extends IIdType> ids, IRepository repository) {
        var idStringArray = ids.stream().map(IPrimitiveType::getValueAsString).toArray(String[]::new);
        var searchParameters = Searches.byId(idStringArray);

        return repository.search(Bundle.class, Measure.class, searchParameters).getEntry().stream()
                .map(BundleEntryComponent::getResource)
                .filter(Measure.class::isInstance)
                .map(Measure.class::cast)
                .toList();
    }

    // If the caller chooses to provide their own IRepository (ex:  federated)
    public MeasureOrNpmResourceHolder foldMeasure(
            Either3<CanonicalType, IdType, Measure> measureEither, IRepository repository) {

        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            return foldMeasureForNpm(measureEither);
        }

        return MeasureOrNpmResourceHolder.measureOnly(foldMeasureFromRepository(measureEither, repository));
    }

    public MeasureOrNpmResourceHolderList foldMeasures(List<Either3<CanonicalType, IdType, Measure>> measureEithers) {
        if (measureEithers == null || measureEithers.isEmpty()) {
            throw new InvalidRequestException("measure IDs or URLs parameter cannot be null or empty.");
        }

        return MeasureOrNpmResourceHolderList.of(
                measureEithers.stream().map(this::foldMeasure).toList());
    }

    public MeasureOrNpmResourceHolder foldMeasureForNpm(Either3<CanonicalType, IdType, Measure> measureEither) {

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
                            "Queries by measure ID: %s are not supported by NPM resources".formatted(measureId));
                },
                measure -> {
                    throw new InvalidRequestException(
                            "Not sure how we got here, but we have a Measure: %s".formatted(measure));
                });
    }

    public MeasureOrNpmResourceHolderList getMeasureOrNpmDetails(
            List<IdType> measureIds, List<String> measureIdentifiers, List<String> measureCanonicals) {

        List<MeasureOrNpmResourceHolder> measuresPlusResourceHolders = new ArrayList<>();
        if (measureIds != null && !measureIds.isEmpty()) {
            if (evaluationSettings.isUseNpmForQualifyingResources()) {
                throw new InvalidRequestException(
                        "Queries by measure IDs: %s are not supported by NPM resources".formatted(measureIds));
            }

            for (IdType measureId : measureIds) {
                var measureById = resolveMeasureById(measureId);
                measuresPlusResourceHolders.add(MeasureOrNpmResourceHolder.measureOnly(measureById));
            }
        }

        if (measureCanonicals != null && !measureCanonicals.isEmpty()) {
            for (String measureCanonical : measureCanonicals) {
                if (evaluationSettings.isUseNpmForQualifyingResources()) {
                    var npmResourceHolder = resolveByUrlFromNpm(measureCanonical);
                    measuresPlusResourceHolders.add(MeasureOrNpmResourceHolder.npmOnly(npmResourceHolder));
                } else {
                    var measureByUrl = resolveByUrl(measureCanonical);
                    if (measureByUrl != null) {
                        measuresPlusResourceHolders.add(MeasureOrNpmResourceHolder.measureOnly(measureByUrl));
                    }
                }
            }
        }

        if (measureIdentifiers != null && !measureIdentifiers.isEmpty()) {
            if (evaluationSettings.isUseNpmForQualifyingResources()) {
                throw new InvalidRequestException(
                        "Queries by measure identifiers: %s are not supported by NPM resources"
                                .formatted(measureIdentifiers));
            }
            for (String measureIdentifier : measureIdentifiers) {
                var measureByIdentifier = resolveByIdentifier(measureIdentifier);
                measuresPlusResourceHolders.add(MeasureOrNpmResourceHolder.measureOnly(measureByIdentifier));
            }
        }

        return MeasureOrNpmResourceHolderList.of(
                distinctByKey(measuresPlusResourceHolders, MeasureOrNpmResourceHolder::getMeasureUrl));
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

    public Library resolveLibraryById(IdType id) {
        // LUKETODO: what to do here????
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            throw new InvalidRequestException(
                    "Queries by measure ID: %s are not supported by NPM resources".formatted(id));
        }

        return this.repository.read(Library.class, id);
    }

    public Measure resolveMeasureById(IdType id) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            throw new InvalidRequestException(
                    "Queries by measure ID: %s are not supported by NPM resources".formatted(id));
        }

        return this.repository.read(Measure.class, id);
    }

    public Measure resolveByUrl(CanonicalType measureUrl) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            final NpmResourceHolder npmResourceHolder = npmPackageLoader.loadNpmResources(measureUrl);

            var optMeasureAdapter = npmResourceHolder.getMeasure();

            if (optMeasureAdapter.isEmpty()) {
                throw new IllegalArgumentException("No measure found for URL: %s".formatted(measureUrl.getValue()));
            }

            var measureAdapter = optMeasureAdapter.get();

            if (!(measureAdapter.get() instanceof Measure measure)) {
                throw new IllegalArgumentException("MeasureAdapter is not a Measure for URL: %s".formatted(measureUrl));
            }

            return measure;
        }

        return resolveByUrl(measureUrl.getValue());
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

    public Library resolveLibraryByUrl(CanonicalType measureUrl) {
        if (evaluationSettings.isUseNpmForQualifyingResources()) {
            var optLibraryAdapter = npmPackageLoader.loadLibraryByUrl(measureUrl);

            if (optLibraryAdapter.isEmpty()) {
                throw new IllegalArgumentException("No Library found for URL: %s".formatted(measureUrl.getValue()));
            }

            var libraryAdapter = optLibraryAdapter.get();

            if (!(libraryAdapter.get() instanceof Library library)) {
                throw new IllegalArgumentException("MeasureAdapter is not a Measure for URL: %s".formatted(measureUrl));
            }

            return library;
        }

        return resolveLibraryByUrl(measureUrl);
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
