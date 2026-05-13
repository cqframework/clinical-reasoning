package org.opencds.cqf.fhir.cr.group;

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
import org.opencds.cqf.fhir.cr.group.evaluate.EvaluateRequest;
import org.opencds.cqf.fhir.cr.group.evaluate.IEvaluateProcessor;
import org.opencds.cqf.fhir.cr.group.r4.EvaluateProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class GroupProcessor {
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

    public GroupProcessor(IRepository repository) {
        this(repository, CrSettings.getDefault());
    }

    public GroupProcessor(IRepository repository, CrSettings crSettings) {
        this(repository, crSettings, null);
    }

    public GroupProcessor(
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

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveGroup(Either3<C, IIdType, R> group) {
        return new ResourceResolver("Group", repository).resolve(group);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageGroup(
            Either3<C, IIdType, R> group) {
        return packageGroup(group, false);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageGroup(
            Either3<C, IIdType, R> group, boolean isPut) {
        return packageGroup(group, packageParameters(fhirVersion, null, isPut));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageGroup(
            Either3<C, IIdType, R> group, IBaseParameters parameters) {
        return packageGroup(resolveGroup(group), parameters);
    }

    public IBaseBundle packageGroup(IBaseResource group, IBaseParameters parameters) {
        var processor = packageProcessor != null ? packageProcessor : new PackageProcessor(repository, crSettings);
        return processor.packageResource(group, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle releaseGroup(
            Either3<C, IIdType, R> group) {
        return releaseGroup(group, false);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle releaseGroup(
            Either3<C, IIdType, R> group, boolean isPut) {
        return releaseGroup(group, packageParameters(fhirVersion, null, isPut));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle releaseGroup(
            Either3<C, IIdType, R> group, IBaseParameters parameters) {
        return releaseGroup(resolveGroup(group), parameters);
    }

    public IBaseBundle releaseGroup(IBaseResource group, IBaseParameters parameters) {
        var processor = releaseProcessor != null
                ? releaseProcessor
                : new ReleaseProcessor(repository, crSettings.getTerminologyServerClientSettings());
        return processor.releaseResource(group, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<C, IIdType, R> group, IBaseParameters parameters) {
        return dataRequirements(resolveGroup(group), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource group, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository, crSettings.getEvaluationSettings());
        return processor.getDataRequirements(group, parameters);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> EvaluateRequest buildEvaluateRequest(
            Either3<C, IIdType, R> group,
            String subject,
            IBaseParameters parameters,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine) {
        return new EvaluateRequest(
                resolveGroup(group),
                StringUtils.isBlank(subject) ? null : Ids.newId(fhirVersion, subject),
                parameters,
                data,
                prefetchData,
                libraryEngine,
                modelResolver);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R evaluate(
            Either3<C, IIdType, R> group,
            String subject,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return evaluate(
                group,
                subject,
                parameters,
                useServerData,
                data,
                prefetchData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R evaluate(
            Either3<C, IIdType, R> group,
            String subject,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            IRepository dataRepository,
            IRepository contentRepository,
            IRepository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return evaluate(
                group,
                subject,
                parameters,
                data,
                prefetchData,
                new LibraryEngine(repository, crSettings.getEvaluationSettings()));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R evaluate(
            Either3<C, IIdType, R> group,
            String subject,
            IBaseParameters parameters,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine) {
        var processor = evaluateProcessor != null
                ? evaluateProcessor
                : new EvaluateProcessor(repository, crSettings.getEvaluationSettings());
        return processor.evaluate(buildEvaluateRequest(group, subject, parameters, data, prefetchData, libraryEngine));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle deleteGroup(
            Either3<C, IIdType, R> group, IBaseParameters parameters) {
        var processor = deleteProcessor != null ? deleteProcessor : new DeleteProcessor(repository);
        return processor.deleteResource(resolveGroup(group), parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle retireGroup(
            Either3<C, IIdType, R> group, IBaseParameters parameters) {
        var processor = retireProcessor != null ? retireProcessor : new RetireProcessor(repository);
        return processor.retireResource(resolveGroup(group), parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle withdrawGroup(
            Either3<C, IIdType, R> group, IBaseParameters parameters) {
        var processor = withdrawProcessor != null ? withdrawProcessor : new WithdrawProcessor(repository);
        return processor.withdrawResource(resolveGroup(group), parameters);
    }

    public IBaseResource reviseGroup(IBaseResource resource) {
        var processor = reviseProcessor != null ? reviseProcessor : new ReviseProcessor(repository);
        return processor.reviseResource(resource);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters artifactDiff(
            Either3<C, IIdType, R> sourceGroup,
            Either3<C, IIdType, R> targetGroup,
            Boolean compareComputable,
            Boolean compareExecutable,
            IBaseResource terminologyEndpoint) {
        var processor = artifactDiffProcessor != null ? artifactDiffProcessor : new ArtifactDiffProcessor();
        return processor.getArtifactDiff(
                resolveGroup(sourceGroup),
                resolveGroup(targetGroup),
                compareComputable,
                compareExecutable,
                null,
                terminologyEndpoint);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource createChangelog(
            Either3<C, IIdType, R> sourceGroup, Either3<C, IIdType, R> targetGroup, IBaseResource terminologyEndpoint) {
        var processor =
                createChangelogProcessor != null ? createChangelogProcessor : new CreateChangelogProcessor(repository);
        return processor.createChangelog(resolveGroup(sourceGroup), resolveGroup(targetGroup), terminologyEndpoint);
    }
}
