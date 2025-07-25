package org.opencds.cqf.fhir.cr.valueset;

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
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class ValueSetProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected IPackageProcessor packageProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected IRepository repository;
    protected EvaluationSettings evaluationSettings;
    protected TerminologyServerClientSettings terminologyServerClientSettings;

    public ValueSetProcessor(IRepository repository) {
        this(repository, EvaluationSettings.getDefault(), new TerminologyServerClientSettings());
    }

    public ValueSetProcessor(
            IRepository repository,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings) {
        this(repository, evaluationSettings, terminologyServerClientSettings, null, null);
    }

    public ValueSetProcessor(
            IRepository repository,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings,
            IPackageProcessor packageProcessor,
            IDataRequirementsProcessor dataRequirementsProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.terminologyServerClientSettings =
                requireNonNull(terminologyServerClientSettings, "terminologyServerClientSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.packageProcessor = packageProcessor;
        this.dataRequirementsProcessor = dataRequirementsProcessor;
    }

    public EvaluationSettings evaluationSettings() {
        return evaluationSettings;
    }

    protected <T extends IPrimitiveType<String>, R extends IBaseResource> R resolveValueSet(
            Either3<T, IIdType, R> valueSet) {
        return new ResourceResolver("ValueSet", repository).resolve(valueSet);
    }

    public <T extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageValueSet(
            Either3<T, IIdType, R> valueSet) {
        return packageValueSet(valueSet, false);
    }

    public <T extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageValueSet(
            Either3<T, IIdType, R> valueSet, boolean isPut) {
        return packageValueSet(valueSet, packageParameters(fhirVersion, null, isPut));
    }

    public <T extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageValueSet(
            Either3<T, IIdType, R> valueSet, IBaseParameters parameters) {
        return packageValueSet(resolveValueSet(valueSet), parameters);
    }

    public IBaseBundle packageValueSet(IBaseResource valueSet, IBaseParameters parameters) {
        var processor = packageProcessor != null
                ? packageProcessor
                : new PackageProcessor(repository, terminologyServerClientSettings);
        return processor.packageResource(valueSet, parameters);
    }

    public <T extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<T, IIdType, R> valueSet, IBaseParameters parameters) {
        return dataRequirements(resolveValueSet(valueSet), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource valueSet, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository, evaluationSettings);
        return processor.getDataRequirements(valueSet, parameters);
    }
}
