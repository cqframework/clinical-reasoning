package org.opencds.cqf.fhir.cr.visitor.dstu3;

import static org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.dstu3.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.SearchParameter;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.opencds.cqf.fhir.cr.visitor.IWithdrawVisitorTest;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

public class WithdrawVisitorTest implements IWithdrawVisitorTest {

    private final FhirContext fhirContext = FhirContext.forDstu3Cached();

    private final AdapterFactory factory = new AdapterFactory();
    private IRepository repo;

    private final IParser jsonParser = fhirContext.newJsonParser();

    @BeforeEach
    public void setup() {
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
