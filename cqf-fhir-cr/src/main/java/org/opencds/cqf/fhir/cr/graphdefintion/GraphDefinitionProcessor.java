package org.opencds.cqf.fhir.cr.graphdefintion;

import static java.util.Objects.isNull;
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
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.graphdefintion.apply.ApplyProcessor;
import org.opencds.cqf.fhir.cr.graphdefintion.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.graphdefintion.apply.IApplyProcessor;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

@SuppressWarnings("UnstableApiUsage")
public class GraphDefinitionProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected IApplyProcessor applyProcessor;
    protected IPackageProcessor packageProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected IRepository repository;
    protected EvaluationSettings evaluationSettings;
    protected TerminologyServerClientSettings terminologyServerClientSettings;

    public GraphDefinitionProcessor(IRepository repository) {
        this(repository, EvaluationSettings.getDefault(), new TerminologyServerClientSettings());
    }

    public GraphDefinitionProcessor(
            IRepository repository,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings) {
        this(repository, evaluationSettings, terminologyServerClientSettings, null, null, null);
    }

    public GraphDefinitionProcessor(
            IRepository repository,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings,
            IApplyProcessor applyProcessor,
            IPackageProcessor packageProcessor,
            IDataRequirementsProcessor dataRequirementsProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        if (packageProcessor == null) {
            this.terminologyServerClientSettings =
                    requireNonNull(terminologyServerClientSettings, "terminologyServerClientSettings can not be null");
        }
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.packageProcessor = packageProcessor;
        this.dataRequirementsProcessor = dataRequirementsProcessor;
        this.applyProcessor = applyProcessor;

    }

    public EvaluationSettings evaluationSettings() {
        return evaluationSettings;
    }

    public IApplyProcessor getApplyProcessor() {
        if (isNull(this.applyProcessor)) {
            applyProcessor = new ApplyProcessor(repository, modelResolver);

        }
        return applyProcessor;
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveGraphDefinition(
            Either3<C, IIdType, R> graphDefinition) {
        return new ResourceResolver("GraphDefinition", repository).resolve(graphDefinition);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageGraphDefinition(
            Either3<C, IIdType, R> graphDefinition) {
        return packageGraphDefinition(graphDefinition, false);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageGraphDefinition(
            Either3<C, IIdType, R> graphDefinition, boolean isPut) {
        return packageGraphDefinition(graphDefinition, packageParameters(fhirVersion, null, isPut));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageGraphDefinition(
            Either3<C, IIdType, R> graphDefinition, IBaseParameters parameters) {
        return packageGraphDefinition(resolveGraphDefinition(graphDefinition), parameters);
    }

    public IBaseBundle packageGraphDefinition(IBaseResource graphDefinition, IBaseParameters parameters) {
        var processor = packageProcessor != null
                ? packageProcessor
                : new PackageProcessor(repository, terminologyServerClientSettings);
        return processor.packageResource(graphDefinition, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<C, IIdType, R> graphDefinition, IBaseParameters parameters) {
        return dataRequirements(resolveGraphDefinition(graphDefinition), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource graphDefinition, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository, evaluationSettings);
        return processor.getDataRequirements(graphDefinition, parameters);
    }

    public IBaseResource apply(ApplyRequest applyRequest) {
        return getApplyProcessor().apply(applyRequest);
    }
}
