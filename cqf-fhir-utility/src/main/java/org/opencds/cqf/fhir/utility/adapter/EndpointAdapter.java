package org.opencds.cqf.fhir.utility.adapter;

public interface EndpointAdapter extends ResourceAdapter {
    public String getAddress();

    public EndpointAdapter setAddress(String address);
}
