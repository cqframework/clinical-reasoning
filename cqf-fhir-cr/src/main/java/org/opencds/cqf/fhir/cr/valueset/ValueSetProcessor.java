package org.opencds.cqf.fhir.cr.valueset;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.Parameters.newBooleanPart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.Repository;
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
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class ValueSetProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected IPackageProcessor packageProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected Repository repository;
    protected EvaluationSettings evaluationSettings;

    public ValueSetProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public ValueSetProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null, null);
    }

    public ValueSetProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            IPackageProcessor packageProcessor,
            IDataRequirementsProcessor dataRequirementsProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
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
        return packageValueSet(
                valueSet,
                newParameters(
                        repository.fhirContext(),
                        "package-parameters",
                        newBooleanPart(repository.fhirContext(), "isPut", isPut)));
    }

    public <T extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageValueSet(
            Either3<T, IIdType, R> valueSet, IBaseParameters parameters) {
        return packageValueSet(resolveValueSet(valueSet), parameters);
    }

    public IBaseBundle packageValueSet(IBaseResource valueSet, IBaseParameters parameters) {
        var processor = packageProcessor != null ? packageProcessor : new PackageProcessor(repository);
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
