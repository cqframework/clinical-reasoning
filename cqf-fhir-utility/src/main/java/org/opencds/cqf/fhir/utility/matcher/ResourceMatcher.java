package org.opencds.cqf.fhir.utility.matcher;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.fhirpath.IFhirPath.IParsedExpression;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.param.UriParam;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public interface ResourceMatcher {

    public static class SPPathKey {
        private final String resourceType;
        private final String resourcePath;

        public SPPathKey(String resourceType, String resourcePath) {
            this.resourceType = resourceType;
            this.resourcePath = resourcePath;
        }

        public String path() {
            return resourcePath;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
            result = prime * result + ((resourcePath == null) ? 0 : resourcePath.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            SPPathKey or = (SPPathKey) obj;
            if (resourceType == null) {
                if (or.resourceType != null) return false;
            } else if (!resourceType.equals(or.resourceType)) return false;
            if (resourcePath == null) {
                if (or.resourcePath != null) return false;
            } else if (!resourcePath.equals(or.resourcePath)) return false;
            return true;
        }
    }

    public IFhirPath getEngine();

    public FhirContext getContext();

    public Map<SPPathKey, IParsedExpression> getPathCache();

    // The list here is an OR list. Meaning, if any element matches it's a match
    default boolean matches(String name, List<IQueryParameterType> params, IBaseResource resource) {
        boolean match = true;

        var context = getContext();
        var s = context.getResourceDefinition(resource).getSearchParam(name);
        if (s == null) {
            throw new RuntimeException(String.format(
                    "The SearchParameter %s for Resource %s is not supported.", name, resource.fhirType()));
        }

        var path = s.getPath();

        // System search parameters...
        if (path.isEmpty() && name.startsWith("_")) {
            path = name.substring(1);
        }

        List<IBase> pathResult = null;
        try {
            var parsed = getPathCache().computeIfAbsent(new SPPathKey(resource.fhirType(), path), p -> {
                try {
                    return getEngine().parse(p.path());
                } catch (Exception e) {
                    throw new RuntimeException(
                            String.format(
                                    "Parsing SearchParameter %s for Resource %s resulted in an error.",
                                    name, resource.fhirType()),
                            e);
                }
            });
            pathResult = getEngine().evaluate(resource, parsed, IBase.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "Evaluating SearchParameter %s for Resource %s resulted in an error.",
                            name, resource.fhirType()),
                    e);
        }

        if (pathResult == null || pathResult.isEmpty()) {
            return false;
        }

        for (IQueryParameterType param : params) {
            for (var r : pathResult) {
                if (param instanceof ReferenceParam) {
                    match = isMatchReference(param, r);
                } else if (param instanceof DateParam) {
                    match = isMatchDate((DateParam) param, r);
                } else if (param instanceof TokenParam) {
                    match = isMatchToken((TokenParam) param, r);
                    if (!match) {
                        var codes = getCodes(r);
                        match = isMatchCoding((TokenParam) param, r, codes);
                    }
                } else if (param instanceof UriParam) {
                    match = isMatchUri((UriParam) param, r);
                } else if (param instanceof StringParam) {
                    match = isMatchString((StringParam) param, r);
                } else {
                    throw new NotImplementedException("Resource matching not implemented for search params of type "
                            + param.getClass().getSimpleName());
                }

                if (match) {
                    return true;
                }
            }
        }

        return false;
    }

    default boolean isMatchReference(IQueryParameterType param, IBase pathResult) {
        if (pathResult instanceof IBaseReference) {
            return ((IBaseReference) pathResult)
                    .getReferenceElement()
                    .getValue()
                    .equals(((ReferenceParam) param).getValue());
        } else if (pathResult instanceof IPrimitiveType) {
            return ((IPrimitiveType<?>) pathResult).getValueAsString().equals(((ReferenceParam) param).getValue());
        } else if (pathResult instanceof Iterable) {
            for (var element : (Iterable<?>) pathResult) {
                if (element instanceof IBaseReference
                        && ((IBaseReference) element)
                                .getReferenceElement()
                                .getValue()
                                .equals(((ReferenceParam) param).getValue())) {
                    return true;
                }
                if (element instanceof IPrimitiveType
                        && ((IPrimitiveType<?>) element)
                                .getValueAsString()
                                .equals(((ReferenceParam) param).getValue())) {
                    return true;
                }
            }
        } else {
            throw new UnsupportedOperationException(
                    "Expected Reference element, found " + pathResult.getClass().getSimpleName());
        }
        return false;
    }

    default boolean isMatchDate(DateParam param, IBase pathResult) {
        DateRangeParam dateRange;
        // date, dateTime and instant are PrimitiveType<Date>
        if (pathResult instanceof IPrimitiveType) {
            var result = ((IPrimitiveType<?>) pathResult).getValue();
            if (result instanceof Date) {
                dateRange = new DateRangeParam((Date) result, (Date) result);
            } else {
                throw new UnsupportedOperationException(
                        "Expected date, found " + pathResult.getClass().getSimpleName());
            }
        } else if (pathResult instanceof ICompositeType) {
            dateRange = getDateRange((ICompositeType) pathResult);
        } else {
            throw new UnsupportedOperationException(
                    "Expected element of type date, dateTime, instant, Timing or Period, found "
                            + pathResult.getClass().getSimpleName());
        }
        return matchesDateBounds(dateRange, new DateRangeParam(param));
    }

    default boolean isMatchToken(TokenParam param, IBase pathResult) {
        if (param.getValue() == null) {
            return true;
        }

        if (pathResult instanceof IIdType) {
            var id = (IIdType) pathResult;
            return param.getValue().equals(id.getIdPart());
        }

        if (pathResult instanceof IBaseEnumeration) {
            return param.getValue().equals(((IBaseEnumeration<?>) pathResult).getValueAsString());
        }

        if (pathResult instanceof IPrimitiveType) {
            return param.getValue().equals(((IPrimitiveType<?>) pathResult).getValue());
        }

        return false;
    }

    default boolean isMatchCoding(TokenParam param, IBase pathResult, List<TokenParam> codes) {
        if (codes == null || codes.isEmpty()) {
            return false;
        }

        if (param.getModifier() == TokenParamModifier.IN) {
            throw new UnsupportedOperationException("In modifier is unsupported");
        }

        for (var c : codes) {
            var matches = param.getValue().equals(c.getValue())
                    && (param.getSystem() == null || param.getSystem().equals(c.getSystem()));
            if (matches) {
                return true;
            }
        }

        return false;
    }

    default boolean isMatchUri(UriParam param, IBase pathResult) {
        if (pathResult instanceof IPrimitiveType) {
            return param.getValue().equals(((IPrimitiveType<?>) pathResult).getValue());
        }
        throw new UnsupportedOperationException("Expected element of type url or uri, found "
                + pathResult.getClass().getSimpleName());
    }

    default boolean isMatchString(StringParam param, Object pathResult) {
        if (pathResult instanceof IPrimitiveType) {
            return param.getValue().equals(((IPrimitiveType<?>) pathResult).getValue());
        }
        throw new UnsupportedOperationException("Expected element of type string, found "
                + pathResult.getClass().getSimpleName());
    }

    default boolean matchesDateBounds(DateRangeParam resourceRange, DateRangeParam paramRange) {
        Date resourceLowerBound = resourceRange.getLowerBoundAsInstant();
        Date resourceUpperBound = resourceRange.getUpperBoundAsInstant();
        Date paramLowerBound = paramRange.getLowerBoundAsInstant();
        Date paramUpperBound = paramRange.getUpperBoundAsInstant();
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

    DateRangeParam getDateRange(ICompositeType type);

    List<TokenParam> getCodes(IBase codeElement);
}
