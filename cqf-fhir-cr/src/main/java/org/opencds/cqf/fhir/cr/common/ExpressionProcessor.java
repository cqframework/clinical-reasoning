package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessor.class);
    protected static final String EXCEPTION_MESSAGE_TEMPLATE =
            "Error encountered evaluating expression (%s) for item (%s): %s";

    /**
     * Returns the results of a given CqfExpression for an item using a CqlOperationRequest
     *
     * @param request operation request with parameters
     * @param expression CqfExpression to evaluate
     * @param itemLinkId link Id of the item
     * @return the results of the expression
     */
    public List<IBase> getExpressionResultForItem(
            ICqlOperationRequest request,
            CqfExpression expression,
            String itemLinkId,
            IBaseParameters parameters,
            Map<String, Object> rawParameters) {
        try {
            return getExpressionResult(request, expression, parameters, rawParameters);
        } catch (Exception ex) {
            final String message =
                    EXCEPTION_MESSAGE_TEMPLATE.formatted(expression.getExpression(), itemLinkId, ex.getMessage());
            throw new UnprocessableEntityException(message);
        }
    }

    /**
     * Returns the results of a given CqfExpression using a CqlOperationRequest
     *
     * @param request operation request with parameters
     * @param expression CqfExpression to evaluate
     * @return the results of the expression
     */
    public List<IBase> getExpressionResult(ICqlOperationRequest request, CqfExpression expression) {
        return getExpressionResult(request, expression, null, null);
    }

    /**
     * Returns the results of a given CqfExpression with overridden parameters
     *
     * @param request operation request with parameters
     * @param expression CqfExpression to evaluate
     * @param parameters the parameters to use in place of the request parameters
     * @param rawParameters the raw CQL parameters to use in place of the request raw parameters
     * @return the results of the expression
     */
    public List<IBase> getExpressionResult(
            ICqlOperationRequest request,
            CqfExpression expression,
            @Nullable IBaseParameters parameters,
            @Nullable Map<String, Object> rawParameters) {
        parameters = parameters == null ? request.getParameters() : parameters;
        rawParameters = rawParameters == null ? request.getRawParameters() : rawParameters;
        var result = expression == null
                ? null
                : request.getLibraryEngine()
                        .resolveExpression(
                                request.getSubjectId().getIdPart(),
                                expression,
                                parameters,
                                rawParameters,
                                request.getData(),
                                request.getContextVariable(),
                                request.getResourceVariable());
        return result == null
                ? new ArrayList<>()
                : result.stream()
                        // Missing values are returned as a BooleanType with a null value which can cause
                        // problems when the consumer of the result is expecting a specific result type,
                        // so we are filtering out primitive types with no value.
                        .map(r -> r instanceof IPrimitiveType<?> primitiveType && primitiveType.getValue() == null
                                ? null
                                : r)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    /**
     * Returns a CqfExpression from a list of extensions filtered by the given url.
     *
     * @param request operation request with parameters
     * @param extensions list of extensions to pull the CqfExpression from
     * @param extensionUrl the list of extensions will be filtered by this url
     * @return a CqfExpression
     */
    public <E extends IBaseExtension<?, ?>> CqfExpression getCqfExpression(
            IOperationRequest request, List<E> extensions, String extensionUrl) {
        var extension = extensions.stream()
                .filter(e -> e.getUrl().equals(extensionUrl))
                .findFirst()
                .orElse(null);
        return extension == null ? null : CqfExpression.of(extension, request.getReferencedLibraries());
    }

    /**
     * Returns a CqfExpression from a given element that contains an Expression element.
     *
     * @param request operation request with parameters
     * @param element the element to pull the Expression element from
     * @return a CqfExpression
     */
    public CqfExpression getCqfExpressionForElement(IOperationRequest request, IBaseBackboneElement element) {
        if (element == null) {
            return null;
        }
        String expressionPath = "expression";
        return switch (request.getFhirVersion()) {
            case DSTU3 ->
                new CqfExpression(
                        request.resolvePathString(element, "language"),
                        request.resolvePathString(element, expressionPath),
                        request.getReferencedLibraries());
            case R4 ->
                CqfExpression.of(
                        request.resolvePath(element, expressionPath, org.hl7.fhir.r4.model.Expression.class),
                        request.getReferencedLibraries());
            case R5 ->
                CqfExpression.of(
                        request.resolvePath(element, expressionPath, org.hl7.fhir.r5.model.Expression.class),
                        request.getReferencedLibraries());
            default -> null;
        };
    }

    /**
     * Returns a CqfExpression for the initial expression of a given item with an SDC Initial Expression Extension
     * "<a href="http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression">...</a>"
     *
     * @param request operation request with parameters
     * @param item the item
     * @return a CqfExpression
     */
    public CqfExpression getItemInitialExpression(IOperationRequest request, IQuestionnaireItemComponentAdapter item) {
        if (!item.hasExtension()) {
            return null;
        }
        var cqfExpression = getCqfExpression(request, item.getExtension(), Constants.CQF_EXPRESSION);
        return cqfExpression != null
                ? cqfExpression
                : getCqfExpression(request, item.getExtension(), Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
    }
}
