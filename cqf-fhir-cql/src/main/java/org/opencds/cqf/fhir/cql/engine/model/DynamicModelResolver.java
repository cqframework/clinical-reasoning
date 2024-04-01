package org.opencds.cqf.fhir.cql.engine.model;

import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildPrimitiveEnumerationDatatypeDefinition;
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
            logger.error(String.format("Error encountered resolving path: %s", path), e);
        }
        return pathResult;
    }

    @Override
    public Object as(Object value, Class<?> type, boolean isStrict) {
        if (value instanceof org.hl7.fhir.dstu3.model.StringType) {
            var stringType = (org.hl7.fhir.dstu3.model.StringType) value;
            switch (type.getSimpleName()) {
                case CODEABLECONCEPT:
                    return stringType.castToCodeableConcept(stringType.castToCode(stringType));
                case URI:
                    return stringType.castToUri(stringType);
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.dstu3.model.Coding) {
            var coding = (org.hl7.fhir.dstu3.model.Coding) value;
            // var patient = new org.hl7.fhir.dstu3.model.Patient().setGender(null)
            switch (type.getSimpleName()) {
                case CODE:
                case PRIMITIVE:
                    return coding.getCodeElement();
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r4.model.StringType) {
            var stringType = (org.hl7.fhir.r4.model.StringType) value;
            switch (type.getSimpleName()) {
                case CODEABLECONCEPT:
                    return stringType.castToCodeableConcept(stringType.castToCode(stringType));
                case CANONICAL:
                    return stringType.castToCanonical(stringType);
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r4.model.Coding) {
            var coding = (org.hl7.fhir.r4.model.Coding) value;
            switch (type.getSimpleName()) {
                case CODE:
                case PRIMITIVE:
                    return coding.getCodeElement();
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r5.model.StringType) {
            var stringType = (org.hl7.fhir.r5.model.StringType) value;
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

        if (value instanceof org.hl7.fhir.r5.model.Coding) {
            var coding = (org.hl7.fhir.r5.model.Coding) value;
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

        if (target instanceof IBaseEnumeration && path.equals("value")) {
            ((IBaseEnumeration<?>) target).setValueAsString((String) value);
            return;
        }

        IBase base = (IBase) target;
        BaseRuntimeElementCompositeDefinition<?> definition;
        if (base instanceof IPrimitiveType) {
            ((FhirModelResolver) this.getInnerResolver()).setPrimitiveValue(value, (IPrimitiveType) base);
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
    protected BaseRuntimeElementCompositeDefinition<?> resolveRuntimeDefinition(IBase base) {
        if (base instanceof IAnyResource) {
            return fhirContext.getResourceDefinition((IAnyResource) base);
        } else if (base instanceof IBaseBackboneElement || base instanceof IBaseElement) {
            return (BaseRuntimeElementCompositeDefinition<?>) fhirContext.getElementDefinition(base.getClass());
        } else if (base instanceof ICompositeType) {
            return (BaseRuntimeElementCompositeDefinition<ICompositeType>)
                    fhirContext.getElementDefinition(base.getClass());
        }

        throw new UnknownType(String.format(
                "Unable to resolve the runtime definition for %s",
                base.getClass().getName()));
    }

    public void setNestedValue(IBase target, String path, Object value, BaseRuntimeElementCompositeDefinition<?> def) {
        // var def = (BaseRuntimeElementCompositeDefinition<?>) fhirContext
        // .getElementDefinition(target.getClass());
        var identifiers = path.split("\\.");
        for (int i = 0; i < identifiers.length; i++) {
            var identifier = identifiers[i];
            var isList = identifier.contains("[");
            var isLast = i == identifiers.length - 1;
            var index = isList ? Character.getNumericValue(identifier.charAt(identifier.indexOf("[") + 1)) : 0;
            var targetPath = isList ? identifier.replaceAll("\\[\\d\\]", "") : identifier;
            var targetDef = def.getChildByName(targetPath);

            var targetValues = targetDef.getAccessor().getValues(target);
            IBase targetValue;
            if (targetValues.size() >= index + 1 && !isLast) {
                targetValue = targetValues.get(index);
            } else {
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
            }
            target = targetValue == null ? target : targetValue;
            if (!isLast) {
                var nextDef = fhirContext.getElementDefinition(target.getClass());
                def = (BaseRuntimeElementCompositeDefinition<?>) nextDef;
            }
        }
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
                    String.format("Failed to instantiate %s", className));
        }
        return retVal;
    }
}
