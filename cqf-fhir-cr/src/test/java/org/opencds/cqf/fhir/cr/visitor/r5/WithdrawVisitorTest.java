package org.opencds.cqf.fhir.cr.visitor.r5;

import static org.opencds.cqf.fhir.utility.r5.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r5.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.SearchParameter;
import org.junit.jupiter.api.BeforeEach;
import org.opencds.cqf.fhir.cr.visitor.IWithdrawVisitorTest;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WithdrawVisitorTest implements IWithdrawVisitorTest {

    private static final Logger log = LoggerFactory.getLogger(WithdrawVisitorTest.class);
    private final FhirContext fhirContext = FhirContext.forR5Cached();

    private final AdapterFactory factory = new AdapterFactory();
    private IRepository repo;

    private final IParser jsonParser = fhirContext.newJsonParser();

    @BeforeEach
    void setup() {
        SearchParameter sp = (SearchParameter)
                jsonParser.parseResource(getClass().getResourceAsStream("SearchParameter-artifactAssessment.json"));
        repo = new InMemoryFhirRepository(fhirContext);
        repo.update(sp);
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public IRepository getRepo() {
        return repo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T createFromResourceLocation(String resourceLocation) {
        return (T) jsonParser.parseResource(getClass().getResourceAsStream(resourceLocation));
    }

    @Override
    public Class<? extends IBaseResource> libraryClass() {
        return Library.class;
    }

    @Override
    public IAdapterFactory getAdapterFactory() {
        return factory;
    }

    @Override
    public IBaseParameters createParametersForWithdrawVisitor(String version) {
        return parameters(part("version", version));
    }
}
