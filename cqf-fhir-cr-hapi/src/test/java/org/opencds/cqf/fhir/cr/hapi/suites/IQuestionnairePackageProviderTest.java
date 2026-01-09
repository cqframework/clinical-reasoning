package org.opencds.cqf.fhir.cr.hapi.suites;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public interface IQuestionnairePackageProviderTest<T> {

    IBaseBackboneElement terminologyEndpointFromString(String val);



    @ParameterizedTest
    @CsvSource(
        // id, canonical, url, version, terminologyEndpoint, usePut
        value = """
        Questionnaire/1,null,null,null,null,null,
        
        """, nullValues = "null"
    )
    default void packageQuestionnaire_parameterTest_coverage(String id, String canonical, String url, String version, String terminologyEndpoint, Boolean usePut) {

    }
}
