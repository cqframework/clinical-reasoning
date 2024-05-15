package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.StringType;

public class EndpointCredentials extends Endpoint {

    @Child(
            name = "vsacUsername",
            type = {StringType.class},
            order = 11,
            min = 0,
            max = 1,
            modifier = false,
            summary = true)
    @Description(
            shortDefinition = "A name that this endpoint can be identified by",
            formalDefinition = "A friendly name that this endpoint can be referred to with.")
    protected StringType vsacUsername;

    @Child(
            name = "apiKey",
            type = {StringType.class},
            order = 11,
            min = 0,
            max = 1,
            modifier = false,
            summary = true)
    @Description(
            shortDefinition = "A name that this endpoint can be identified by",
            formalDefinition = "A friendly name that this endpoint can be referred to with.")
    protected StringType apiKey;

    public StringType getVsacUsername() {
        return vsacUsername;
    }

    public void setVsacUsername(StringType vsacUsername) {
        this.vsacUsername = vsacUsername;
    }

    public StringType getApiKey() {
        return apiKey;
    }

    public void setApiKey(StringType apiKey) {
        this.apiKey = apiKey;
    }
}
