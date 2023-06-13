package org.opencds.cqf.cql.evaluator.fhir.repository.matcher;

import ca.uhn.fhir.model.base.composite.BaseCodingDt;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.InternalCodingDt;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Timing;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceMatcherR4 implements BaseResourceMatcher {
  @Override
  public ModelResolver getModelResolver() {
    return new R4FhirModelResolver();
  }

  @Override
  public DateRangeParam getDateRange(ICompositeType type) {
    if (type instanceof Period) {
      return new DateRangeParam(((Period) type).getStart(), ((Period) type).getEnd());
    }
    else if (type instanceof Timing) {
      throw new NotImplementedException("Timing resolution has not yet been implemented");
    }
    else {
      throw new UnsupportedOperationException(
          "Expected element of type Period or Timing, found "
              + type.getClass().getSimpleName());
    }
  }

  @Override
  public List<BaseCodingDt> getCodes(Object codeElement) {
    List<BaseCodingDt> resolvedCodes = new ArrayList<>();
    if (codeElement instanceof Coding) {
      resolvedCodes.add(new InternalCodingDt(((Coding) codeElement).getSystem(),
          ((Coding) codeElement).getCode()));
    }
    else if (codeElement instanceof CodeType) {
      resolvedCodes.add(new InternalCodingDt(((CodeType) codeElement).getSystem(),
          ((CodeType) codeElement).getCode()));
    }
    else if (codeElement instanceof CodeableConcept) {
      resolvedCodes = ((CodeableConcept) codeElement).getCoding().stream()
          .map(code -> new InternalCodingDt(
              code.getSystem(), code.getCode())).collect(Collectors.toList());
    }
    else {
      // unsupported
      throw new UnsupportedOperationException(
          "Expected element of type Coding, CodeableConcept or code, found "
              + codeElement.getClass().getSimpleName());
    }
    return resolvedCodes;
  }

  @Override
  public boolean inValueSet(List<BaseCodingDt> codes) {
    throw new UnsupportedOperationException("InValueSet operation is not available");
  }
}
