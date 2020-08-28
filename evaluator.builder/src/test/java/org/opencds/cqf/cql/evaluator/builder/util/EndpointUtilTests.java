package org.opencds.cqf.cql.evaluator.builder.util;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.opencds.cqf.cql.evaluator.builder.api.model.EndpointInfo;
import org.testng.annotations.Test;

public class EndpointUtilTests {

    @Test
    public void TestR4Endpoint() {

        org.hl7.fhir.r4.model.Endpoint endpoint = new org.hl7.fhir.r4.model.Endpoint();
        endpoint.setAddress("http://localhost");
        endpoint.setConnectionType(new org.hl7.fhir.r4.model.Coding().setCode("hl7-cql-files"));
        endpoint.setHeader(Collections.singletonList(new org.hl7.fhir.r4.model.StringType("X-Test-Header: Header")));

        EndpointInfo expected = new EndpointInfo()
            .setType(new org.hl7.fhir.r4.model.Coding().setCode("hl7-cql-files"))
            .setAddress("http://localhost")
            .setHeaders(Collections.singletonList("X-Test-Header: Header"));

        EndpointInfo actual = EndpointUtil.getEndpointInfo(endpoint);

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getHeaders().get(0), actual.getHeaders().get(0));
        assertEquals(expected.getType().getCode(), actual.getType().getCode());

        endpoint = new org.hl7.fhir.r4.model.Endpoint();
        endpoint.setAddress("file://path");


        expected = new EndpointInfo()
            .setAddress("file://path");

        actual = EndpointUtil.getEndpointInfo(endpoint);

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getHeaders(), actual.getHeaders());
        assertEquals(expected.getType(), actual.getType());
    }
    
    @Test
    public void TestDstu3Endpoint() {

        org.hl7.fhir.dstu3.model.Endpoint endpoint = new org.hl7.fhir.dstu3.model.Endpoint();
        endpoint.setAddress("http://localhost");
        endpoint.setConnectionType(new org.hl7.fhir.dstu3.model.Coding().setCode("hl7-cql-files"));
        endpoint.setHeader(Collections.singletonList(new org.hl7.fhir.dstu3.model.StringType("X-Test-Header: Header")));

        EndpointInfo expected = new EndpointInfo()
            .setType(new org.hl7.fhir.dstu3.model.Coding().setCode("hl7-cql-files"))
            .setAddress("http://localhost")
            .setHeaders(Collections.singletonList("X-Test-Header: Header"));

        EndpointInfo actual = EndpointUtil.getEndpointInfo(endpoint);

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getHeaders().get(0), actual.getHeaders().get(0));
        assertEquals(expected.getType().getCode(), actual.getType().getCode());

        endpoint = new org.hl7.fhir.dstu3.model.Endpoint();
        endpoint.setAddress("file://path");

        expected = new EndpointInfo()
                .setAddress("file://path");


        actual = EndpointUtil.getEndpointInfo(endpoint);

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getHeaders(), actual.getHeaders());
        assertEquals(expected.getType(), actual.getType());
    }
    
}