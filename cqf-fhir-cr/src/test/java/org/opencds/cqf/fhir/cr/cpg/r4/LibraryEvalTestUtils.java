package org.opencds.cqf.fhir.cr.cpg.r4;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class LibraryEvalTestUtils {

    public static void verifyNoErrors(Parameters result) {
        var operationOutcomes = result.getParameter()
            .stream()
            .map(ParametersParameterComponent::getResource)
            .filter(OperationOutcome.class::isInstance)
            .map(OperationOutcome.class::cast)
            .toList();

        var hasErrors = operationOutcomes.stream()
            .anyMatch(
                oo -> oo.getIssueFirstRep().getSeverity() == OperationOutcome.IssueSeverity.ERROR);

        assertFalse(
            hasErrors,
            () -> "OperationOutcome issues: "
                + operationOutcomes.stream()
                .map(OperationOutcome::getIssue)
                .flatMap(Collection::stream)
                .map(issue -> issue.getSeverity() + ": "
                    + issue.getDetails().getText())
                .reduce((first, second) -> first + "; " + second)
                .orElse("No issues found"));
    }
}
