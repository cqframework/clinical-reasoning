package org.opencds.cqf.fhir.utility.matcher;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.fhirpath.IFhirPath.IParsedExpression;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Timing;
import org.opencds.cqf.fhir.utility.FhirPathCache;

public class ResourceMatcherR4 implements ResourceMatcher {

    private Map<SPPathKey, IParsedExpression> pathCache = new HashMap<>();
    private Map<String, RuntimeSearchParam> searchParams = new HashMap<>();

    @Override
    public IFhirPath getEngine() {
        return FhirPathCache.cachedForVersion(FhirVersionEnum.R4);
    }

    @Override
    public FhirContext getContext() {
        return FhirContext.forR4Cached();
    }

    @Override
    public DateRangeParam getDateRange(ICompositeType type) {
        if (type instanceof Period period) {
            return new DateRangeParam(period.getStart(), period.getEnd());
        } else if (type instanceof Timing) {
            throw new NotImplementedException("Timing resolution has not yet been implemented");
        } else {
            throw new UnsupportedOperationException("Expected element of type Period or Timing, found "
                    + type.getClass().getSimpleName());
        }
    }

    @Override
    public List<TokenParam> getCodes(IBase codeElement) {
        List<TokenParam> resolvedCodes = new ArrayList<>();
        if (codeElement instanceof Coding c) {
            resolvedCodes.add(new TokenParam(c.getSystem(), c.getCode()));
        } else if (codeElement instanceof CodeType c) {
            resolvedCodes.add(new TokenParam(c.getValue()));
        } else if (codeElement instanceof CodeableConcept concept) {
            resolvedCodes = concept.getCoding().stream()
                    .map(code -> new TokenParam(code.getSystem(), code.getCode()))
                    .collect(Collectors.toList());
        }

        return resolvedCodes;
    }

    @Override
    public Map<SPPathKey, IParsedExpression> getPathCache() {
        return pathCache;
    }

    @Override
    public boolean isMatchToken(TokenParam param, IBase pathResult) {
        if (param.getValue() == null) {
            // TODO - should this be false?
            return true;
        }

        if (pathResult instanceof IIdType id) {
            return param.getValue().equals(id.getIdPart());
        }

        // [parameter]=[code]: the value of [code] matches a Coding.code or Identifier.value irrespective of the
        // value of the system property
        // [parameter]=[system]|[code]: the value of [code] matches a Coding.code or Identifier.value, and the value
        // of [system] matches the system property of the Identifier or Coding
        // [parameter]=|[code]: the value of [code] matches a Coding.code or Identifier.value, and the
        // Coding/Identifier has no system property
        // [parameter]=[system]|: any
        if (pathResult instanceof Identifier identifier) {
            var system = identifier.getSystem();
            var value = identifier.getValue();

            if (param.getSystem() != null
                    && param.getSystem().equals(system)
                    && param.getValue() != null
                    && param.getValue().equals(value)) {
                return true;
            } else if (param.getValue() != null && param.getValue().equals(value)) {
                return true;
            } else if (param.getSystem() != null && param.getSystem().equals(system)) {
                return true;
            } else {
                // this will never be reached because of the top null check
                return false;
            }
        }

        if (pathResult instanceof IBaseEnumeration<?> enumeration) {
            return param.getValue().equals(enumeration.getValueAsString());
        }

        if (pathResult instanceof IPrimitiveType<?> type) {
            return param.getValue().equals(type.getValue());
        }

        return false;
    }

    @Override
    public void addCustomParameter(RuntimeSearchParam searchParam) {
        this.searchParams.put(searchParam.getName(), searchParam);
    }

    @Override
    public Map<String, RuntimeSearchParam> getCustomParameters() {
        return this.searchParams;
    }
}
