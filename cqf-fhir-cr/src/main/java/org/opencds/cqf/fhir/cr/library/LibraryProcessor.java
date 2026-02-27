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
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor;
import org.opencds.cqf.fhir.cr.common.CreateChangelogProcessor;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.DeleteProcessor;
import org.opencds.cqf.fhir.cr.common.IArtifactDiffProcessor;
import org.opencds.cqf.fhir.cr.common.ICreateChangelogProcessor;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IDeleteProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.IReleaseProcessor;
import org.opencds.cqf.fhir.cr.common.IRetireProcessor;
import org.opencds.cqf.fhir.cr.common.IReviseProcessor;
import org.opencds.cqf.fhir.cr.common.IWithdrawProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ReleaseProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.common.RetireProcessor;
import org.opencds.cqf.fhir.cr.common.ReviseProcessor;
import org.opencds.cqf.fhir.cr.common.WithdrawProcessor;
import org.opencds.cqf.fhir.cr.library.evaluate.EvaluateProcessor;
import org.opencds.cqf.fhir.cr.library.evaluate.EvaluateRequest;
import org.opencds.cqf.fhir.cr.library.evaluate.IEvaluateProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

@SuppressWarnings("UnstableApiUsage")
public class LibraryProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected IPackageProcessor packageProcessor;
    protected IReleaseProcessor releaseProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected IEvaluateProcessor evaluateProcessor;
    protected IDeleteProcessor deleteProcessor;
    protected IRetireProcessor retireProcessor;
    protected IWithdrawProcessor withdrawProcessor;
    protected IReviseProcessor reviseProcessor;
    protected IArtifactDiffProcessor artifactDiffProcessor;
    protected ICreateChangelogProcessor createChangelogProcessor;

    protected IRepository repository;
    protected CrSettings crSettings;

    public LibraryProcessor(IRepository repository) {
        this(repository, CrSettings.getDefault());
    }

    public LibraryProcessor(IRepository repository, CrSettings crSettings) {
        this(repository, crSettings, null);
    }

    public LibraryProcessor(
            IRepository repository, CrSettings crSettings, List<? extends IOperationProcessor> operationProcessors) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.crSettings = requireNonNull(crSettings, "crSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        if (operationProcessors != null && !operationProcessors.isEmpty()) {
            operationProcessors.forEach(p -> {
                if (p instanceof IPackageProcessor pack) {
                    packageProcessor = pack;
                }
                if (p instanceof IDataRequirementsProcessor dataReq) {
                    dataRequirementsProcessor = dataReq;
                }
                if (p instanceof IEvaluateProcessor evaluate) {
                    evaluateProcessor = evaluate;
                }
                if (p instanceof IDeleteProcessor delete) {
                    deleteProcessor = delete;
                }
                if (p instanceof IRetireProcessor retire) {
                    retireProcessor = retire;
                }
                if (p instanceof IWithdrawProcessor withdraw) {
                    withdrawProcessor = withdraw;
                }
                if (p instanceof IReviseProcessor revise) {
                    reviseProcessor = revise;
                }
                if (p instanceof IArtifactDiffProcessor artifactDiff) {
                    artifactDiffProcessor = artifactDiff;
                }
                if (p instanceof ICreateChangelogProcessor createChangelog) {
                    createChangelogProcessor = createChangelog;
                }
            });
        }
    }

    public CrSettings settings() {
        return crSettings;
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
        var processor = packageProcessor != null ? packageProcessor : new PackageProcessor(repository, crSettings);
        return processor.packageResource(library, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle releaseLibrary(
            Either3<C, IIdType, R> library) {
        return releaseLibrary(library, false);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle releaseLibrary(
            Either3<C, IIdType, R> library, boolean isPut) {
        return releaseLibrary(library, packageParameters(fhirVersion, null, isPut));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle releaseLibrary(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        return releaseLibrary(resolveLibrary(library), parameters);
    }

    public IBaseBundle releaseLibrary(IBaseResource library, IBaseParameters parameters) {
        var processor = releaseProcessor != null
                ? releaseProcessor
                : new ReleaseProcessor(repository, crSettings.getTerminologyServerClientSettings());
        return processor.releaseResource(library, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        return dataRequirements(resolveLibrary(library), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource library, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository, crSettings.getEvaluationSettings());
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
                new LibraryEngine(repository, crSettings.getEvaluationSettings()));
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
                : new EvaluateProcessor(repository, crSettings.getEvaluationSettings());
        return processor.evaluate(
                buildEvaluateRequest(library, subject, expression, parameters, data, prefetchData, libraryEngine));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle deleteLibrary(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        var processor = deleteProcessor != null ? deleteProcessor : new DeleteProcessor(repository);
        return processor.deleteResource(resolveLibrary(library), parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle retireLibrary(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        var processor = retireProcessor != null ? retireProcessor : new RetireProcessor(repository);
        return processor.retireResource(resolveLibrary(library), parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle withdrawLibrary(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        var processor = withdrawProcessor != null ? withdrawProcessor : new WithdrawProcessor(repository);
        return processor.withdrawResource(resolveLibrary(library), parameters);
    }

    public IBaseResource reviseLibrary(IBaseResource resource) {
        var processor = reviseProcessor != null ? reviseProcessor : new ReviseProcessor(repository);
        return processor.reviseResource(resource);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters artifactDiff(
            Either3<C, IIdType, R> sourceLibrary,
            Either3<C, IIdType, R> targetLibrary,
            Boolean compareComputable,
            Boolean compareExecutable,
            IBaseResource terminologyEndpoint) {
        var processor = artifactDiffProcessor != null ? artifactDiffProcessor : new ArtifactDiffProcessor();
        return processor.getArtifactDiff(
                resolveLibrary(sourceLibrary),
                resolveLibrary(targetLibrary),
                compareComputable,
                compareExecutable,
                null,
                terminologyEndpoint);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource createChangelog(
            Either3<C, IIdType, R> sourceLibrary,
            Either3<C, IIdType, R> targetLibrary,
            IBaseResource terminologyEndpoint) {
        var processor = createChangelogProcessor != null ? createChangelogProcessor : new CreateChangelogProcessor();
        return processor.createChangelog(
                resolveLibrary(sourceLibrary), resolveLibrary(targetLibrary), terminologyEndpoint);
    }
}
