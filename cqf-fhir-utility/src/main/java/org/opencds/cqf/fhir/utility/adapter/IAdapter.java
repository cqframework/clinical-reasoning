package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ca.uhn.fhir.util.FhirTerser;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marker interface for HL7 Structure adapters
 *
 * @param <T> An HL7 Structure Type
 */
public interface IAdapter<T extends IBase> {
    public static final Logger logger = LoggerFactory.getLogger(IAdapter.class);

    String UNSUPPORTED_VERSION = "Unsupported version: %s";
    String MISSING_EXTENSION = "Field 'extension' does not exist on Element type {}";

    /**
     * @return returns the underlying HL7 Structure for this adapter
     */
    T get();

    FhirContext fhirContext();

    FhirTerser fhirTerser();

    default FhirVersionEnum fhirVersion() {
        return fhirContext().getVersion().getVersion();
    }

    IAdapterFactory getAdapterFactory();

    default void setExtension(List<? extends IBaseExtension<?, ?>> extensions) {
        try {
            setValue(get(), "extension", null);
            setValue(get(), "extension", extensions);
        } catch (Exception e) {
            // Do nothing
            logger.debug(MISSING_EXTENSION, get().fhirType());
        }
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> E addExtension() {
        if (get() instanceof IBaseHasExtensions baseHasExtensions) {
            return (E) baseHasExtensions.addExtension();
        }
        return null;
    }


//    <E extends IBaseExtension<?, ?>> E addExtension(E extension);

    default <E extends IBaseExtension<?, ?>> void addExtension(E extension) {
        try {
            setValue(get(), "extension", Collections.singletonList(extension));
        } catch (Exception e) {
            // Do nothing
            logger.debug(MISSING_EXTENSION, get().fhirType());
        }
    }

    default boolean hasExtension() {
        return !getExtension().isEmpty();
    }

    default boolean hasExtension(String url) {
        return hasExtension(get(), url);
    }

    default <E extends IBaseExtension<?, ?>> List<E> getExtension() {
        return getExtension(get());
    }

    default <E extends IBaseExtension<?, ?>> E getExtensionByUrl(String url) {
        return getExtensionByUrl(get(), url);
    }

    default <E extends IBaseExtension<?, ?>> List<E> getExtensionsByUrl(String url) {
        return getExtensionsByUrl(get(), url);
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> List<E> getExtension(IBase base) {
        return resolvePathList(base, "extension").stream().map(e -> (E) e).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> List<E> getExtensionsByUrl(IBase base, String url) {
        return getExtension(base).stream()
                .filter(e -> e.getUrl().equals(url))
                .map(e -> (E) e)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> List<E> getExtensionsByUrls(IBase base, Set<String> urls) {
        return getExtension(base).stream()
                .filter(e -> urls.contains(e.getUrl()))
                .map(e -> (E) e)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    default <E extends IBaseExtension<?, ?>> E getExtensionByUrl(IBase base, String url) {
        return getExtensionsByUrl(base, url).stream()
                .map(e -> (E) e)
                .findFirst()
                .orElse(null);
    }

    default Boolean hasExtension(IBase base, String url) {
        return getExtension(base).stream().anyMatch(e -> e.getUrl().equals(url));
    }

    Object resolvePath(Object target, String path);

    default List<IBase> resolvePathList(String path) {
        return resolvePathList(get(), path);
    }

    @SuppressWarnings("unchecked")
    default List<IBase> resolvePathList(IBase base, String path) {
        var pathResult = resolvePath(base, path);
        return pathResult instanceof List ? (List<IBase>) pathResult : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    default <B extends IBase> List<B> resolvePathList(IBase base, String path, Class<B> clazz) {
        return resolvePathList(base, path).stream().map(i -> (B) i).collect(Collectors.toList());
    }

    default String resolvePathString(String path) {
        return resolvePathString(get(), path);
    }

    default String resolvePathString(IBase base, String path) {
        var result = resolvePath(base, path);
        if (result == null) {
            return null;
        } else if (result instanceof IPrimitiveType<?> primitive && primitive.getValue() instanceof String string) {
            return string;
        } else if (result instanceof IBaseReference reference) {
            return reference.getReferenceElement().getValue();
        } else if (result instanceof IBaseEnumeration<?> enumeration) {
            return enumeration.getValueAsString();
        } else {
            throw new UnprocessableEntityException(String.format(
                    "Path (%s) on element of type (%s) could not be resolved",
                    path, base.getClass().getSimpleName()));
        }
    }

    default Object resolvePath(String path) {
        return resolvePath(get(), path);
    }

    default <B extends IBase> B resolvePath(String path, Class<B> clazz) {
        return resolvePath(get(), path, clazz);
    }

    @SuppressWarnings("unchecked")
    default <B extends IBase> B resolvePath(IBase base, String path, Class<B> clazz) {
        return (B) resolvePath(base, path);
    }

    void setValue(IBase base, String path, Object value);

    default void setValue(String path, Object value) {
        setValue(get(), path, value);
    }

    @SuppressWarnings("unchecked")
    static <T extends ICompositeType> T newPeriod(FhirVersionEnum version) {
        return switch (version) {
            case DSTU3 -> (T) new org.hl7.fhir.dstu3.model.Period();
            case R4 -> (T) new org.hl7.fhir.r4.model.Period();
            case R5 -> (T) new org.hl7.fhir.r5.model.Period();
            default -> throw new UnprocessableEntityException(UNSUPPORTED_VERSION.formatted(version.toString()));
        };
    }

    @SuppressWarnings("unchecked")
    static <T extends IPrimitiveType<String>> T newStringType(FhirVersionEnum version, String string) {
        return switch (version) {
            case DSTU3 -> (T) new org.hl7.fhir.dstu3.model.StringType(string);
            case R4 -> (T) new org.hl7.fhir.r4.model.StringType(string);
            case R5 -> (T) new org.hl7.fhir.r5.model.StringType(string);
            default -> throw new UnprocessableEntityException(UNSUPPORTED_VERSION.formatted(version.toString()));
        };
    }

    @SuppressWarnings("unchecked")
    static <T extends IPrimitiveType<String>> T newUriType(FhirVersionEnum version, String string) {
        return switch (version) {
            case DSTU3 -> (T) new org.hl7.fhir.dstu3.model.UriType(string);
            case R4 -> (T) new org.hl7.fhir.r4.model.UriType(string);
            case R5 -> (T) new org.hl7.fhir.r5.model.UriType(string);
            default -> throw new UnprocessableEntityException(UNSUPPORTED_VERSION.formatted(version.toString()));
        };
    }

    @SuppressWarnings("unchecked")
    static <T extends IPrimitiveType<String>> T newUrlType(FhirVersionEnum version, String string) {
        return switch (version) {
            case DSTU3 -> (T) new org.hl7.fhir.dstu3.model.UriType(string);
            case R4 -> (T) new org.hl7.fhir.r4.model.UrlType(string);
            case R5 -> (T) new org.hl7.fhir.r5.model.UrlType(string);
            default -> throw new UnprocessableEntityException(UNSUPPORTED_VERSION.formatted(version.toString()));
        };
    }

    @SuppressWarnings("unchecked")
    static <T extends IPrimitiveType<Date>> T newDateType(FhirVersionEnum version, Date date) {
        return switch (version) {
            case DSTU3 -> (T) new org.hl7.fhir.dstu3.model.DateType(date);
            case R4 -> (T) new org.hl7.fhir.r4.model.DateType(date);
            case R5 -> (T) new org.hl7.fhir.r5.model.DateType(date);
            default -> throw new UnprocessableEntityException(UNSUPPORTED_VERSION.formatted(version.toString()));
        };
    }

    @SuppressWarnings("unchecked")
    static <T extends IPrimitiveType<Date>> T newDateTimeType(FhirVersionEnum version, Date date) {
        return switch (version) {
            case DSTU3 -> (T) new org.hl7.fhir.dstu3.model.DateTimeType(date);
            case R4 -> (T) new org.hl7.fhir.r4.model.DateTimeType(date);
            case R5 -> (T) new org.hl7.fhir.r5.model.DateTimeType(date);
            default -> throw new UnprocessableEntityException(UNSUPPORTED_VERSION.formatted(version.toString()));
        };
    }
}
