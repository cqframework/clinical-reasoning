package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersParameterComponentAdapter;

// TODO: Move this to the Measure evaluation module sine that's where it's actually needed.

/**
 * This class maps the standard input parameters of the Measure evaluate operation to FHIR parameters
 */
public class OperationParametersParser {

    protected AdapterFactory adapterFactory;
    protected FhirTypeConverter fhirTypeConverter;

    public OperationParametersParser(AdapterFactory adapterFactory, FhirTypeConverter fhirTypeConverter) {
        this.adapterFactory = adapterFactory;
        this.fhirTypeConverter = fhirTypeConverter;
    } 

    public void addMeasurementPeriod(IBaseParameters parameters, String periodStart, String periodEnd) {
        requireNonNull(parameters);

        if (periodStart == null || periodEnd == null) {
            return;
        }

        ICompositeType measurementPeriodFhir = this.fhirTypeConverter.toFhirPeriod(new Interval(new Date(periodStart), true, new Date(periodEnd), true));

        this.addChild(parameters, "Measurement Period", measurementPeriodFhir);
    }

    public void addProductLine(IBaseParameters parameters, String productLine) {
        requireNonNull(parameters);

        if (productLine == null) {
            return;
        }

        IPrimitiveType<String> productLineFhir = this.fhirTypeConverter.toFhirString(productLine);

        this.addChild(parameters, "Product Line", productLineFhir);
    }

    protected void addChild(IBaseParameters parameters, String name, IBaseDatatype value) {
        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        List<ParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
            .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.toList());

        ParametersParameterComponentAdapter part = parts.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
        if (part == null) {
            part = this.adapterFactory.createParametersParameters(parametersAdapter.addParameter());
         
        }

        part.setName(name);
        part.setResource(null);
        part.setValue(value);
    }
}
