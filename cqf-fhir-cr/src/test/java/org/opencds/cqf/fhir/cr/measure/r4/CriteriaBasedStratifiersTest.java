package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.SelectedReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CriteriaBasedStratifiersTest {

    private static final Logger logger = LoggerFactory.getLogger(CriteriaBasedStratifiersTest.class);

    private static final Given GIVEN_REPO = Measure.given().repositoryFor("CriteriaBasedStratifiers");

    @Test
    void singlePatientSingleEncounter() {
        final SelectedReport selectedReport = GIVEN_REPO
                .when()
                .measureId("CriteriaBasedStratifiers")
                .subject("Patient/patient1")
                .evaluate()
                .then();

        final IParser jsonParser = FhirContext.forR4Cached().newJsonParser();

        final String serialized = jsonParser.setPrettyPrint(true).encodeResourceToString(selectedReport.report());

        logger.info(serialized);

        selectedReport
                .firstGroup()
                .population("initial-population")
                .hasCount(4)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                // LUKETODO:  what do we do about this?
                //                .hasCode("in-progress encounters")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasCount(1);
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

    @Test
    void allPatientsTwoEncounters() {
        final SelectedReport selectedReport = GIVEN_REPO
                .when()
                .measureId("CriteriaBasedStratifiers")
                .evaluate()
                .then();

        final IParser jsonParser = FhirContext.forR4Cached().newJsonParser();

        final String serialized = jsonParser.setPrettyPrint(true).encodeResourceToString(selectedReport.report());

        logger.info(serialized);

        selectedReport
                .firstGroup()
                .population("initial-population")
                .hasCount(9)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                //                .hasCode("in-progress encounters")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasCount(2);
    }

    /*
        {
      "resourceType": "MeasureReport",
      "status": "complete",
      "type": "summary",
      "measure": "http://example.com/Measure/CriteriaBasedStratifiers",
      "date": "2025-10-02T10:04:56-04:00",
      "group": [ {
        "id": "group-1",
        "population": [ {
          "id": "initial-population",
          "code": {
            "coding": [ {
              "system": "http://terminology.hl7.org/CodeSystem/measure-population",
              "code": "initial-population",
              "display": "Initial Population"
            } ]
          },
          "count": 8
        } ],
        "stratifier": [ {
          "id": "stratifier-1",
          "stratum": [ {
            "value": {
              "text": "Encounter/enc_in_progress_pat1_1"
            },
            "population": [ {
              "id": "initial-population",
              "code": {
                "coding": [ {
                  "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                  "code": "initial-population",
                  "display": "Initial Population"
                } ]
              },
              "count": 1
            } ]
          }, {
            "value": {
              "text": "Encounter/enc_in_progress_pat2_1"
            },
            "population": [ {
              "id": "initial-population",
              "code": {
                "coding": [ {
                  "system": "http://terminology.hl7.org/CodeSystem/measure-population",
                  "code": "initial-population",
                  "display": "Initial Population"
                } ]
              },
              "count": 1
            } ]
          } ]
        } ]
      } ]
    }
         */
}
