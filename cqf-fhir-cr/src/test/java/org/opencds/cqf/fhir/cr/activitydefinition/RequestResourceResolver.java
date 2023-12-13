package org.opencds.cqf.fhir.cr.activitydefinition;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;

public class RequestResourceResolver {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/activitydefinition";

    public static class Given {
        private Repository repository;
        private IIdType activityDefinitionId;
        private Class<? extends IBaseResource> activityDefinitionClass;
        private Class<? extends BaseRequestResourceResolver> resolverClass;

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = TestRepositoryFactory.createRepository(
                    fhirContext, this.getClass(), CLASS_PATH + "/" + repositoryPath, IGLayoutMode.TYPE_PREFIX);
            return this;
        }

        public Given resolverClasses(
                Class<? extends BaseRequestResourceResolver> resolverClass,
                Class<? extends IBaseResource> activityDefinitionClass) {
            this.resolverClass = resolverClass;
            this.activityDefinitionClass = activityDefinitionClass;
            return this;
        }

        public Given activityDefinition(String activityDefinitionId) {
            this.activityDefinitionId = Ids.newId(activityDefinitionClass, activityDefinitionId);
            return this;
        }

        private BaseRequestResourceResolver buildResolver() {
            try {
                var activityDefinition = repository.read(activityDefinitionClass, activityDefinitionId);
                return resolverClass.getConstructor(activityDefinitionClass).newInstance(activityDefinition);
            } catch (Exception e) {
                return null;
            }
        }

        public When when() {
            return new When(buildResolver());
        }
    }

    public static class When {
        private final BaseRequestResourceResolver resolver;
        private IIdType subjectId;
        private IIdType encounterId;
        private IIdType practitionerId;
        private IIdType organizationId;

        When(BaseRequestResourceResolver resolver) {
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
            return resolver.resolve(subjectId, encounterId, practitionerId, organizationId);
        }
    }
}
