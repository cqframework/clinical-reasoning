package org.opencds.cqf.fhir.cql.engine.model;

import org.opencds.cqf.cql.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.cql.engine.model.ModelResolver;

public class DynamicModelResolver extends CachingModelResolverDecorator {

  public DynamicModelResolver(ModelResolver modelResolver) {
    super(modelResolver);
  }

  @Override
  public Object resolvePath(Object target, String path) {
    Object pathResult = null;
    try {
      pathResult = super.resolvePath(target, path);
    } catch (Exception e) {
    }
    return pathResult;
  }

  @Override
  public Object as(Object value, Class<?> type, boolean isStrict) {
    if (value instanceof org.hl7.fhir.dstu3.model.StringType) {
      org.hl7.fhir.r4.model.StringType stringType = (org.hl7.fhir.r4.model.StringType) value;
      switch (type.getSimpleName()) {
        case "CodeableConcept":
          return stringType.castToCodeableConcept(stringType.castToCode(stringType));
        case "CanonicalType":
          return stringType.castToCanonical(stringType);
        default:
          break;
      }
    }

    if (value instanceof org.hl7.fhir.r4.model.StringType) {
      org.hl7.fhir.r4.model.StringType stringType = (org.hl7.fhir.r4.model.StringType) value;
      switch (type.getSimpleName()) {
        case "CodeableConcept":
          return stringType.castToCodeableConcept(stringType.castToCode(stringType));
        case "CanonicalType":
          return stringType.castToCanonical(stringType);
        default:
          break;
      }
    }

    if (value instanceof org.hl7.fhir.r5.model.StringType) {
      org.hl7.fhir.r5.model.StringType stringType = (org.hl7.fhir.r5.model.StringType) value;
      switch (type.getSimpleName()) {
        case "CodeableConcept":
          return new org.hl7.fhir.r5.model.CodeableConcept(
              new org.hl7.fhir.r5.model.Coding(null, stringType.asStringValue(), null));
        case "CanonicalType":
          return new org.hl7.fhir.r5.model.CanonicalType(stringType.asStringValue());
        default:
          break;
      }
    }

    return super.as(value, type, isStrict);
  }

}
