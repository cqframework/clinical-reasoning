package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.utility.CqfExpression;

@ExtendWith(MockitoExtension.class)
class ExpressionProcessorTests {
    @Mock
    IRepository repository;

    @Mock
    private LibraryEngine libraryEngine;

    @Spy
    @InjectMocks
    private ExpressionProcessor fixture;

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
    }

    @Test
    void getExpressionResultShouldReturnEmptyListForNullExpression() {
        var request =
                RequestHelpers.newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, new Questionnaire());
        var result = fixture.getExpressionResult(request, null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getExpressionResultShouldReturnEmptyListForNullExpressionResult() {
        var questionnaire = new Questionnaire();
        var request = RequestHelpers.newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var expression = new CqfExpression();
        doReturn(null).when(libraryEngine).resolveExpression(any(), any(), any(), any(), any(), any(), any());
        var result = fixture.getExpressionResult(request, expression);
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
