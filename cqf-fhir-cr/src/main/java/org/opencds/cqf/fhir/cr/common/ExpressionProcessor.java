package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessor.class);
    protected static final String EXCEPTION_MESSAGE_TEMPLATE =
            "Error encountered evaluating expression (%s) for item (%s): %s";

    /**
     * Returns the results of a given CqfExpression for an item
     *
     * @param request operation request with parameters
     * @param expression CqfExpression to evaluate
     * @param itemLinkId link Id of the item
     * @return
     * @throws ResolveExpressionException
     */
    public List<IBase> getExpressionResultForItem(
            IOperationRequest request, CqfExpression expression, String itemLinkId) throws ResolveExpressionException {
        if (expression == null) {
            return new ArrayList<>();
        }
        try {
            return getExpressionResult(request, expression);
        } catch (Exception ex) {
            final String message =
                    String.format(EXCEPTION_MESSAGE_TEMPLATE, expression.getExpression(), itemLinkId, ex.getMessage());
            throw new ResolveExpressionException(message);
        }
    }

    /**
     * Returns the results of a given CqfExpression
     *
     * @param request operation request with parameters
     * @param expression CqfExpression to evaluate
     * @return
     */
    public List<IBase> getExpressionResult(IOperationRequest request, CqfExpression expression) {
        return request
                .getLibraryEngine()
                .resolveExpression(
                        request.getSubjectId().getIdPart(), expression, request.getParameters(), request.getData())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns the results of a given CqfExpression with overridden parameters
     *
     * @param request operation request with parameters
     * @param expression CqfExpression to evaluate
     * @param parameters the parameters to use in place of the request parameters
     * @return
     */
    public List<IBase> getExpressionResult(
            IOperationRequest request, CqfExpression expression, IBaseParameters parameters) {
        return request
                .getLibraryEngine()
                .resolveExpression(request.getSubjectId().getIdPart(), expression, parameters, request.getData())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns a CqfExpression from a list of extensions filtered by the given url.  This is done against a list of extensions to support the lack of an Expression type in Dstu3.
     *
     * @param request operation request with parameters
     * @param extensions list of extensions to pull the CqfExpression from
     * @param extensionUrl the list of extensions will be filtered by this url
     * @return
     */
    public <E extends IBaseExtension<?, ?>> CqfExpression getCqfExpression(
            IOperationRequest request, List<E> extensions, String extensionUrl) {
        var extension = extensions.stream()
                .filter(e -> e.getUrl().equals(extensionUrl))
                .findFirst()
                .orElse(null);
        return extension == null ? null : CqfExpression.of(extension, request.getDefaultLibraryUrl());
        // if (extension == null) {
        //     return null;
        // }
        // switch (request.getFhirVersion()) {
        //     case DSTU3:
        //         // var languageExtension = extensions.stream()
        //         var languageExtension = extension.getExtension().stream()
        //                 .map(e -> (IBaseExtension<?, ?>) e)
        //                 .filter(e -> e.getUrl().equals(Constants.CQF_EXPRESSION_LANGUAGE))
        //                 .findFirst()
        //                 .orElse(null);
        //         // var libraryExtension = extensions.stream()
        //         var libraryExtension = extension.getExtension().stream()
        //                 .map(e -> (IBaseExtension<?, ?>) e)
        //                 .filter(e -> e.getUrl().equals(Constants.CQF_LIBRARY))
        //                 .findFirst()
        //                 .orElse(null);
        //         return new CqfExpression(
        //                 languageExtension == null
        //                         ? null
        //                         : languageExtension.getValue().toString(),
        //                 extension.getValue().toString(),
        //                 libraryExtension == null
        //                         ? request.getDefaultLibraryUrl()
        //                         : libraryExtension.getValue().toString());
        //     case R4:
        //         return CqfExpression.of(
        //                 (org.hl7.fhir.r4.model.Expression) extension.getValue(), request.getDefaultLibraryUrl());
        //     case R5:
        //         return CqfExpression.of(
        //                 (org.hl7.fhir.r5.model.Expression) extension.getValue(), request.getDefaultLibraryUrl());

        //     default:
        //         return null;
        // }
    }

    /**
     * Returns a CqfExpression from a given element that contains an Expression element.
     *
     * @param request operation request with parameters
     * @param element the element to pull the Expression element from
     * @return
     */
    public CqfExpression getCqfExpressionForElement(IOperationRequest request, IBaseBackboneElement element) {
        if (element == null) {
            return null;
        }
        switch (request.getFhirVersion()) {
            case DSTU3:
                return new CqfExpression(
                        request.resolvePathString(element, "language"),
                        request.resolvePathString(element, "expression"),
                        request.getDefaultLibraryUrl());
            case R4:
                return CqfExpression.of(
                        request.resolvePath(element, "expression", org.hl7.fhir.r4.model.Expression.class),
                        request.getDefaultLibraryUrl());
            case R5:
                return CqfExpression.of(
                        request.resolvePath(element, "expression", org.hl7.fhir.r5.model.Expression.class),
                        request.getDefaultLibraryUrl());

            default:
                return null;
        }
    }

    /**
     * Returns a CqfExpression for the initial expression of a given item with an SDC Initial Expression Extension
     * "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"
     *
     * @param request operation request with parameters
     * @param item the item
     * @return
     */
    public CqfExpression getItemInitialExpression(IOperationRequest request, IBaseBackboneElement item) {
        if (!item.hasExtension()) {
            return null;
        }
        var expressionExtensionUrl = request.getFhirVersion() == FhirVersionEnum.DSTU3
                ? Constants.CQIF_CQL_EXPRESSION
                : Constants.CQF_EXPRESSION;
        var cqfExpression = getCqfExpression(request, item.getExtension(), expressionExtensionUrl);
        return cqfExpression != null
                ? cqfExpression
                : getCqfExpression(request, item.getExtension(), Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
    }
}
