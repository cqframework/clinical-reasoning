package org.opencds.cqf.fhir.utility.model;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildPrimitiveEnumerationDatatypeDefinition;
import ca.uhn.fhir.context.RuntimePrimitiveDatatypeDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
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

    private static final Pattern EXTENSION_PATTERN = Pattern.compile("extension\\('([^']+)'\\)(\\[(\\d+)\\])?");

    private record ExtensionInfo(String url, int index) {}

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

    public void setNestedValue(IBase target, String path, Object value, BaseRuntimeElementDefinition<?> def) {
        var segments = splitPathSegments(path);
        for (int i = 0; i < segments.size(); i++) {
            var segment = segments.get(i);
            var isLast = i == segments.size() - 1;

            if (isExtensionSegment(segment)) {
                var extInfo = parseExtensionSegment(segment);
                target = resolveOrCreateExtension(target, extInfo);
                if (!isLast) {
                    def = fhirContext.getElementDefinition(target.getClass());
                }
            } else {
                var isList = segment.contains("[");
                var isSlice = segment.contains(":");
                var sliceName = isSlice ? segment.split(":")[1] : null;
                var index = isList ? Character.getNumericValue(segment.charAt(segment.indexOf("[") + 1)) : 0;
                var targetPath = getTargetPath(segment, isList, isSlice);
                var targetDef = def.getChildByName(targetPath);
                if (targetDef != null) {
                    var targetValues = targetDef.getAccessor().getValues(target);
                    var targetValue = (targetValues.size() >= index + 1 && !isLast)
                            ? getTargetValueFromList(sliceName, index, targetValues)
                            : getTargetValue(target, value, isLast, targetPath, targetDef);
                    target = targetValue == null ? target : targetValue;
                    if (!isLast) {
                        var nextDef = fhirContext.getElementDefinition(target.getClass());
                        if (nextDef instanceof BaseRuntimeElementCompositeDefinition<?>) def = nextDef;
                        else if (nextDef instanceof RuntimePrimitiveDatatypeDefinition) def = nextDef;
                        else
                            throw new UnknownType("Unable to resolve the runtime definition for %s"
                                    .formatted(target.getClass().getName()));
                    }
                }
            }
        }
    }

    private static List<String> splitPathSegments(String path) {
        var segments = new ArrayList<String>();
        var current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\'') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '.' && !inQuotes) {
                segments.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            segments.add(current.toString());
        }
        return segments;
    }

    private static boolean isExtensionSegment(String segment) {
        return EXTENSION_PATTERN.matcher(segment).matches();
    }

    private static ExtensionInfo parseExtensionSegment(String segment) {
        var matcher = EXTENSION_PATTERN.matcher(segment);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not an extension segment: " + segment);
        }
        var url = matcher.group(1);
        var index = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return new ExtensionInfo(url, index);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private IBase resolveOrCreateExtension(IBase target, ExtensionInfo info) {
        if (!(target instanceof IBaseHasExtensions hasExtensions)) {
            throw new IllegalArgumentException(
                    "Target does not support extensions: " + target.getClass().getName());
        }
        var matching = hasExtensions.getExtension().stream()
                .filter(ext -> info.url().equals(ext.getUrl()))
                .toList();
        if (matching.size() > info.index()) {
            return (IBase) matching.get(info.index());
        }
        var extensionList = (List) hasExtensions.getExtension();
        IBaseExtension<?, ?> created = null;
        for (int i = matching.size(); i <= info.index(); i++) {
            created = newExtension(info.url());
            extensionList.add(created);
        }
        return (IBase) created;
    }

    private IBaseExtension<?, ?> newExtension(String url) {
        return switch (fhirContext.getVersion().getVersion()) {
            case DSTU3 -> new org.hl7.fhir.dstu3.model.Extension(url);
            case R4 -> new org.hl7.fhir.r4.model.Extension(url);
            case R5 -> new org.hl7.fhir.r5.model.Extension(url);
            default -> throw new IllegalStateException(
                    "Unsupported FHIR version: " + fhirContext.getVersion().getVersion());
        };
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
