package org.opencds.cqf.fhir.test;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

/**
 * @deprecated This class has been deprecated. Use InMemoryFhirRepository instead.
 */
@Deprecated
public class TestRepository extends InMemoryFhirRepository {

    public TestRepository(FhirContext context) {
        super(context);
    }

    // public TestRepository(FhirContext context, Class<?> clazz, List<String> directoryList, boolean recursive) {
    //     super(context, clazz, directoryList, recursive);
    // }

    public TestRepository(FhirContext fhirContext, IBaseBundle bundle) {
        super(fhirContext, bundle);
    }
}
