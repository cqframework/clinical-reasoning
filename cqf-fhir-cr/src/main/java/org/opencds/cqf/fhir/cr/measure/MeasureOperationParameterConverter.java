package org.opencds.cqf.fhir.cr.measure;

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
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

/**
 * This class maps the standard input parameters of the Measure evaluate operation to FHIR
 * parameters
 */
public class MeasureOperationParameterConverter {

    protected IAdapterFactory adapterFactory;
    protected FhirTypeConverter fhirTypeConverter;

    public MeasureOperationParameterConverter(IAdapterFactory adapterFactory, FhirTypeConverter fhirTypeConverter) {
        this.adapterFactory = adapterFactory;
        this.fhirTypeConverter = fhirTypeConverter;
    }

    public void addMeasurementPeriod(IBaseParameters parameters, String periodStart, String periodEnd) {
        requireNonNull(parameters);

        if (periodStart == null || periodEnd == null) {
            return;
        }

        ICompositeType measurementPeriodFhir = this.fhirTypeConverter.toFhirPeriod(
                new Interval(new Date(periodStart), true, new Date(periodEnd), true));

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
        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        List<IParametersParameterComponentAdapter> parts = parametersAdapter.getParameter().stream()
                .map(x -> this.adapterFactory.createParametersParameter(x))
                .collect(Collectors.toList());

        IParametersParameterComponentAdapter part =
                parts.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
        if (part == null) {
            part = this.adapterFactory.createParametersParameter(parametersAdapter.addParameter());
        }

        part.setName(name);
        part.setResource(null);
        part.setValue(value);
    }
}
