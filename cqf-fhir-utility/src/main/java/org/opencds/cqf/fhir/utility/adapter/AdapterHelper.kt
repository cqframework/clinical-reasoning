package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.fhir.utility.Resources;

public class AdapterHelper {
    private static final String CAST_ERROR_MESSAGE = "Cannot cast a value of type %s as %s.";
    private static final String CANONICAL = "CanonicalType";
    private static final String CODE = "Code";
    private static final String CODEABLECONCEPT = "CodeableConcept";
    private static final String PRIMITIVE = "IPrimitiveType";
    private static final String URI = "UriType";

    private AdapterHelper() {
        // intentionally empty
    }

    public static Object as(FhirVersionEnum fhirVersion, Object value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (value.getClass().getSimpleName().equals("Tuple")) {
            var adapterFactory = IAdapterFactory.forFhirVersion(fhirVersion);
            var tupleAdapter = adapterFactory.createTuple((IBase) value);
            var result = adapterFactory.createBase(Resources.newBaseForVersion(type.getSimpleName(), fhirVersion));
            tupleAdapter.getProperties().forEach(result::setValue);
            return result.get();
        }

        return switch (fhirVersion) {
            case DSTU3 -> asDstu3(value, type);
            case R4 -> asR4(value, type);
            case R5 -> asR5(value, type);
            default -> null;
        };
    }

