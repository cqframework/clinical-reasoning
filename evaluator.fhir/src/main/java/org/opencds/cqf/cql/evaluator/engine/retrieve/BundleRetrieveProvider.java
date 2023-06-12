package org.opencds.cqf.cql.evaluator.engine.retrieve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;

public class BundleRetrieveProvider extends RetrieveProvider {

  private final Map<String, List<IBaseResource>> resourceMap = new HashMap<>();

  public BundleRetrieveProvider(final FhirContext fhirContext, final IBaseBundle iBaseBundle) {
    super(fhirContext);
    var resources = BundleUtil.toListOfResources(fhirContext, iBaseBundle);
    for (var r : resources) {
      resourceMap.computeIfAbsent(r.fhirType(), k -> new ArrayList<>()).add(r);
    }
  }

  @Override
  public Iterable<Object> retrieve(final String context, final String contextPath,
      final Object contextValue, final String dataType, final String templateId,
      final String codePath, final Iterable<Code> codes, final String valueSet,
      final String datePath, final String dateLowPath, final String dateHighPath,
      final Interval dateRange) {

    return resourceMap.computeIfAbsent(dataType, k -> Collections.emptyList()).stream()
        .filter(filterByTemplateId(dataType, templateId))
        .filter(filterByContext(dataType, context, contextPath, contextValue))
        .filter(filterByTerminology(dataType, codePath, codes, valueSet))
        .collect(Collectors.<Object>toList());
  }
}
