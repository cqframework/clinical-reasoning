package org.opencds.cqf.cql.evaluator.evaluation;

import java.util.Collections;

import org.opencds.cqf.cql.evaluator.Response;
import org.opencds.cqf.cql.evaluator.Evaluator;
import org.opencds.cqf.cql.evaluator.ExpressionInfo;
import org.opencds.cqf.cql.evaluator.ParameterInfo;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.Parameters;

// This class is the start of something like "evaluate in context"
public class ExpressionEvaluator {

    public static Object evaluateExpression(IBaseResource resource, String expression) {
        if (resource == null) {
            throw new IllegalArgumentException("resource can not be null");
        }
        if (expression == null) {
            throw new IllegalArgumentException("expression can not be null");
        }

        String libraryContent = constructLocalLibrary(resource, expression);

        return evaluateLocalLibrary(resource, libraryContent);
    }

    // How should we handle the case that resource is null?
    // We need to know the type to create the library.
    private static String constructLocalLibrary(IBaseResource resource, String expression) {
        String resourceType = resource.fhirType();
        String fhirVersion = resource.getStructureFhirVersionEnum().getFhirVersionString();
        fhirVersion = fhirVersion.equals("3.0.2") || fhirVersion.equals("3.0.1") ? fhirVersion = "3.0.0" : fhirVersion;
        String source = String.format(
                "library LocalLibrary using FHIR version '%s' include FHIRHelpers version '%s' called FHIRHelpers parameter %s %s define Expression: %s",
                fhirVersion, fhirVersion, resourceType, resourceType, expression);

        return source;
    }

    private static Object evaluateLocalLibrary(IBaseResource resource, String libraryContent) {
        Parameters parameters = new Parameters();
        parameters.libraries = Collections.singletonList(libraryContent);
        parameters.expressions = Collections.singletonList(new ExpressionInfo("LocalLibrary", "Expression"));
        parameters.parameters = Collections.singletonList(new ParameterInfo(null, resource.fhirType(), resource));
        Evaluator service = new Evaluator();
        Response response = service.evaluate(parameters);

        return response.evaluationResult.forExpression("Expression");
    }
}