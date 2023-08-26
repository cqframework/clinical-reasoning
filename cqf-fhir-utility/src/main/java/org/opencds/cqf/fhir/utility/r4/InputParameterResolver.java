package org.opencds.cqf.fhir.utility.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

/**
 * This class provides the default parameters passed into an operation as CQL Resource parameters
 * for evaluation. e.g. "%subject"
 */
public class InputParameterResolver {
  private static final Logger logger = LoggerFactory.getLogger(InputParameterResolver.class);

  private final String subjectId;
  private final String encounterId;
  private final String practitionerId;
  private final Parameters parameters;
  private final Repository repository;

  public InputParameterResolver(String subjectId, String encounterId, String practitionerId,
      IBaseParameters parameters, Boolean useServerData, IBaseBundle bundle,
      Repository repository) {
    this.subjectId = subjectId;
    this.encounterId = encounterId;
    this.practitionerId = practitionerId;
    Repository bundleRepository = null;
    if (bundle != null) {
      bundleRepository = new InMemoryFhirRepository(repository.fhirContext(), bundle);
    }
    this.repository = resolveRepository(useServerData, repository, bundleRepository);
    this.parameters = resolveParameters(parameters);
  }

  private Repository resolveRepository(Boolean useServerData, Repository serverRepository,
      Repository bundleRepository) {
    if (bundleRepository == null) {
      return serverRepository;
    } else {
      return Boolean.TRUE.equals(useServerData)
          ? new FederatedRepository(serverRepository, bundleRepository)
          : bundleRepository;
    }
  }

  private FhirContext fhirContext() {
    return repository.fhirContext();
  }

  private <R extends IBaseResource> R readRepository(Class<R> resourceType, String id) {
    try {
      return repository.read(resourceType, new IdType(id), null);
    } catch (Exception e) {
      return null;
    }
  }

  private Parameters resolveParameters(IBaseParameters baseParameters) {
    var params = parameters();
    if (baseParameters != null) {
      params.getParameter().addAll(((Parameters) baseParameters).getParameter());
    }
    var subjectType = subjectId.contains("/") ? subjectId.split("/")[0] : "Patient";
    var subjectClass = fhirContext().getResourceDefinition(subjectType).getImplementingClass();
    var subject = readRepository(subjectClass, subjectId);
    if (subject != null) {
      params.addParameter(part("%subject", (Resource) subject));
    }
    if (encounterId != null && !encounterId.isEmpty()) {
      var encounter = readRepository(Encounter.class, encounterId);
      if (encounter != null) {
        params.addParameter(part("%encounter", encounter));
      }
    }
    if (practitionerId != null && !practitionerId.isEmpty()) {
      var practitioner = readRepository(Practitioner.class, practitionerId);
      if (practitioner != null) {
        params.addParameter(part("%practitioner", practitioner));
      }
    }
    return params;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public Parameters resolveInputParameters(List<DataRequirement> dataRequirements) {
    var params = parameters();
    if (parameters != null) {
      params.getParameter().addAll(parameters.getParameter());
    }
    for (var req : dataRequirements) {
      if (!req.hasId()) {
        continue;
      }

      var parameter = part("%" + String.format("%s", req.getId()));
      if (req.hasCodeFilter()) {
        for (var filter : req.getCodeFilter()) {
          if (filter != null && filter.hasPath() && filter.hasValueSet()) {
            var valueSets = repository.search(Bundle.class, ValueSet.class,
                Searches.byCanonical(filter.getValueSet()));
            if (valueSets.hasEntry()) {
              var valueSet = (ValueSet) valueSets.getEntryFirstRep().getResource();
              var codes =
                  valueSet.hasExpansion()
                      ? valueSet.getExpansion().getContains().stream()
                          .map(c -> new Coding(c.getSystem(), c.getCode(), c.getDisplay()))
                          .collect(Collectors.toList())
                      : new ArrayList<Coding>();

              var searchBuilder = Searches.builder();
              codes.forEach(c -> searchBuilder.withTokenParam("code", c.getCode()));
              var resourceType =
                  fhirContext().getResourceDefinition(req.getType()).getImplementingClass();
              var searchResults =
                  repository.search(Bundle.class, resourceType, searchBuilder.build(), null);
              if (searchResults.hasEntry()) {
                parameter.setResource(searchResults.getEntry().size() > 1 ? searchResults
                    : searchResults.getEntry().get(0).getResource());
              }
            }
            logger.debug("Could not find ValueSet with url {} on the local server.",
                filter.getValueSet());
          }
        }
      }
      params.addParameter(parameter);
    }
    return params;
  }
}
