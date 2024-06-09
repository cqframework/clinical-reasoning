package org.opencds.cqf.fhir.cr.activitydefinition;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class RequestResourceResolver {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/activitydefinition";

    public static class Given {
        private IRequestResolverFactory resolverFactory;
        private Repository repository;
        private String activityDefinitionId;

        public Given repository(Repository repository) {
            this.repository = repository;
            this.resolverFactory = IRequestResolverFactory.getDefault(
                    repository.fhirContext().getVersion().getVersion());
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            this.resolverFactory =
                    IRequestResolverFactory.getDefault(fhirContext.getVersion().getVersion());
            return this;
        }

        public Given activityDefinition(String activityDefinitionId) {
            this.activityDefinitionId = activityDefinitionId;
            return this;
        }

        private BaseRequestResourceResolver buildResolver(IBaseResource activityDefinition) {
            return resolverFactory.create(activityDefinition);
        }

        public When when() {
            var activityDefinitionClass = repository
                    .fhirContext()
                    .getResourceDefinition("ActivityDefinition")
                    .getImplementingClass();
            var activityDefinition =
                    repository.read(activityDefinitionClass, Ids.newId(activityDefinitionClass, activityDefinitionId));
            return new When(repository, activityDefinition, buildResolver(activityDefinition));
        }
    }

    public static class When {
        private final Repository repository;
        private final IBaseResource activityDefinition;
        private final BaseRequestResourceResolver resolver;
        private IIdType subjectId;
        private IIdType encounterId;
        private IIdType practitionerId;
        private IIdType organizationId;

        When(Repository repository, IBaseResource activityDefinition, BaseRequestResourceResolver resolver) {
            this.repository = repository;
            this.activityDefinition = activityDefinition;
            this.resolver = resolver;
        }

        public When subjectId(IIdType subjectId) {
            this.subjectId = subjectId;
            return this;
        }

        public When encounterId(IIdType encounterId) {
            this.encounterId = encounterId;
            return this;
        }

        public When practitionerId(IIdType practitionerId) {
            this.practitionerId = practitionerId;
            return this;
        }

        public When organizationId(IIdType organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public IBaseResource resolve() {
            return resolver.resolve(new ApplyRequest(
                    activityDefinition,
                    subjectId,
                    encounterId,
                    practitionerId,
                    organizationId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    new LibraryEngine(repository, EvaluationSettings.getDefault()),
                    FhirModelResolverCache.resolverForVersion(
                            repository.fhirContext().getVersion().getVersion())));
        }
    }
}
