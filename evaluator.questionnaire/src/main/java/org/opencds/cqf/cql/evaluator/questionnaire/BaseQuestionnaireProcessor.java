package org.opencds.cqf.cql.evaluator.questionnaire;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.util.ParametersUtil;
import org.hl7.fhir.instance.model.api.*;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public abstract class BaseQuestionnaireProcessor<T> {
    protected static final Logger logger = LoggerFactory.getLogger(BaseQuestionnaireProcessor.class);

    protected LibraryProcessor libraryProcessor;
    protected ExpressionEvaluator expressionEvaluator;
    protected FhirContext fhirContext;
    protected FhirDal fhirDal;
    protected IFhirPath fhirPath;

    protected String patientId;
    protected IBaseParameters parameters;
    protected IBaseBundle bundle;
    protected IBaseResource dataEndpoint;
    protected IBaseResource contentEndpoint;
    protected IBaseResource terminologyEndpoint;

    public BaseQuestionnaireProcessor(
            FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor,
            ExpressionEvaluator expressionEvaluator) {
        this.fhirContext = fhirContext;
        this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
        this.fhirDal = fhirDal;
        this.libraryProcessor = libraryProcessor;
        this.expressionEvaluator = expressionEvaluator;
    }

    public abstract T prePopulate(T questionnaire, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint);
    public abstract IBaseResource populate(T questionnaire, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint);
    public abstract T generateQuestionnaire(String theId, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint);
    // public abstract IBackboneElement generateItem(ICompositeType actionInput, Integer itemCount);
    public abstract Object resolveParameterValue(IBase value);
    public abstract IBaseResource getSubject();

    public IBase getExpressionResult(
            String expression, String language, String libraryToBeEvaluated, IBaseParameters params) {
        validateExpression(language, expression, libraryToBeEvaluated);
        IBase result = null;
        IBaseParameters parametersResult;
        switch (language) {
            case "text/cql":
            case "text/cql.expression":
            case "text/cql-expression":
                parametersResult = expressionEvaluator.evaluate(expression, params);
                // The expression is assumed to be the parameter component name
                // The expression evaluator creates a library with a single expression defined as "return"
                expression = "return";
                result = (IBase) resolveParameterValue(ParametersUtil.getNamedParameter(
                        fhirContext, parametersResult, expression).orElse(null));
                break;
            case "text/cql-identifier":
            case "text/cql.identifier":
            case "text/cql.name":
            case "text/cql-name":
                parametersResult = libraryProcessor.evaluate(libraryToBeEvaluated, patientId, parameters,
                        contentEndpoint, terminologyEndpoint, dataEndpoint, bundle, Collections.singleton(expression));
                result = (IBase) resolveParameterValue(ParametersUtil.getNamedParameter(
                        fhirContext, parametersResult, expression).orElse(null));
                break;
            case "text/fhirpath":
                List<IBase> outputs;
                try {
                    outputs = fhirPath.evaluate(getSubject(), expression, IBase.class);
                } catch (FhirPathExecutionException e) {
                    throw new IllegalArgumentException("Error evaluating FHIRPath expression", e);
                }
                if (outputs != null && outputs.size() == 1) {
                    result = outputs.get(0);
                } else {
                    throw new IllegalArgumentException(
                            "Expected only one value when evaluating FHIRPath expression: " + expression);
                }
                break;
            default:
                logger.warn("An action language other than CQL was found: {}", language);
        }

        return result;
    }

    public void validateExpression(String language, String expression, String libraryUrl) {
        if (language == null) {
            logger.error("Missing language type for the Expression");
            throw new IllegalArgumentException("Missing language type for the Expression");
        } else if (expression == null) {
            logger.error("Missing expression for the Expression");
            throw new IllegalArgumentException("Missing expression for the Expression");
        } else if (libraryUrl == null) {
            logger.error("Missing library for the Expression");
            throw new IllegalArgumentException("Missing library for the Expression");
        }
    }
}
