package org.opencds.cqf.cql.evaluator.plandefinition.r5;

import static org.opencds.cqf.cql.evaluator.fhir.util.r5.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r5.Parameters.stringPart;

import java.util.List;

import org.hl7.fhir.r5.model.Parameters;
import org.opencds.cqf.cql.evaluator.fhir.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.Repositories;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class PlanDefinitionProcessorTests {

  private final FhirContext fhirContext = FhirContext.forR5Cached();

  @Test()
  public void testChildRoutineVisit() {
    PlanDefinition.Assert
        .that("ChildRoutineVisit-PlanDefinition-1.0.0", "Patient/ChildRoutine-Reportable", null)
        .withAdditionalData("child-routine-visit/child_routine_visit_patient.json")
        .withContent("child-routine-visit/child_routine_visit_plan_definition.json").apply()
        .isEqualsTo("child-routine-visit/child_routine_visit_bundle.json");
  }

  @Test
  public void testHelloWorld() {
    PlanDefinition.Assert
        .that("hello-world-patient-view", "helloworld-patient-1",
            "helloworld-patient-1-encounter-1")
        .withAdditionalData("hello-world/hello-world-patient-data.json")
        .withContent("hello-world/hello-world-patient-view-bundle.json").apply()
        .isEqualsTo("hello-world/hello-world-bundle.json");
  }

  // Disabling this test because the current resources are using R4
  @Test(enabled = false)
  public void testOpioidRec10PatientView() {
    var data =
        new InMemoryFhirRepository(fhirContext, this.getClass(),
            List.of("opioid-Rec10-patient-view/tests"),
            false);
    var content =
        new InMemoryFhirRepository(fhirContext, this.getClass(),
            List.of("opioid-Rec10-patient-view/content"), false);
    var terminology = new InMemoryFhirRepository(fhirContext, this.getClass(),
        List.of("opioid-Rec10-patient-view/vocabulary/CodeSystem",
            "opioid-Rec10-patient-view/vocabulary/ValueSet"),
        false);
    var repository = Repositories.proxy(data, content, terminology);
    PlanDefinition.Assert
        .that("opioidcds-10-patient-view", "example-rec-10-patient-view-POS-Cocaine-drugs",
            "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch")
        .withRepository(repository).apply()
        .isEqualsTo("opioid-Rec10-patient-view/tests/Bundle-opioid-Rec10-patient-view.json");
  }

  @Test(enabled = false)
  public void testRuleFiltersNotReportable() {
    PlanDefinition.Assert.that("plandefinition-RuleFilters-1.0.0", "NotReportable", null)
        .withAdditionalData("rule-filters/tests-NotReportable-bundle.json")
        .withContent("rule-filters/RuleFilters-1.0.0-bundle.json").apply()
        .isEqualsTo("rule-filters/NotReportableBundle.json");
  }

  @Test(enabled = false)
  public void testRuleFiltersReportable() {
    PlanDefinition.Assert.that("plandefinition-RuleFilters-1.0.0", "Reportable", null)
        .withAdditionalData("rule-filters/tests-Reportable-bundle.json")
        .withContent("rule-filters/RuleFilters-1.0.0-bundle.json").apply()
        .isEqualsTo("rule-filters/ReportableBundle.json");
  }

  @Test(enabled = false) // Need valid r5 content for this test
  public void testQuestionnairePrepopulate() {
    PlanDefinition.Assert.that("prepopulate", "OPA-Patient1", null)
        .withAdditionalData("prepopulate/prepopulate-patient-data.json")
        .withContent("prepopulate/prepopulate-content-bundle.json")
        .withParameters(new Parameters().addParameter("ClaimId", "OPA-Claim1")).apply()
        .isEqualsTo("prepopulate/prepopulate-bundle.json");
  }

  @Test(enabled = false) // Need valid r5 content for this test
  public void testQuestionnairePrepopulate_NoLibrary() {
    PlanDefinition.Assert.that("prepopulate", "OPA-Patient1", null)
        .withAdditionalData("prepopulate/prepopulate-patient-data.json")
        .withContent("prepopulate/prepopulate-content-bundle-noLibrary.json")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).apply()
        .isEqualsTo("prepopulate/prepopulate-bundle-noLibrary.json");
  }

  @Test
  public void testQuestionnaireResponse() {
    PlanDefinition.Assert.that("prepopulate", "OPA-Patient1", null)
        .withAdditionalData("extract-questionnaireresponse/patient-data.json")
        .withContent("prepopulate/prepopulate-content-bundle.json")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).apply()
        .hasContained(3);
  }

  @Test(enabled = false) // Need valid r5 content for this test
  public void testGenerateQuestionnaire() {
    PlanDefinition.Assert.that("generate-questionnaire", "OPA-Patient1", null)
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).apply()
        .isEqualsTo("tests/Bundle-generate-questionnaire.json");
  }
}
