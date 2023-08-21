package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;

class ParametersAdapter extends ResourceAdapter
    implements org.opencds.cqf.fhir.utility.adapter.ParametersAdapter {

  public ParametersAdapter(IBaseResource parameters) {
    super(parameters);

    if (!parameters.fhirType().equals("Parameters")) {
      throw new IllegalArgumentException(
          "resource passed as parameters argument is not a Parameters resource");
    }

    this.parameters = (Parameters) parameters;
  }

  private Parameters parameters;

  protected Parameters getParameters() {
    return this.parameters;
  }

  @Override
  public List<IBaseBackboneElement> getParameter() {
    return this.getParameters().getParameter().stream().collect(Collectors.toList());
  }

  @Override
  public void setParameter(List<IBaseBackboneElement> parametersParameterComponents) {
    this.getParameters().setParameter(parametersParameterComponents.stream()
        .map(x -> (ParametersParameterComponent) x).collect(Collectors.toList()));
  }

  @Override
  public IBaseBackboneElement addParameter() {
    return this.getParameters().addParameter();
  }


}
