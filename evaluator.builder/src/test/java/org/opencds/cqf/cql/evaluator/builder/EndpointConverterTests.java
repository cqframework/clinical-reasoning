package org.opencds.cqf.cql.evaluator.builder;

import static org.testng.Assert.assertEquals;

import java.util.Collections;

import org.testng.annotations.Test;

public class EndpointConverterTests {

    @Test
    public void TestR4Endpoint() {

        EndpointConverter endpointConverter = new EndpointConverter(new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory());

        org.hl7.fhir.r4.model.Endpoint endpoint = new org.hl7.fhir.r4.model.Endpoint();
        endpoint.setAddress("http://localhost");
        endpoint.setConnectionType(new org.hl7.fhir.r4.model.Coding().setCode("hl7-cql-files"));
        endpoint.setHeader(Collections.singletonList(new org.hl7.fhir.r4.model.StringType("X-Test-Header: Header")));

        EndpointInfo expected = new EndpointInfo()
            .setType(new org.hl7.fhir.r4.model.Coding().setCode("hl7-cql-files"))
            .setAddress("http://localhost")
            .setHeaders(Collections.singletonList("X-Test-Header: Header"));

        EndpointInfo actual = endpointConverter.getEndpointInfo(endpoint); 

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getHeaders().get(0), actual.getHeaders().get(0));
        assertEquals(expected.getType().getCode(), actual.getType().getCode());

        endpoint = new org.hl7.fhir.r4.model.Endpoint();
        endpoint.setAddress("file://path");


        expected = new EndpointInfo()
            .setAddress("file://path");

        actual = endpointConverter.getEndpointInfo(endpoint);

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getHeaders(), actual.getHeaders());
        assertEquals(expected.getType(), actual.getType());
    }
    
    @Test
    public void TestDstu3Endpoint() {

        EndpointConverter endpointConverter = new EndpointConverter(new org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory());

        org.hl7.fhir.dstu3.model.Endpoint endpoint = new org.hl7.fhir.dstu3.model.Endpoint();
        endpoint.setAddress("http://localhost");
        endpoint.setConnectionType(new org.hl7.fhir.dstu3.model.Coding().setCode("hl7-cql-files"));
        endpoint.setHeader(Collections.singletonList(new org.hl7.fhir.dstu3.model.StringType("X-Test-Header: Header")));

        EndpointInfo expected = new EndpointInfo()
            .setType(new org.hl7.fhir.dstu3.model.Coding().setCode("hl7-cql-files"))
            .setAddress("http://localhost")
            .setHeaders(Collections.singletonList("X-Test-Header: Header"));

        EndpointInfo actual = endpointConverter.getEndpointInfo(endpoint);

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getHeaders().get(0), actual.getHeaders().get(0));
        assertEquals(expected.getType().getCode(), actual.getType().getCode());

        endpoint = new org.hl7.fhir.dstu3.model.Endpoint();
        endpoint.setAddress("file://path");

        expected = new EndpointInfo()
                .setAddress("file://path");


        actual = endpointConverter.getEndpointInfo(endpoint);

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getHeaders(), actual.getHeaders());
        assertEquals(expected.getType(), actual.getType());
    }
    
}