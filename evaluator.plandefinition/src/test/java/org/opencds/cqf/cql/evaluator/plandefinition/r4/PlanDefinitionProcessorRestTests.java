package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import org.testng.annotations.Test;

public class PlanDefinitionProcessorRestTests extends PlanDefinitionProcessorRestTestBase {

    @Test
    public void testColorectalCancerScreeningShouldScreen() {
        test(
                "cqf-ccc/tests-should-screen-ccs-bundle.json",
                "cqf-ccc/ColorectalCancerScreeningCDS-bundle.json",
                "ColorectalCancerScreeningCDS",
                "should-screen-ccs",
                null,
                "cqf-ccc/careplan-should-screen-ccs.json"
        );
    }

    @Test
    public void testColorectalCancerScreeningShouldNotScreen() {
        test(
                "cqf-ccc/tests-should-not-screen-ccs-bundle.json",
                "cqf-ccc/ColorectalCancerScreeningCDS-bundle.json",
                "ColorectalCancerScreeningCDS",
                "should-not-screen-ccs",
                null,
                "cqf-ccc/careplan-should-not-screen-ccs.json"
        );
    }
}