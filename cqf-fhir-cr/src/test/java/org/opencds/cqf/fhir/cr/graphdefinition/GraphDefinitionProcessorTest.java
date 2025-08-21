package org.opencds.cqf.fhir.cr.graphdefinition;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.time.ZonedDateTime;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.graphdefintion.GraphDefinitionProcessor;
import org.opencds.cqf.fhir.utility.monad.Eithers;

@SuppressWarnings("UnstableApiUsage")
public class GraphDefinitionProcessorTest {

    @Test
    void testApply_returnsParametersResource() {
        IRepository repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        GraphDefinitionProcessor processor = new GraphDefinitionProcessor(repository);

        IIdType graphId = mock(IIdType.class);
        Parameters inputParams = new Parameters();

        IBaseParameters result = processor.apply(
                Eithers.forMiddle3(graphId),
                "Patient/123",
                ZonedDateTime.now().minusDays(1),
                ZonedDateTime.now(),
                inputParams);

        assertNotNull(result);
        assertTrue(result instanceof Parameters);

        Parameters parameters = (Parameters) result;
        assertTrue(parameters.getParameter().size() > 0);

        Object firstParameter = parameters.getParameter().get(0);
        assertTrue(firstParameter instanceof Object);

    }
}
