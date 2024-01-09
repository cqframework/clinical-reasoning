package org.opencds.cqf.fhir.cr.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessor.class);
    protected static final String EXCEPTION_MESSAGE_TEMPLATE =
            "Error encountered evaluating expression (%s) for item (%s): %s";

    public List<IBase> getExpressionResult(IOperationRequest request, CqfExpression expression, String itemLinkId)
            throws ResolveExpressionException {
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

    public List<IBase> getExpressionResult(IOperationRequest request, CqfExpression expression) {
        return request
                .getLibraryEngine()
                .resolveExpression(
                        request.getSubjectId().getIdPart(), expression, request.getParameters(), request.getBundle())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<IBase> getExpressionResult(
            IOperationRequest request, CqfExpression expression, IBaseParameters parameters) {
        return request
                .getLibraryEngine()
                .resolveExpression(request.getSubjectId().getIdPart(), expression, parameters, request.getBundle())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public <E extends IBaseExtension<?, ?>> CqfExpression getCqfExpression(
            IOperationRequest request, List<E> extensions, String extensionUrl) {
        var extension = extensions.stream()
                .filter(e -> e.getUrl().equals(extensionUrl))
                .findFirst()
                .orElse(null);
        if (extension == null) {
            return null;
        }
        switch (request.getFhirVersion()) {
            case DSTU3:
                var languageExtension = extensions.stream()
                        .filter(e -> e.getUrl().equals(Constants.CQF_EXPRESSION_LANGUAGE))
                        .findFirst()
                        .orElse(null);
                return new CqfExpression(
                        languageExtension.getValue().toString(),
                        extension.getValue().toString(),
                        request.getDefaultLibraryUrl());
            case R4:
                return CqfExpression.of(
                        (org.hl7.fhir.r4.model.Expression) extension.getValue(), request.getDefaultLibraryUrl());
            case R5:
                return CqfExpression.of(
                        (org.hl7.fhir.r5.model.Expression) extension.getValue(), request.getDefaultLibraryUrl());

            default:
                return null;
        }
    }

    public CqfExpression getCqfExpression(IOperationRequest request, IBaseBackboneElement element) {
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

    public CqfExpression getItemInitialExpression(IOperationRequest request, IBaseBackboneElement item) {
        if (!item.hasExtension()) {
            return null;
        }
        var cqfExpression = getCqfExpression(request, item.getExtension(), Constants.CQF_EXPRESSION);
        return cqfExpression != null
                ? cqfExpression
                : getCqfExpression(request, item.getExtension(), Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
    }
}
