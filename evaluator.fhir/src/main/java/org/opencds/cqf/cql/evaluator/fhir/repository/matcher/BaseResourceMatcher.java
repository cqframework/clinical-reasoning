package org.opencds.cqf.cql.evaluator.fhir.repository.matcher;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.base.composite.BaseCodingDt;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.param.UriParam;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public interface BaseResourceMatcher {
  default boolean matches(String path, List<IQueryParameterType> params, IBaseResource resource) {
    boolean match = false;
    var pathResult = path.equals("_profile") ? resource.getMeta().getProfile()
        : getModelResolver().resolvePath(resource, path);
    if (pathResult == null) {
      return false;
    }
    List<BaseCodingDt> resourcePathCodes = new ArrayList<>();
    for (IQueryParameterType param : params) {
      if (param instanceof ReferenceParam) {
        match = isMatchReference(param, pathResult);
      }
      else if (param instanceof DateParam) {
        match = isMatchDate((DateParam) param, pathResult);
      }
      else if (param instanceof TokenParam) {
        match = isMatchToken((TokenParam) param, pathResult, resourcePathCodes);
      }
      else if (param instanceof BaseCodingDt) {
        if (isMatchCoding((BaseCodingDt) param, pathResult, resourcePathCodes)) {
          return true;
        }
      }
      else if (param instanceof UriParam) {
        match = isMatchUri((UriParam) param, pathResult);
      }
      else {
        throw new NotImplementedException(
            "Resource matching not implemented for search params of type "
                + param.getClass().getSimpleName());
      }
    }
    return match;
  }

  default boolean isMatchReference(IQueryParameterType param, Object pathResult) {
    if (pathResult instanceof IBaseReference) {
      return ((IBaseReference) pathResult).getReferenceElement().getValue().equals(((ReferenceParam) param).getValue());
    }
    else if (pathResult instanceof IPrimitiveType) {
      return ((IPrimitiveType<?>) pathResult).getValueAsString().equals(((ReferenceParam) param).getValue());
    }
    else if (pathResult instanceof Iterable) {
      for (var element : (Iterable<?>) pathResult) {
        if (element instanceof IBaseReference &&
            ((IBaseReference) element).getReferenceElement().getValue().equals(
                ((ReferenceParam) param).getValue())) {
          return true;
        }
        if (element instanceof IPrimitiveType &&
            ((IPrimitiveType<?>) element).getValueAsString().equals(
                ((ReferenceParam) param).getValue())) {
          return true;
        }
      }
    }
    else {
      throw new UnsupportedOperationException("Expected Reference element, found " + pathResult.getClass().getSimpleName());
    }
    return false;
  }

  default boolean isMatchDate(DateParam param, Object pathResult) {
    DateRangeParam dateRange;
    // date, dateTime and instant are PrimitiveType<Date>
    if (pathResult instanceof IPrimitiveType) {
      var result = ((IPrimitiveType<?>) pathResult).getValue();
      if (result instanceof Date) {
        dateRange = new DateRangeParam((Date) result, (Date) result);
      }
      else {
        throw new UnsupportedOperationException(
            "Expected date, found " + pathResult.getClass().getSimpleName());
      }
    }
    else if (pathResult instanceof ICompositeType) {
      dateRange = getDateRange((ICompositeType) pathResult);
    }
    else {
      throw new UnsupportedOperationException(
          "Expected element of type date, dateTime, instant, Timing or Period, found "
              + pathResult.getClass().getSimpleName());
    }
    return matchesDateBounds(dateRange, new DateRangeParam(param));
  }

  default boolean isMatchToken(TokenParam param, Object pathResult,
      List<BaseCodingDt> resourcePathCodes) {
    if (param.getValue() == null) {
      return true;
    }
    // in value set
    if (param.getModifier() == TokenParamModifier.IN) {
      if (resourcePathCodes.isEmpty()) {
        resourcePathCodes = getCodes(pathResult);
      }
      return inValueSet(resourcePathCodes);
    }
    return false;
  }

  default boolean isMatchCoding(BaseCodingDt param, Object pathResult,
      List<BaseCodingDt> resourcePathCodes) {
    if (resourcePathCodes.isEmpty()) {
      resourcePathCodes = getCodes(pathResult);
    }
    return resourcePathCodes.stream().anyMatch((param)::matchesToken);
  }

  default boolean isMatchUri(UriParam param, Object pathResult) {
    if (pathResult instanceof IPrimitiveType) {
      return param.getValue().equals(((IPrimitiveType<?>) pathResult).getValue());
    }
    throw new UnsupportedOperationException(
        "Expected element of type url or uri, found " + pathResult.getClass().getSimpleName());
  }

  default boolean matchesDateBounds(DateRangeParam theResourceRange, DateRangeParam theParamRange) {
    Date resourceLowerBound = theResourceRange.getLowerBoundAsInstant();
    Date resourceUpperBound = theResourceRange.getUpperBoundAsInstant();
    Date paramLowerBound = theParamRange.getLowerBoundAsInstant();
    Date paramUpperBound = theParamRange.getUpperBoundAsInstant();
    if (paramLowerBound == null && paramUpperBound == null) {
      return false;
    } else {
      boolean result = true;
      if (paramLowerBound != null) {
        result &= resourceLowerBound.after(paramLowerBound) || resourceLowerBound.equals(paramLowerBound);
        result &= resourceUpperBound.after(paramLowerBound) || resourceUpperBound.equals(paramLowerBound);
      }

      if (paramUpperBound != null) {
        result &= resourceLowerBound.before(paramUpperBound) || resourceLowerBound.equals(paramUpperBound);
        result &= resourceUpperBound.before(paramUpperBound) || resourceUpperBound.equals(paramUpperBound);
      }

      return result;
    }
  }

  ModelResolver getModelResolver();
  DateRangeParam getDateRange(ICompositeType type);
  List<BaseCodingDt> getCodes(Object codeElement);
  boolean inValueSet(List<BaseCodingDt> codes);
}
