package org.opencds.cqf.fhir.utility.adapter;

import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newStringType;
import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newUrlType;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public interface IEndpointAdapter extends IResourceAdapter {
    default String getAddress() {
        return resolvePathString(get(), "address");
    }

    default void setAddress(String address) {
        getModelResolver()
                .setValue(
                        get(), "address", newUrlType(fhirContext().getVersion().getVersion(), address));
    }

    default boolean hasHeaders() {
        return !getHeaders().isEmpty();
    }

    default List<String> getHeaders() {
        return resolvePathList(get(), "header").stream()
                .map(header -> ((IPrimitiveType<?>) header).getValueAsString())
                .collect(Collectors.toList());
    }

    default void addHeader(String header) {
        var headers = getHeaders();
        headers.add(header);
        setHeaders(headers);
    }

    default void setHeaders(List<String> headers) {
        var mappedHeaders = headers.isEmpty()
                ? null
                : headers.stream()
                        .map(header -> newStringType(fhirContext().getVersion().getVersion(), header))
                        .toList();
        getModelResolver().setValue(get(), "header", mappedHeaders);
    }
}
