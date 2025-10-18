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
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.Timing;
import org.opencds.cqf.fhir.utility.FhirPathCache;

public class ResourceMatcherR5 implements ResourceMatcher {

    private Map<SPPathKey, IParsedExpression> pathCache = new HashMap<>();
    private Map<String, RuntimeSearchParam> searchParams = new HashMap<>();

    @Override
    public IFhirPath getEngine() {
        return FhirPathCache.cachedForVersion(FhirVersionEnum.R5);
    }

    @Override
    public FhirContext getContext() {
        return FhirContext.forR5Cached();
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
    public void addCustomParameter(RuntimeSearchParam searchParam) {
        this.searchParams.put(searchParam.getName(), searchParam);
    }

    @Override
    public Map<String, RuntimeSearchParam> getCustomParameters() {
        return this.searchParams;
    }
}
