package org.opencds.cqf.fhir.cr.activitydefinition.r4;

import ca.uhn.fhir.context.FhirContext;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;

public class RequestResourceResolver {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/activitydefinition/r4";

    public static class Given {
        private Repository repository;
        private Class<BaseRequestResourceResolver> clazz;

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(String repositoryPath) {
            this.repository = TestRepositoryFactory.createRepository(
                    FhirContext.forR4Cached(),
                    this.getClass(),
                    CLASS_PATH + "/" + repositoryPath,
                    IGLayoutMode.DIRECTORY);
            return this;
        }

        private BaseRequestResourceResolver buildResolver() {
            // return clazz.getConstructor(ActivityDefinition.class)
            //     .newInstance(activityDefinition);
            return null;
        }

        public When when() {
            return new When(buildResolver());
        }
    }

    public static class When {
        private final BaseRequestResourceResolver resolver;

        When(BaseRequestResourceResolver resolver) {
            this.resolver = resolver;
        }
    }
}
