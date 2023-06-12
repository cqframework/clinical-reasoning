package org.opencds.cqf.cql.evaluator.fhir.helper.matcher;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.base.composite.BaseCodingDt;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import java.util.Date;
import java.util.List;

public abstract class BaseResourceMatcher {

  public boolean matches(String path, List<IQueryParameterType> params, IBaseResource resource) {
    boolean match = false;
    Object pathResult = getModelResolver().resolvePath(resource, path);
    List<BaseCodingDt> resourcePathCodes = null;
    if (pathResult == null && !path.equals("_profile")) {
      return false;
    }
    // Template (profile), Context, Terminology, and Date
    for (IQueryParameterType param : params) {
      // Template and Context
      if (param instanceof ReferenceParam) {
        // ASSUMPTION: will always be a singleton list
        if (path.equals("_profile")) {
          return resource.getMeta().getProfile().stream().anyMatch(profile -> profile.getValueAsString().equals(((ReferenceParam) param).getValue()));
        }
        else if (pathResult instanceof IBaseReference) {
          return ((IBaseReference) pathResult).getReferenceElement().getValue().equals(((ReferenceParam) param).getValue());
        }
        else if (pathResult instanceof Iterable) {
          for (var element : (Iterable<?>) pathResult) {
            if (element instanceof IBaseReference &&
                ((IBaseReference) element).getReferenceElement().getValue().equals(
                    ((ReferenceParam) param).getValue())) {
              return true;
            }
          }
        }
        else {
          throw new UnsupportedOperationException("Expected Reference element, found " + pathResult.getClass().getSimpleName());
        }
      }
      // Date and Date ranges
      else if (param instanceof DateParam) {
        // date, dateTime, instant, Timing, Period
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
        match = matchesDateBounds(dateRange, new DateRangeParam((DateParam) param));
      }
      // Terminology - in value set
      else if (param instanceof TokenParam) {
        TokenParam tp = (TokenParam) param;
        // in value set
        if (tp.getModifier() == TokenParamModifier.IN) {
          // TODO - not sure what to do here... when populating search params, this is used when
          //  there is no terminology service provided or expanding value sets is not supported
          if (resourcePathCodes == null) {
            resourcePathCodes = getCodes(pathResult);
          }
          if (inValueSet(resourcePathCodes)) {
            match = true;
          }
        }
      }
      // Terminology - match codes
      else if (param instanceof BaseCodingDt) {
        if (resourcePathCodes == null) {
          resourcePathCodes = getCodes(pathResult);
        }
        if (resourcePathCodes.stream().anyMatch(((BaseCodingDt) param)::matchesToken)) {
          match = true;
        }
      }
    }
    return match;
  }

  public boolean matchesDateBounds(DateRangeParam theResourceRange, DateRangeParam theParamRange) {
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

  public abstract ModelResolver getModelResolver();
  public abstract DateRangeParam getDateRange(ICompositeType type);
  public abstract List<BaseCodingDt> getCodes(Object codeElement);
  public abstract boolean inValueSet(List<BaseCodingDt> codes);

}
