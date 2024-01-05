package org.opencds.cqf.fhir.cr.common;

import static org.opencds.cqf.fhir.utility.OperationOutcomes.addExceptionToOperationOutcome;
import static org.opencds.cqf.fhir.utility.OperationOutcomes.newOperationOutcome;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;

public interface IOperationRequest {
    IIdType getSubjectId();

    IBaseBundle getBundle();

    IBaseParameters getParameters();

    LibraryEngine getLibraryEngine();

    ModelResolver getModelResolver();

    FhirVersionEnum getFhirVersion();

    String getDefaultLibraryUrl();

    IBaseOperationOutcome getOperationOutcome();

    void setOperationOutcome(IBaseOperationOutcome operationOutcome);

    default void logException(String exceptionMessage) {
        if (getOperationOutcome() == null) {
            setOperationOutcome(newOperationOutcome(getFhirVersion()));
        }
        addExceptionToOperationOutcome(getOperationOutcome(), exceptionMessage);
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> List<E> getExtensions(IBase base) {
        return resolvePathList(base, "extension").stream().map(e -> (E) e).collect(Collectors.toList());
    }

    default Boolean hasExtension(IBase base, String url) {
        return getExtensions(base).stream().anyMatch(e -> e.getUrl().equals(url));
    }

    @SuppressWarnings("unchecked")
    default List<IBase> resolvePathList(IBase base, String path) {
        var pathResult = this.getModelResolver().resolvePath(base, path);
        return pathResult instanceof List ? (List<IBase>) pathResult : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    default <T extends IBase> List<T> resolvePathList(IBase base, String path, Class<T> clazz) {
        return resolvePathList(base, path).stream().map(i -> (T) i).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    default String resolvePathString(IBase base, String path) {
        var result = (IPrimitiveType<String>) resolvePath(base, path);
        return result == null ? null : result.getValue();
    }

    default IBase resolvePath(IBase base, String path) {
        return (IBase) this.getModelResolver().resolvePath(base, path);
    }

    @SuppressWarnings("unchecked")
    default <T extends IBase> T resolvePath(IBase base, String path, Class<T> clazz) {
        return (T) resolvePath(base, path);
    }

    default List<IBaseBackboneElement> getItems(IBase base) {
        return resolvePathList(base, "item", IBaseBackboneElement.class);
    }
}
