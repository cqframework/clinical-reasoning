package org.opencds.cqf.fhir.utility;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

/**
 * This class provides utility methods for doing reflection on FHIR resources. It's specifically
 * focused on knowledge artifact resources since there's not a common interface for those across
 * different Resources (and FHIR versions)
 */
public class Reflections {

    private Reflections() {}

    /**
     * Gets the IAccessor for the given BaseType and child
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of the IBase type
     * @param childName the name of the child property of the BaseType to generate an accessor for
     * @return an IAccessor for the given child and the BaseType
     */
    public static <BaseType extends IBase> IAccessor getAccessor(
            final Class<? extends BaseType> baseTypeClass, String childName) {
        checkNotNull(baseTypeClass);
        checkNotNull(childName);

        FhirContext fhirContext = FhirContext.forCached(FhirVersions.forClass(baseTypeClass));
        BaseRuntimeElementDefinition<?> elementDefinition = fhirContext.getElementDefinition(baseTypeClass);
        return elementDefinition.getChildByName(childName).getAccessor();
    }

    /**
     * Generates a function to access a primitive property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param <ReturnType> a return type for the Functions
     * @param baseTypeClass the class of a the IBase type
     * @param childName to create a function for
     * @return a function for accessing the "childName" property of the BaseType
     */
    public static <BaseType extends IBase, ReturnType> Function<BaseType, ReturnType> getPrimitiveFunction(
            final Class<? extends BaseType> baseTypeClass, String childName) {
        checkNotNull(baseTypeClass);
        checkNotNull(childName);

        IAccessor accessor = getAccessor(baseTypeClass, childName);
        return r -> {
            Optional<IBase> value = accessor.getFirstValueOrNull(r);
            if (!value.isPresent()) {
                return null;
            } else {
                @SuppressWarnings("unchecked")
                ReturnType x = ((IPrimitiveType<ReturnType>) value.get()).getValue();
                return x;
            }
        };
    }

    /**
     * Generates a function to access a primitive property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param <ReturnType> a return type for the Functions
     * @param baseTypeClass the class of a the IBase type
     * @param childName to create a function for
     * @return a function for accessing the "childName" property of the BaseType
     */
    public static <BaseType extends IBase, ReturnType extends List<? extends IBase>>
            Function<BaseType, ReturnType> getFunction(
                    final Class<? extends BaseType> baseTypeClass, String childName) {
        checkNotNull(baseTypeClass);
        checkNotNull(childName);

        IAccessor accessor = getAccessor(baseTypeClass, childName);
        return r -> {
            @SuppressWarnings("unchecked")
            ReturnType x = (ReturnType) accessor.getValues(r);
            return x;
        };
    }

    /**
     * Generates a function to access the "version" property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of a the IBase type
     * @return a function for accessing the "version" property of the BaseType
     */
    public static <BaseType extends IBase> Function<BaseType, String> getVersionFunction(
            final Class<? extends BaseType> baseTypeClass) {
        checkNotNull(baseTypeClass);

        return getPrimitiveFunction(baseTypeClass, "version");
    }

    /**
     * Generates a function to access the "url" property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of a the IBase type
     * @return a function for accessing the "url" property of the BaseType
     */
    public static <BaseType extends IBase> Function<BaseType, String> getUrlFunction(
            final Class<? extends BaseType> baseTypeClass) {
        checkNotNull(baseTypeClass);

        return getPrimitiveFunction(baseTypeClass, "url");
    }

    /**
     * Generates a function to access the "name" property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of a the IBase type
     * @return a function for accessing the "name" property of the BaseType
     */
    public static <BaseType extends IBase> Function<BaseType, String> getNameFunction(
            final Class<? extends BaseType> baseTypeClass) {
        checkNotNull(baseTypeClass);

        return getPrimitiveFunction(baseTypeClass, "name");
    }
}