    private static Object asDstu3(Object value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (type.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (value instanceof org.hl7.fhir.dstu3.model.UriType uriType) {
            switch (type.getSimpleName()) {
                case "AnnotatedUuidType":
                case "UuidType":
                    return uriType.hasPrimitiveValue() && uriType.getValue().startsWith("urn:uuid:")
                            ? new org.hl7.fhir.dstu3.model.UuidType(uriType.primitiveValue())
                            : null;
                case "OidType":
                    return uriType.hasPrimitiveValue() && uriType.getValue().startsWith("urn:oid:")
                            ? new org.hl7.fhir.dstu3.model.OidType(uriType.primitiveValue())
                            : null;
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.dstu3.model.IntegerType integerType) {
            switch (type.getSimpleName()) {
                case "PositiveIntType":
                    return integerType.hasPrimitiveValue() && integerType.getValue() > 0
                            ? new org.hl7.fhir.dstu3.model.PositiveIntType(integerType.primitiveValue())
                            : null;
                case "UnsignedIntType":
                    return integerType.hasPrimitiveValue() && integerType.getValue() >= 0
                            ? new org.hl7.fhir.dstu3.model.UnsignedIntType(integerType.primitiveValue())
                            : null;
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.dstu3.model.StringType stringType) {
            switch (type.getSimpleName()) {
                case "CodeType":
                    return stringType.castToCode(stringType);
                case "MarkdownType":
                    return stringType.castToMarkdown(stringType);
                case "IdType":
                    return stringType.hasPrimitiveValue()
                            ? new org.hl7.fhir.dstu3.model.IdType(stringType.primitiveValue())
                            : null;
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

        if (value instanceof org.hl7.fhir.dstu3.model.Quantity quantity) {
            switch (type.getSimpleName()) {
                case "Age":
                    var age = new org.hl7.fhir.dstu3.model.Age();
                    age.setValue(quantity.getValue());
                    age.setCode(quantity.getCode());
                    age.setUnit(quantity.getUnit());
                    age.setSystem(quantity.getSystem());
                    age.setComparator(quantity.getComparator());
                    return age;
                case "Distance":
                    var distance = new org.hl7.fhir.dstu3.model.Distance();
                    distance.setValue(quantity.getValue());
                    distance.setCode(quantity.getCode());
                    distance.setUnit(quantity.getUnit());
                    distance.setSystem(quantity.getSystem());
                    distance.setComparator(quantity.getComparator());
                    return distance;
                case "Duration":
                    var duration = new org.hl7.fhir.dstu3.model.Duration();
                    duration.setValue(quantity.getValue());
                    duration.setCode(quantity.getCode());
                    duration.setUnit(quantity.getUnit());
                    duration.setSystem(quantity.getSystem());
                    duration.setComparator(quantity.getComparator());
                    return duration;
                case "Count":
                    var count = new org.hl7.fhir.dstu3.model.Count();
                    count.setValue(quantity.getValue());
                    count.setCode(quantity.getCode());
                    count.setUnit(quantity.getUnit());
                    count.setSystem(quantity.getSystem());
                    count.setComparator(quantity.getComparator());
                    return count;
                case "SimpleQuantity":
                    return quantity.castToSimpleQuantity(quantity);
                // NOTE: This is wrong in that it is copying the comparator, it should be
                // ensuring comparator is not set...
                default:
                    break;
            }
        }

        throw new IllegalArgumentException(
                CAST_ERROR_MESSAGE.formatted(value.getClass().getName(), type.getName()));
    }

    private static Object asR4(Object value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (type.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (value instanceof org.hl7.fhir.r4.model.UriType uriType) {
            switch (type.getSimpleName()) {
                case "UrlType":
                    return uriType.castToUrl(uriType);
                case "CanonicalType":
                    return uriType.castToCanonical(uriType);
                case "AnnotatedUuidType":
                case "UuidType":
                    return uriType.hasPrimitiveValue() && uriType.getValue().startsWith("urn:uuid:")
                            ? new org.hl7.fhir.r4.model.UuidType(uriType.primitiveValue())
                            : null;
                case "OidType":
                    return uriType.hasPrimitiveValue() && uriType.getValue().startsWith("urn:oid:")
                            ? new org.hl7.fhir.r4.model.OidType(uriType.primitiveValue())
                            : null;
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r4.model.IntegerType integerType) {
            switch (type.getSimpleName()) {
                case "PositiveIntType":
                    return integerType.hasPrimitiveValue() && integerType.getValue() > 0
                            ? new org.hl7.fhir.r4.model.PositiveIntType(integerType.primitiveValue())
                            : null;
                case "UnsignedIntType":
                    return integerType.hasPrimitiveValue() && integerType.getValue() >= 0
                            ? new org.hl7.fhir.r4.model.UnsignedIntType(integerType.primitiveValue())
                            : null;
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r4.model.StringType stringType) {
            switch (type.getSimpleName()) {
                case "CodeType":
                    return stringType.castToCode(stringType);
                case "MarkdownType":
                    return stringType.castToMarkdown(stringType);
                case "IdType":
                    return stringType.hasPrimitiveValue()
                            ? new org.hl7.fhir.r4.model.IdType(stringType.primitiveValue())
                            : null;
                case CODEABLECONCEPT:
                    return stringType.castToCodeableConcept(stringType.castToCode(stringType));
                case CANONICAL:
                    return stringType.castToCanonical(stringType);
                case URI:
                    return stringType.castToUri(stringType);
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

        if (value instanceof org.hl7.fhir.r4.model.Quantity quantity) {
            switch (type.getSimpleName()) {
                case "Age":
                    var age = new org.hl7.fhir.r4.model.Age();
                    age.setValue(quantity.getValue());
                    age.setCode(quantity.getCode());
                    age.setUnit(quantity.getUnit());
                    age.setSystem(quantity.getSystem());
                    age.setComparator(quantity.getComparator());
                    return age;
                case "Distance":
                    var distance = new org.hl7.fhir.r4.model.Distance();
                    distance.setValue(quantity.getValue());
                    distance.setCode(quantity.getCode());
                    distance.setUnit(quantity.getUnit());
                    distance.setSystem(quantity.getSystem());
                    distance.setComparator(quantity.getComparator());
                    return distance;
                case "Duration":
                    var duration = new org.hl7.fhir.r4.model.Duration();
                    duration.setValue(quantity.getValue());
                    duration.setCode(quantity.getCode());
                    duration.setUnit(quantity.getUnit());
                    duration.setSystem(quantity.getSystem());
                    duration.setComparator(quantity.getComparator());
                    return duration;
                case "Count":
                    var count = new org.hl7.fhir.r4.model.Count();
                    count.setValue(quantity.getValue());
                    count.setCode(quantity.getCode());
                    count.setUnit(quantity.getUnit());
                    count.setSystem(quantity.getSystem());
                    count.setComparator(quantity.getComparator());
                    return count;
                case "SimpleQuantity":
                    return quantity.castToSimpleQuantity(quantity);
                // NOTE: This is wrong in that it is copying the comparator, it should be
                // ensuring comparator is not set...
                case "MoneyQuantity":
                    var moneyQuantity = new org.hl7.fhir.r4.model.MoneyQuantity();
                    moneyQuantity.setValue(quantity.getValue());
                    moneyQuantity.setCode(quantity.getCode());
                    moneyQuantity.setUnit(quantity.getUnit());
                    moneyQuantity.setSystem(quantity.getSystem());
                    moneyQuantity.setComparator(quantity.getComparator());
                    return moneyQuantity;
                default:
                    break;
            }
        }

        throw new IllegalArgumentException(
                CAST_ERROR_MESSAGE.formatted(value.getClass().getName(), type.getName()));
    }

    private static Object asR5(Object value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (type.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (value instanceof org.hl7.fhir.r5.model.UriType uriType) {
            switch (type.getSimpleName()) {
                case "UrlType":
                    return uriType.hasPrimitiveValue()
                            ? new org.hl7.fhir.r5.model.UrlType(uriType.primitiveValue())
                            : null;
                case "CanonicalType":
                    return uriType.hasPrimitiveValue()
                            ? new org.hl7.fhir.r5.model.CanonicalType(uriType.primitiveValue())
                            : null;
                case "AnnotatedUuidType":
                case "UuidType":
                    return uriType.hasPrimitiveValue() && uriType.getValue().startsWith("urn:uuid:")
                            ? new org.hl7.fhir.r5.model.UuidType(uriType.primitiveValue())
                            : null;
                case "OidType":
                    return uriType.hasPrimitiveValue() && uriType.getValue().startsWith("urn:oid:")
                            ? new org.hl7.fhir.r5.model.OidType(uriType.primitiveValue())
                            : null;
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r5.model.IntegerType integerType) {
            switch (type.getSimpleName()) {
                case "PositiveIntType":
                    return integerType.hasPrimitiveValue() && integerType.getValue() > 0
                            ? new org.hl7.fhir.r5.model.PositiveIntType(integerType.primitiveValue())
                            : null;
                case "UnsignedIntType":
                    return integerType.hasPrimitiveValue() && integerType.getValue() >= 0
                            ? new org.hl7.fhir.r5.model.UnsignedIntType(integerType.primitiveValue())
                            : null;
                default:
                    break;
            }
        }

        if (value instanceof org.hl7.fhir.r5.model.StringType stringType) {
            switch (type.getSimpleName()) {
                case "CodeType":
                    return stringType.hasPrimitiveValue()
                            ? new org.hl7.fhir.r5.model.CodeType(stringType.primitiveValue())
                            : null;
                case "MarkdownType":
                    return stringType.hasPrimitiveValue()
                            ? new org.hl7.fhir.r5.model.MarkdownType(stringType.primitiveValue())
                            : null;
                case "IdType":
                    return stringType.hasPrimitiveValue()
                            ? new org.hl7.fhir.r5.model.IdType(stringType.primitiveValue())
                            : null;
                case CODEABLECONCEPT:
                    return new org.hl7.fhir.r5.model.CodeableConcept(
                            new org.hl7.fhir.r5.model.Coding(null, stringType.asStringValue(), null));
                case CANONICAL:
                    return new org.hl7.fhir.r5.model.CanonicalType(stringType.asStringValue());
                case URI:
                    return new org.hl7.fhir.r5.model.UriType(stringType.asStringValue());
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

        if (value instanceof org.hl7.fhir.r5.model.Quantity quantity) {
            switch (type.getSimpleName()) {
                case "Age":
                    var age = new org.hl7.fhir.r5.model.Age();
                    age.setValue(quantity.getValue());
                    age.setCode(quantity.getCode());
                    age.setUnit(quantity.getUnit());
                    age.setSystem(quantity.getSystem());
                    age.setComparator(quantity.getComparator());
                    return age;
                case "Distance":
                    var distance = new org.hl7.fhir.r5.model.Distance();
                    distance.setValue(quantity.getValue());
                    distance.setCode(quantity.getCode());
                    distance.setUnit(quantity.getUnit());
                    distance.setSystem(quantity.getSystem());
                    distance.setComparator(quantity.getComparator());
                    return distance;
                case "Duration":
                    var duration = new org.hl7.fhir.r5.model.Duration();
                    duration.setValue(quantity.getValue());
                    duration.setCode(quantity.getCode());
                    duration.setUnit(quantity.getUnit());
                    duration.setSystem(quantity.getSystem());
                    duration.setComparator(quantity.getComparator());
                    return duration;
                case "Count":
                    var count = new org.hl7.fhir.r5.model.Count();
                    count.setValue(quantity.getValue());
                    count.setCode(quantity.getCode());
                    count.setUnit(quantity.getUnit());
                    count.setSystem(quantity.getSystem());
                    count.setComparator(quantity.getComparator());
                    return count;
                case "SimpleQuantity":
                    return org.hl7.fhir.r5.model.TypeConvertor.castToSimpleQuantity(quantity);
                // NOTE: This is wrong in that it is copying the comparator,
                // it should be ensuring comparator is not set...
                case "MoneyQuantity":
                    var moneyQuantity = new org.hl7.fhir.r5.model.MoneyQuantity();
                    moneyQuantity.setValue(quantity.getValue());
                    moneyQuantity.setCode(quantity.getCode());
                    moneyQuantity.setUnit(quantity.getUnit());
                    moneyQuantity.setSystem(quantity.getSystem());
                    moneyQuantity.setComparator(quantity.getComparator());
                    return moneyQuantity;
                default:
                    break;
            }
        }

        throw new IllegalArgumentException(
                CAST_ERROR_MESSAGE.formatted(value.getClass().getName(), type.getName()));
    }
}
