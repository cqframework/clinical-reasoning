package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildChoiceDefinition;
import ca.uhn.fhir.context.RuntimeChildPrimitiveEnumerationDatatypeDefinition;
import ca.uhn.fhir.context.RuntimePrimitiveDatatypeDefinition;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public abstract class AdapterBase {
    private static final String ENUMERATION = "Enumeration";
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("extension\\('([^']+)'\\)(\\[(\\d+)])?");

    protected final FhirContext fhirContext;
    protected final FhirTerser fhirTerser;
    protected final IAdapterFactory adapterFactory;

    private record ExtensionInfo(String url, int index) {}

    public AdapterBase(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        fhirTerser = this.fhirContext.newTerser();
        adapterFactory = IAdapterFactory.forFhirContext(this.fhirContext);
    }

    public FhirContext fhirContext() {
        return fhirContext;
    }

    public FhirTerser fhirTerser() {
        return fhirTerser;
    }

    public IAdapterFactory getAdapterFactory() {
        return adapterFactory;
    }

    public void setValue(IBase target, String path, Object value) {
        if (target == null) {
            return;
        }

        var definition = resolveRuntimeDefinition(target);
        if (path.contains(("."))) {
            setNestedValue(target, path, value, definition);
        } else {
            if (!path.contains("[x]")) {
                var childDef = definition.getChildByName(path);
                if (childDef != null) {
                    var elementDef = childDef.getChildByName(path);
                    if (elementDef != null
                            && elementDef.getImplementingClass().getSimpleName().equals(ENUMERATION)
                            && value != null
                            && !value.getClass().getSimpleName().equals(ENUMERATION)) {
                        value = getEnumValue((RuntimeChildPrimitiveEnumerationDatatypeDefinition) childDef, value);
                    }
                }
            }

            if (target instanceof IBaseEnumeration<?> enumeration && path.equals("value")) {
                enumeration.setValueAsString((String) value);
                return;
            }

            if (target instanceof IPrimitiveType<?> primitiveType) {
                setPrimitiveValue(value, primitiveType);
                return;
            }

            BaseRuntimeChildDefinition child = definition.getChildByName(path);
            if (child == null) {
                child = resolveChoiceProperty(definition, path);
            }

            if (child == null) {
                throw new IllegalArgumentException(String.format("Unable to resolve path %s.", path));
            }

            try {
                if (value instanceof Iterable) {
                    for (Object val : (Iterable<?>) value) {
                        child.getMutator().addValue(target, setBaseValue(val, target));
                    }
                } else {
                    child.getMutator().setValue(target, setBaseValue(value, target));
                }
            } catch (IllegalArgumentException le) {
                //                if (value != null && value.getClass().getSimpleName().equals("Quantity")) {
                //                    try {
                //                        value = adapterFactory.createSimpleQuantity(value)
                // castToSimpleQuantity((BaseType) value);
                //                    } catch (FHIRException e) {
                //                        throw new UnprocessableEntityException(
                //                            "Unable to cast Quantity to SimpleQuantity");
                //                    }
                //                    child.getMutator().setValue(target, setBaseValue(value, target));
                //                } else {
                throw new UnprocessableEntityException(
                        String.format("Configuration error encountered: %s", le.getMessage()));
                //                }
            }
        }
    }

    public void setPrimitiveValue(Object value, IPrimitiveType target) {
        String simpleName = target.getClass().getSimpleName();
        switch (simpleName) {
            case "DateTimeType":
            case "InstantType":
                // Ensure offset is taken into account from the ISO datetime String instead of the default timezone
                target.setValueAsString(value.toString());
                // TODO:
                // setCalendarConstant((BaseDateTimeType) target, (BaseTemporal) value);
                break;
            case "DateType":
                target.setValue(value);
                // TODO:
                // setCalendarConstant((BaseDateTimeType) target, (BaseTemporal) value);
                break;
            case "TimeType":
                target.setValue(value.toString());
                break;
            case "Base64BinaryType":
                target.setValueAsString((String) value);
                break;
            default:
                target.setValue(value);
        }
    }

    public IBase setBaseValue(Object value, IBase target) {
        if (target instanceof IPrimitiveType<?> primitiveType) {
            setPrimitiveValue(value, primitiveType);
        }
        return (IBase) value;
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

        throw new UnprocessableEntityException("Unable to resolve the runtime definition for %s"
                .formatted(base.getClass().getName()));
    }

    protected BaseRuntimeChildDefinition resolveChoiceProperty(
            BaseRuntimeElementCompositeDefinition<?> definition, String path) {
        for (Object child : definition.getChildren()) {
            if (child instanceof RuntimeChildChoiceDefinition choiceDefinition) {
                if (choiceDefinition.getElementName().startsWith(path)) {
                    return choiceDefinition;
                }
            }
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Enum<?>, E extends IBaseEnumeration<T>> E getEnumValue(
            RuntimeChildPrimitiveEnumerationDatatypeDefinition targetDef, Object value) {
        String enumValue;
        if (value instanceof IPrimitiveType<?> primitiveType) {
            enumValue = primitiveType.getValueAsString();
        } else if (value instanceof IBaseCoding coding) {
            enumValue = coding.getCode();
        } else {
            enumValue = value.toString();
        }
        return switch (fhirContext.getVersion().getVersion()) {
            case DSTU3 ->
                (E) new org.hl7.fhir.dstu3.model.Enumeration(toEnumFactory(targetDef.getBoundEnumType()), enumValue);
            case R4 ->
                (E) new org.hl7.fhir.r4.model.Enumeration(toEnumFactory(targetDef.getBoundEnumType()), enumValue);
            case R5 ->
                (E) new org.hl7.fhir.r5.model.Enumeration(toEnumFactory(targetDef.getBoundEnumType()), enumValue);
            default -> null;
        };
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
            throw new UnprocessableEntityException("Failed to instantiate %s".formatted(className));
        }
        return retVal;
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
                            throw new UnprocessableEntityException("Unable to resolve the runtime definition for %s"
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
            default ->
                throw new IllegalStateException(
                        "Unsupported FHIR version: " + fhirContext.getVersion().getVersion());
        };
    }

    private String getTargetPath(String identifier, boolean isList, boolean isSlice) {
        if (isList) {
            return identifier.replaceAll("\\[\\d]", "");
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
                        ((IPrimitiveType<?>) value).getValueAsString());
            } else {
                targetValue = (IBase) value;
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
}
