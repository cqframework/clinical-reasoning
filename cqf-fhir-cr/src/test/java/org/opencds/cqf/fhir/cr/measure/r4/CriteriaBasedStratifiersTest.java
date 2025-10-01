package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class CriteriaBasedStratifiersTest {
    private static final Given GIVEN_REPO = Measure.given().repositoryFor("CriteriaBasedStratifiers");

    @Test
    void test() {
        GIVEN_REPO
                .when()
                .measureId("CriteriaBasedStratifiers")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(4)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCode("in-progress encounters")
                .hasStratumCount(1)
                .firstStratum()
                .hasComponentStratifierCount(1);
    }

    // LUKETODO:  test with multiple stratifiers and multiple strata and assert exception

    //      "group": [
    //    {
    //        "id": "group-1",
    //        "population": [
    //        {
    //            "code": {
    //            "coding": [
    //            {
    //                "system": "http://terminology.hl7.org/CodeSystem/measure-population",
    //                "code": "initial-population",
    //                "display": "Initial Population"
    //            }
    //            ]
    //        },
    //            "count": 2
    //        }
    //      ],
    //        "stratifier": [
    //        {
    //            "id": "stratifier-1",
    //            "code": [
    //            {
    //                "text": "in-progress encounters"
    //            }
    //          ],
    //            "stratum": [
    //            {
    //                "population": [
    //                {
    //                    "id": "stratifier-1-initial-population",
    //                    "extension": [
    //                    {
    //                        "url":
    // "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.population.description",
    //                        "valueString": "Initial Population"
    //                    }
    //                ],
    //                    "code": {
    //                    "coding": [
    //                    {
    //                        "system": "https://ncqa.org/fhir/CodeSystem/measure-group",
    //                        "code": "initial-population"
    //                    }
    //                  ]
    //                },
    //                    "count": 1
    //                }
    //            ]
    //            }
    //          ]
    //        }
    //        ]
    //    }
    //    ]
}
