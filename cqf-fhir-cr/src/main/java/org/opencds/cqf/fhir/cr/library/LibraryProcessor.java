package org.opencds.cqf.fhir.cr.library;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
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
    protected IPackageProcessor packageProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected IEvaluateProcessor evaluateProcessor;
    protected IRepository repository;
    protected EvaluationSettings evaluationSettings;

    public LibraryProcessor(IRepository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public LibraryProcessor(IRepository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null, null, null);
    }

    public LibraryProcessor(
            IRepository repository,
            EvaluationSettings evaluationSettings,
            IPackageProcessor packageProcessor,
            IDataRequirementsProcessor dataRequirementsProcessor,
            IEvaluateProcessor evaluateProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.packageProcessor = packageProcessor;
        this.dataRequirementsProcessor = dataRequirementsProcessor;
        this.evaluateProcessor = evaluateProcessor;
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
        return packageLibrary(library, packageParameters(fhirVersion, null, isPut));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageLibrary(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        return packageLibrary(resolveLibrary(library), parameters);
    }

    public IBaseBundle packageLibrary(IBaseResource library, IBaseParameters parameters) {
        var processor = packageProcessor != null ? packageProcessor : new PackageProcessor(repository);
        return processor.packageResource(library, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        return dataRequirements(resolveLibrary(library), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource library, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository, evaluationSettings);
        return processor.getDataRequirements(library, parameters);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> EvaluateRequest buildEvaluateRequest(
            Either3<C, IIdType, R> library,
            String subject,
            List<String> expression,
            IBaseParameters parameters,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine) {
        return new EvaluateRequest(
                resolveLibrary(library),
                StringUtils.isBlank(subject) ? null : Ids.newId(fhirVersion, subject),
                expression,
                parameters,
                data,
                prefetchData,
                libraryEngine,
                modelResolver);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters evaluate(
            Either3<C, IIdType, R> library,
            String subject,
            List<String> expression,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
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
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            IRepository dataRepository,
            IRepository contentRepository,
            IRepository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return evaluate(
                library,
                subject,
                expression,
                parameters,
                data,
                prefetchData,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters evaluate(
            Either3<C, IIdType, R> library,
            String subject,
            List<String> expression,
            IBaseParameters parameters,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine) {
        var processor = evaluateProcessor != null
                ? evaluateProcessor
                : new EvaluateProcessor(this.repository, this.evaluationSettings);
        return processor.evaluate(
                buildEvaluateRequest(library, subject, expression, parameters, data, prefetchData, libraryEngine));
    }
}
