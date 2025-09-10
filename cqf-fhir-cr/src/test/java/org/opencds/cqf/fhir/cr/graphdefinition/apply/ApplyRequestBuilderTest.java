package org.opencds.cqf.fhir.cr.graphdefinition.apply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@ExtendWith(MockitoExtension.class)
class ApplyRequestBuilderTest {

    @Mock
    private IRepository repository;

    private EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

    private FhirContext fhirContext = FhirContext.forR4Cached();

    @Test
    void build_withoutSubject_throwsIllegalArgumentException() {
        when(repository.fhirContext()).thenReturn(fhirContext);

        ApplyRequestBuilder builder = new ApplyRequestBuilder(repository, evaluationSettings);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::buildApplyRequest);
        assertEquals("Missing required parameter: 'subject'", ex.getMessage());
    }

    @Test
    void build_withoutPractitioner_throwsIllegalArgumentException() {
        when(repository.fhirContext()).thenReturn(fhirContext);

        ApplyRequestBuilder builder =
                new ApplyRequestBuilder(repository, evaluationSettings).withSubject("Patient/123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::buildApplyRequest);
        assertEquals("Missing required parameter: 'practitioner'", ex.getMessage());
    }

    @Test
    void build_withGraphDefinitionAndSubject_returnsApplyRequest() {
        IRepository localRepository = new InMemoryFhirRepository(fhirContext);
        IdType id = (IdType) localRepository.create(new GraphDefinition()).getId();

        ApplyRequestBuilder builder = new ApplyRequestBuilder(localRepository, evaluationSettings)
                .withGraphDefinitionId(id)
                .withSubject("Patient/123")
                .withPractitioner("Practitioner/456")
                .withUseLocalData(true);

        ApplyRequest request = builder.buildApplyRequest();

        assertThat(request).isNotNull();
        LibraryEngine libraryEngine = request.getLibraryEngine();

        assertThat(libraryEngine).isNotNull();
        assertThat(libraryEngine.getRepository()).isEqualTo(localRepository);
        assertThat(libraryEngine.getSettings()).isEqualTo(evaluationSettings);

        assertThat(request.getModelResolver()).isNotNull();
        assertThat(request.getGraphDefinition().getStructureFhirVersionEnum())
                .isEqualTo(fhirContext.getVersion().getVersion());
    }
}
