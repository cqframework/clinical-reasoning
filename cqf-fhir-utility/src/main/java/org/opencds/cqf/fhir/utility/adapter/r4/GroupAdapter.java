package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IGroupAdapter;

public class GroupAdapter implements IGroupAdapter {

    @Override
    public IDomainResource get() {
        return null;
    }

    @Override
    public FhirContext fhirContext() {
        return null;
    }

    @Override
    public ModelResolver getModelResolver() {
        return null;
    }
}
