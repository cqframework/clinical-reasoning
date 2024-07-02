package org.opencds.cqf.fhir.utility.adapter;

import static org.opencds.cqf.fhir.utility.adapter.ResourceAdapter.newUrlType;

public interface EndpointAdapter extends ResourceAdapter {
    public default String getAddress() {
        return resolvePathString(get(), "address");
    }

    public default void setAddress(String address) {
        getModelResolver()
                .setValue(
                        get(), "address", newUrlType(fhirContext().getVersion().getVersion(), address));
    }
}
