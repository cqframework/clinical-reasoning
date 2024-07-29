package org.opencds.cqf.fhir.cr.library;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.Parameters.newBooleanPart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.library.evaluate.EvaluateProcessor;
import org.opencds.cqf.fhir.cr.library.evaluate.EvaluateRequest;
import org.opencds.cqf.fhir.cr.library.evaluate.IEvaluateProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class LibraryProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected final IPackageProcessor packageProcessor;
    protected final IEvaluateProcessor evaluateProcessor;
    protected Repository repository;
    protected EvaluationSettings evaluationSettings;

    public LibraryProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public LibraryProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null, null);
    }

    public LibraryProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            IPackageProcessor packageProcessor,
            IEvaluateProcessor evaluateProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.packageProcessor = packageProcessor != null ? packageProcessor : new PackageProcessor(this.repository);
        this.evaluateProcessor = evaluateProcessor != null
                ? evaluateProcessor
                : new EvaluateProcessor(this.repository, this.evaluationSettings);
    }

    public EvaluationSettings evaluationSettings() {
        return evaluationSettings;
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveLibrary(
            Either3<C, IIdType, R> library) {
        return new ResourceResolver("Library", repository).resolve(library);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageLibrary(
            Either3<C, IIdType, R> library) {
        return packageLibrary(library, false);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageLibrary(
            Either3<C, IIdType, R> library, boolean isPut) {
        return packageLibrary(
                library,
                newParameters(
                        repository.fhirContext(),
                        "package-parameters",
                        newBooleanPart(repository.fhirContext(), "isPut", isPut)));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageLibrary(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        return packageLibrary(resolveLibrary(library), parameters);
    }

    public IBaseBundle packageLibrary(IBaseResource questionnaire, IBaseParameters parameters) {
        return packageProcessor.packageResource(questionnaire, parameters);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> EvaluateRequest buildEvaluateRequest(
            Either3<C, IIdType, R> library,
            String subject,
            List<String> expression,
            IBaseParameters parameters,
            Boolean useServerData,
            IBaseBundle data,
            IBaseParameters prefetchData,
            LibraryEngine libraryEngine) {
        return new EvaluateRequest(
                resolveLibrary(library),
                StringUtils.isBlank(subject) ? null : Ids.newId(fhirVersion, subject),
                expression,
                parameters,
                useServerData,
                data,
                libraryEngine,
                modelResolver);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters evaluate(
            Either3<C, IIdType, R> library,
            String subject,
            List<String> expression,
            IBaseParameters parameters,
            Boolean useServerData,
            IBaseBundle data,
            IBaseParameters prefetchData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return evaluate(
                library,
                subject,
                expression,
                parameters,
                useServerData,
                data,
                prefetchData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters evaluate(
            Either3<C, IIdType, R> library,
            String subject,
            List<String> expression,
            IBaseParameters parameters,
            Boolean useServerData,
            IBaseBundle data,
            IBaseParameters prefetchData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return evaluate(
                library,
                subject,
                expression,
                parameters,
                useServerData,
                data,
                prefetchData,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters evaluate(
            Either3<C, IIdType, R> library,
            String subject,
            List<String> expression,
            IBaseParameters parameters,
            Boolean useServerData,
            IBaseBundle data,
            IBaseParameters prefetchData,
            LibraryEngine libraryEngine) {
        return evaluateProcessor.evaluate(buildEvaluateRequest(
                library, subject, expression, parameters, useServerData, data, prefetchData, libraryEngine));
    }
}
