package org.opencds.cqf.fhir.cr.library;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class LibraryProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected final IPackageProcessor packageProcessor;
    protected Repository repository;
    protected EvaluationSettings evaluationSettings;

    public LibraryProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public LibraryProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null);
    }

    public LibraryProcessor(
            Repository repository, EvaluationSettings evaluationSettings, IPackageProcessor packageProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.packageProcessor = packageProcessor != null ? packageProcessor : new PackageProcessor(this.repository);
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
        return packageProcessor.packageResource(resolveLibrary(library));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packageLibrary(
            Either3<C, IIdType, R> library, boolean isPut) {
        return packageProcessor.packageResource(resolveLibrary(library), isPut ? "PUT" : "POST");
    }
}
