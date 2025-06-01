package org.opencds.cqf.fhir.utility.model;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildPrimitiveEnumerationDatatypeDefinition;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownType;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicModelResolver extends CachingModelResolverDecorator {
    protected static final Logger logger = LoggerFactory.getLogger(DynamicModelResolver.class);

    private static final String CANONICAL = "CanonicalType";
    private static final String CODE = "Code";
    private static final String CODEABLECONCEPT = "CodeableConcept";
    private static final String ENUMERATION = "Enumeration";
    private static final String PRIMITIVE = "IPrimitiveType";
    private static final String URI = "UriType";

    private final FhirContext fhirContext;

    @SuppressWarnings("rawtypes")
    public DynamicModelResolver(ModelResolver modelResolver) {
        super(modelResolver);
        this.fhirContext = ((FhirModelResolver) modelResolver).getFhirContext();
    }

    public DynamicModelResolver(ModelResolver modelResolver, FhirContext fhirContext) {
        super(modelResolver);
        this.fhirContext = fhirContext;
    }

    @Override
    public Object resolvePath(Object target, String path) {
        Object pathResult = null;
        try {
            pathResult = super.resolvePath(target, path);
        } catch (Exception e) {
            logger.error("Error encountered resolving path: %s".formatted(path), e);
        }
        return pathResult;
    }

    @Override
    public Object as(Object value, Class<?> type, boolean isStrict) {
        if (value instanceof org.hl7.fhir.dstu3.model.StringType stringType) {
            switch (type.getSimpleName()) {
                case CODEABLECONCEPT:
                    return stringType.castToCodeableConcept(stringType.castToCode(stringType));
                case URI:
                    return stringType.castToUri(stringType);
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.dstu3.model.Coding coding) {
            switch (type.getSimpleName()) {
                case CODE:
                case PRIMITIVE:
                    return coding.getCodeElement();
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r4.model.StringType stringType) {
            switch (type.getSimpleName()) {
                case CODEABLECONCEPT:
                    return stringType.castToCodeableConcept(stringType.castToCode(stringType));
                case CANONICAL:
                    return stringType.castToCanonical(stringType);
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r4.model.Coding coding) {
            switch (type.getSimpleName()) {
                case CODE:
                case PRIMITIVE:
                    return coding.getCodeElement();
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r5.model.StringType stringType) {
            switch (type.getSimpleName()) {
                case CODEABLECONCEPT:
                    return new org.hl7.fhir.r5.model.CodeableConcept(
                            new org.hl7.fhir.r5.model.Coding(null, stringType.asStringValue(), null));
                case CANONICAL:
                    return new org.hl7.fhir.r5.model.CanonicalType(stringType.asStringValue());
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r5.model.Coding coding) {
            switch (type.getSimpleName()) {
                case CODE:
                case PRIMITIVE:
                    return coding.getCodeElement();
                default:
                    break;
            }
        }

        return super.as(value, type, isStrict);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setValue(Object target, String path, Object value) {
        if (target == null) {
            return;
        }

        if (target instanceof IBaseEnumeration<?> enumeration && path.equals("value")) {
            enumeration.setValueAsString((String) value);
            return;
        }

        IBase base = (IBase) target;
        BaseRuntimeElementCompositeDefinition<?> definition;
        if (base instanceof IPrimitiveType type) {
            ((FhirModelResolver) this.getInnerResolver()).setPrimitiveValue(value, type);
            return;
        } else {
            definition = resolveRuntimeDefinition(base);
        }

        if (path.contains(("."))) {
            setNestedValue((IBase) target, path, value, definition);
        } else {
            if (!path.contains("[x]")) {
                var childDef = definition.getChildByName(path);
                if (childDef != null) {
                    var elementDef = childDef.getChildByName(path);
                    if (elementDef != null
                            && elementDef.getImplementingClass().getSimpleName().equals(ENUMERATION)
                            && value != null
                            && !value.getClass().getSimpleName().equals(ENUMERATION)) {
                        value = getEnumValue(
                                (RuntimeChildPrimitiveEnumerationDatatypeDefinition) childDef,
                                ((IPrimitiveType<?>) this.as(value, IPrimitiveType.class, false)).getValueAsString());
                    }
                }
            }

            super.setValue(target, path, value);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends IBase> BaseRuntimeElementCompositeDefinition<T> resolveRuntimeDefinition(IBase base) {
        if (base instanceof IAnyResource resource) {
            return (BaseRuntimeElementCompositeDefinition<T>) fhirContext.getResourceDefinition(resource);
        } else if (base instanceof IBaseBackboneElement || base instanceof IBaseElement) {
            return (BaseRuntimeElementCompositeDefinition<T>) fhirContext.getElementDefinition(base.getClass());
        } else if (base instanceof ICompositeType) {
            return (BaseRuntimeElementCompositeDefinition<T>) fhirContext.getElementDefinition(base.getClass());
        }

        throw new UnknownType("Unable to resolve the runtime definition for %s"
                .formatted(base.getClass().getName()));
    }

    public void setNestedValue(IBase target, String path, Object value, BaseRuntimeElementCompositeDefinition<?> def) {
        var identifiers = path.split("\\.");
        for (int i = 0; i < identifiers.length; i++) {
            var identifier = identifiers[i];
            var isList = identifier.contains("[");
            var isSlice = identifier.contains(":");
            var sliceName = isSlice ? identifier.split(":")[1] : null;
            var isLast = i == identifiers.length - 1;
            var index = isList ? Character.getNumericValue(identifier.charAt(identifier.indexOf("[") + 1)) : 0;
            var targetPath = getTargetPath(identifier, isList, isSlice);
            var targetDef = def.getChildByName(targetPath);
            var targetValues = targetDef.getAccessor().getValues(target);
            var targetValue = (targetValues.size() >= index + 1 && !isLast)
                    ? getTargetValueFromList(sliceName, index, targetValues)
                    : getTargetValue(target, value, isLast, targetPath, targetDef);
            target = targetValue == null ? target : targetValue;
            if (!isLast) {
                var nextDef = fhirContext.getElementDefinition(target.getClass());
                def = (BaseRuntimeElementCompositeDefinition<?>) nextDef;
            }
        }
    }

    private String getTargetPath(String identifier, boolean isList, boolean isSlice) {
        if (isList) {
            return identifier.replaceAll("\\[\\d\\]", "");
        }
        if (isSlice) {
            return identifier.substring(0, identifier.indexOf(":"));
        }
        return identifier;
    }

    private IBase getTargetValue(
            IBase target, Object value, boolean isLast, String targetPath, BaseRuntimeChildDefinition targetDef) {
        IBase targetValue;
        var elementDef = targetDef.getChildByName(targetPath);
        if (isLast) {
            var elementClass = elementDef.getImplementingClass();
            if (elementClass.getSimpleName().equals(ENUMERATION)) {
                targetValue = getEnumValue(
                        (RuntimeChildPrimitiveEnumerationDatatypeDefinition) targetDef,
                        ((IPrimitiveType<?>) this.as(value, IPrimitiveType.class, false)).getValueAsString());
            } else {
                targetValue = (IBase) this.as(value, elementClass, false);
            }
        } else {
            targetValue = elementDef.newInstance(targetDef.getInstanceConstructorArguments());
        }
        if (targetValue != null) {
            targetDef.getMutator().addValue(target, targetValue);
        }
        return targetValue;
    }

    private IBase getTargetValueFromList(String sliceName, int index, List<IBase> targetValues) {
        IBase targetValue;
        if (targetValues.size() > 1 && StringUtils.isNotBlank(sliceName)) {
            // TODO: handle slice names
            // targetValue = targetValues.stream()
            targetValue = targetValues.get(0);
        } else {
            targetValue = targetValues.get(index);
        }
        return targetValue;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Enum<?>, E extends IBaseEnumeration<T>> E getEnumValue(
            RuntimeChildPrimitiveEnumerationDatatypeDefinition targetDef, String value) {
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                return (E) new org.hl7.fhir.dstu3.model.Enumeration(
                        (org.hl7.fhir.dstu3.model.EnumFactory<Enum<?>>) toEnumFactory(targetDef.getBoundEnumType()),
                        value);
            case R4:
                return (E) new org.hl7.fhir.r4.model.Enumeration(
                        (org.hl7.fhir.r4.model.EnumFactory<Enum<?>>) toEnumFactory(targetDef.getBoundEnumType()),
                        value);
            case R5:
                return (E) new org.hl7.fhir.r5.model.Enumeration(
                        (org.hl7.fhir.r5.model.EnumFactory<Enum<?>>) toEnumFactory(targetDef.getBoundEnumType()),
                        value);

            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    static <E extends IBaseEnumFactory<?>> E toEnumFactory(Class<?> enumerationType) {
        Class<?> clazz;
        String className = enumerationType.getName() + "EnumFactory";
        E retVal;
        try {
            clazz = Class.forName(className);
            retVal = (E) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new org.opencds.cqf.cql.engine.fhir.exception.UnknownType(
                    "Failed to instantiate %s".formatted(className));
        }
        return retVal;
    }
}
