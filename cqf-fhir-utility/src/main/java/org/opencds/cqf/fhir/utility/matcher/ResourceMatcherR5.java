package org.opencds.cqf.fhir.utility.matcher;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.fhirpath.IFhirPath.IParsedExpression;
import ca.uhn.fhir.model.base.composite.BaseCodingDt;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.InternalCodingDt;
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
        if (type instanceof Period) {
            return new DateRangeParam(((Period) type).getStart(), ((Period) type).getEnd());
        } else if (type instanceof Timing) {
            throw new NotImplementedException("Timing resolution has not yet been implemented");
        } else {
            throw new UnsupportedOperationException("Expected element of type Period or Timing, found "
                    + type.getClass().getSimpleName());
        }
    }

    @Override
    public List<BaseCodingDt> getCodes(IBase codeElement) {
        List<BaseCodingDt> resolvedCodes = new ArrayList<>();
        if (codeElement instanceof Coding) {
            resolvedCodes.add(
                    new InternalCodingDt(((Coding) codeElement).getSystem(), ((Coding) codeElement).getCode()));
        } else if (codeElement instanceof CodeType) {
            resolvedCodes.add(
                    new InternalCodingDt(((CodeType) codeElement).getSystem(), ((CodeType) codeElement).getCode()));
        } else if (codeElement instanceof CodeableConcept) {
            resolvedCodes = ((CodeableConcept) codeElement)
                    .getCoding().stream()
                            .map(code -> new InternalCodingDt(code.getSystem(), code.getCode()))
                            .collect(Collectors.toList());
        } else {
            return null;
        }
        return resolvedCodes;
    }

    @Override
    public boolean inValueSet(List<BaseCodingDt> codes) {
        throw new UnsupportedOperationException("InValueSet operation is not available");
    }

    @Override
    public Map<SPPathKey, IParsedExpression> getPathCache() {
        return pathCache;
    }
}
