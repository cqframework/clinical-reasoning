package org.opencds.cqf.fhir.cr.valueset;

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
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.utility.monad.Either3;

@SuppressWarnings("UnstableApiUsage")
public class ValueSetProcessor {
    protected final FhirVersionEnum fhirVersion;
    protected IPackageProcessor packageProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected IRepository repository;
    protected CrSettings crSettings;

    public ValueSetProcessor(IRepository repository) {
        this(repository, CrSettings.getDefault());
    }

    public ValueSetProcessor(IRepository repository, CrSettings crSettings) {
        this(repository, crSettings, null);
    }

    public ValueSetProcessor(
            IRepository repository, CrSettings crSettings, List<? extends IOperationProcessor> operationProcessors) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.crSettings = requireNonNull(crSettings, "crSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        if (operationProcessors != null && !operationProcessors.isEmpty()) {
            operationProcessors.forEach(p -> {
                if (p instanceof IPackageProcessor pack) {
                    packageProcessor = pack;
                }
                if (p instanceof IDataRequirementsProcessor dataReq) {
                    dataRequirementsProcessor = dataReq;
                }
            });
        }
    }

    public CrSettings settings() {
        return crSettings;
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
        var processor = packageProcessor != null ? packageProcessor : new PackageProcessor(repository, crSettings);
        return processor.packageResource(valueSet, parameters);
    }

    public <T extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<T, IIdType, R> valueSet, IBaseParameters parameters) {
        return dataRequirements(resolveValueSet(valueSet), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource valueSet, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository, crSettings.getEvaluationSettings());
        return processor.getDataRequirements(valueSet, parameters);
    }
}
