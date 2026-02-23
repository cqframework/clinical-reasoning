package org.opencds.cqf.fhir.cr.implementationguide;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.IReleaseProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ReleaseProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

@SuppressWarnings("UnstableApiUsage")
public class ImplementationGuideProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected IPackageProcessor packageProcessor;
    protected IReleaseProcessor releaseProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected IRepository repository;
    protected CrSettings crSettings;

    public ImplementationGuideProcessor(IRepository repository) {
        this(repository, CrSettings.getDefault());
    }

    public ImplementationGuideProcessor(IRepository repository, CrSettings crSettings) {
        this(repository, crSettings, null);
    }

    public ImplementationGuideProcessor(
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
                if (p instanceof IReleaseProcessor release) {
                    releaseProcessor = release;
                }
                if (p instanceof IDataRequirementsProcessor dataReq) {
                    dataRequirementsProcessor = dataReq;
                }
            });
        }
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveImplementationGuide(
            Either3<C, IIdType, R> implementationGuide) {
        return new ResourceResolver("ImplementationGuide", repository).resolve(implementationGuide);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageImplementationGuide(
            Either3<C, IIdType, R> implementationGuide) {
        return packageImplementationGuide(implementationGuide, false);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageImplementationGuide(
            Either3<C, IIdType, R> implementationGuide, boolean isPut) {
        return packageImplementationGuide(implementationGuide, packageParameters(fhirVersion, null, isPut));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageImplementationGuide(
            Either3<C, IIdType, R> library, IBaseParameters parameters) {
        return packageImplementationGuide(resolveImplementationGuide(library), parameters);
    }

    public IBaseBundle packageImplementationGuide(IBaseResource implementationGuide, IBaseParameters parameters) {
        var processor = packageProcessor != null ? packageProcessor : new PackageProcessor(repository, crSettings);
        return processor.packageResource(implementationGuide, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<C, IIdType, R> implementationGuide, IBaseParameters parameters) {
        return dataRequirements(resolveImplementationGuide(implementationGuide), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource implementationGuide, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository, crSettings.getEvaluationSettings());
        return processor.getDataRequirements(implementationGuide, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle releaseImplementationGuide(
            Either3<C, IIdType, R> implementationGuide, IBaseParameters parameters) {
        return releaseImplementationGuide(resolveImplementationGuide(implementationGuide), parameters);
    }

    public IBaseBundle releaseImplementationGuide(IBaseResource implementationGuide, IBaseParameters parameters) {
        var processor = releaseProcessor != null
                ? releaseProcessor
                : new ReleaseProcessor(repository, crSettings.getTerminologyServerClientSettings());
        return processor.releaseResource(implementationGuide, parameters);
    }
}
