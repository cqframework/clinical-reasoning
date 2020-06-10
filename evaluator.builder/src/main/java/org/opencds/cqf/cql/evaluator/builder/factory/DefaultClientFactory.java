package org.opencds.cqf.cql.evaluator.builder.factory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

import org.apache.commons.lang3.NotImplementedException;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.engine.fhir.terminology.HeaderInjectionInterceptor;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;

public class DefaultClientFactory implements ClientFactory {

    /*
     * TODO - depending on future needs: 1. add OAuth 2. change if to switch to
     * accommodate additional FHIR versions
     */
    private static FhirContext fhirContext;

    @Override
    public IGenericClient create(URL url) throws IOException, InterruptedException, URISyntaxException {
        HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).followRedirects(Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20)).build();
        HttpRequest request = HttpRequest.newBuilder().uri(url.toURI().resolve("metadata"))
                .timeout(Duration.ofMinutes(2)).header("Accept-Type", "application/fhir+json").GET().build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        JsonReader jsonReader = Json.createReader(new StringReader(response.body()));
        JsonObject jsonObject = jsonReader.readObject();
        FhirVersionEnum fhirVersionEnum = ModelVersionHelper.forVersionString(jsonObject.getString("fhirVersion"));
        setFhirContextFromEnum(fhirVersionEnum);
        fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return fhirContext.newRestfulGenericClient(url.toURI().toString());
    }

    // Overload in case you need to specify a specific version of the context
    public IGenericClient getClient(org.hl7.fhir.dstu3.model.Endpoint endpoint) throws IOException,
            InterruptedException, URISyntaxException {
        IGenericClient client = create(new URL(endpoint.getAddress()));
        if (endpoint.hasHeader()) {
            List<String> headerList = endpoint.getHeader().stream().map(headerString -> headerString.asStringValue())
                    .collect(Collectors.toList());
            registerAuth(client, headerList);
        }
        return client;
    }

    public IGenericClient getClient(org.hl7.fhir.r4.model.Endpoint endpoint) throws IOException, InterruptedException,
            URISyntaxException {
        IGenericClient client = create(new URL(endpoint.getAddress()));
        if (endpoint.hasHeader()) {
            List<String> headerList = endpoint.getHeader().stream().map(headerString -> headerString.asStringValue())
                    .collect(Collectors.toList());
            registerAuth(client, headerList);
        }
        return client;
    }

    private static void registerAuth(IGenericClient client, List<String> headerList) {
        Map<String, String> headerMap = setupHeaderMap(headerList);
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            IClientInterceptor headInterceptor = new HeaderInjectionInterceptor(entry.getKey(), entry.getValue());
            client.registerInterceptor(headInterceptor);
        }
    }

    private static Map<String, String> setupHeaderMap(List<String> headerList) {
        Map<String, String> headerMap = new HashMap<String, String>();
        String leftAuth = null;
        String rightAuth = null;
        if (headerList.size() < 1 || headerList.isEmpty()) {
            leftAuth = null;
            rightAuth = null;
            headerMap.put(leftAuth, rightAuth);
        } else {
            for (String header : headerList) {
                if (!header.contains(":")) {
                    throw new RuntimeException("Endpoint header must contain \":\" .");
                }
                String[] authSplit = header.split(":");
                leftAuth = authSplit[0];
                rightAuth = authSplit[1];
                headerMap.put(leftAuth, rightAuth);
            }

        }
        return headerMap;
    }

    private static void setFhirContextFromEnum(FhirVersionEnum versionEnum) {
        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no implementation for anything older than DSTU2 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            fhirContext = FhirContext.forDstu2();
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            fhirContext = FhirContext.forDstu3();
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            fhirContext = FhirContext.forR4(); 
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no implementation for anything newer than or equal to R5 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum");
        }
    }
}