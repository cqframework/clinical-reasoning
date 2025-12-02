package org.opencds.cqf.fhir.cr.questionnaire.populate;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.utility.adapter.r4.QuestionnaireResponseItemAnswerComponentAdapter;
import org.opencds.cqf.fhir.utility.repository.INpmRepository;

@ExtendWith(MockitoExtension.class)
public class NpmBackedRepoTest {

    @Mock
    private IRepository repository;

    @Mock
    private INpmRepository npmRepository;

    @Spy
    private FhirContext fhirCtx = FhirContext.forR4Cached();

    @InjectMocks
    private NpmBackedRepository npmBackedRepo;

    @Test
    public void search_forAppropriateResources_works() {
        // setup
        ContactPoint value = new ContactPoint();
        value.setSystem(ContactPointSystem.FAX);
        value.setValue("+1 (123) 4567890");

        new QuestionnaireResponseItemAnswerComponent().setValue((Type) value);
    }
}
