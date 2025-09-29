package org.opencds.cqf.fhir.cr.implementationguide;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.IReleaseProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ReleaseProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.library.evaluate.IEvaluateProcessor;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class ImplementationGuideProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected IPackageProcessor packageProcessor;
    protected IReleaseProcessor releaseProcessor;
    protected IRepository repository;
    protected TerminologyServerClientSettings terminologyServerClientSettings;

    public ImplementationGuideProcessor(IRepository repository) {
        this(repository, EvaluationSettings.getDefault(), TerminologyServerClientSettings.getDefault());
    }

    public ImplementationGuideProcessor(
            IRepository repository,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings) {
        this(repository, evaluationSettings, terminologyServerClientSettings, null, null, null, null);
    }

    public ImplementationGuideProcessor(
            IRepository repository,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings,
            IPackageProcessor packageProcessor,
            IReleaseProcessor releaseProcessor,
            IDataRequirementsProcessor dataRequirementsProcessor,
            IEvaluateProcessor evaluateProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.terminologyServerClientSettings = requireNonNull(terminologyServerClientSettings,
                "terminologyServerClientSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.packageProcessor = packageProcessor;
        this.releaseProcessor = releaseProcessor;
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
        var processor = packageProcessor != null
                ? packageProcessor
                : new PackageProcessor(repository, terminologyServerClientSettings);
        return processor.packageResource(implementationGuide, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle releaseImplementationGuide(
            Either3<C, IIdType, R> implementationGuide, IBaseParameters parameters) {
        return releaseImplementationGuide(resolveImplementationGuide(implementationGuide), parameters);
    }

    public IBaseBundle releaseImplementationGuide(IBaseResource implementationGuide, IBaseParameters parameters) {
        var processor = releaseProcessor != null
                ? releaseProcessor
                : new ReleaseProcessor(repository, terminologyServerClientSettings);
        return processor.releaseResource(implementationGuide, parameters);
    }
}
