package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import org.testng.annotations.Test;

import static org.testng.Assert.assertThrows;

public class QuestionnaireProcessorTests {
    @Test
    void testPrePopulate() {
        TestQuestionnaire.Assert.that("questionnaire-for-order.json")
                .withData("o2_peter_bundle.json")
                .withLibrary("outpatientPA.json")
                .prePopulate()
                .isEqualsTo("questionnaire-for-order-populated.json");
    }

    @Test
    void testPrePopulate_noQuestionnaire_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestQuestionnaire.Assert.that("")
                    .prePopulate();
        });
    }

    @Test
    void testPrePopulate_notQuestionnaire_throwsException() {
        assertThrows(ClassCastException.class, () -> {
            TestQuestionnaire.Assert.that("invalid-questionnaire.json")
                    .prePopulate();
        });
    }
}
