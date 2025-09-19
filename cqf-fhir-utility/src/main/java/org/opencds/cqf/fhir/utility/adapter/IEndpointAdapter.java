package org.opencds.cqf.fhir.utility.adapter;

import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newUrlType;

public interface IEndpointAdapter extends IResourceAdapter {
    default String getAddress() {
        return resolvePathString(get(), "address");
    }

    default void setAddress(String address) {
        getModelResolver()
                .setValue(
                        get(), "address", newUrlType(fhirContext().getVersion().getVersion(), address));
    }
}
