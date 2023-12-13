package org.opencds.cqf.fhir.cr.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessor.class);
    protected static final String EXCEPTION_MESSAGE_TEMPLATE =
            "Error encountered evaluating expression (%s) for item (%s): %s";

    @SuppressWarnings("unchecked")
    public List<IBase> getExpressionResultForItem(IOperationRequest request, IBaseBackboneElement item)
            throws ResolveExpressionException {
        var expression = getInitialExpression(request, item);
        var itemLinkId = ((IPrimitiveType<String>) request.getModelResolver().resolvePath(item, "linkId")).getValue();
        return getExpressionResult(request, expression, itemLinkId);
    }

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
                // final IBaseExtension<?, ?> languageExtension = getExtensionByUrl(item,
                // Constants.CQF_EXPRESSION_LANGUAGE);
                return new CqfExpression(
                        languageExtension.getValue().toString(),
                        extension.getValue().toString(),
                        request.getDefaultLibraryUrl());
            case R4:
                return new CqfExpression(
                        (org.hl7.fhir.r4.model.Expression) extension.getValue(), request.getDefaultLibraryUrl(), null);
            case R5:
                return new CqfExpression(
                        (org.hl7.fhir.r5.model.Expression) extension.getValue(), request.getDefaultLibraryUrl(), null);

            default:
                return null;
        }
    }

    protected CqfExpression getInitialExpression(IOperationRequest request, IBaseBackboneElement item) {
        if (!item.hasExtension()) {
            return null;
        }
        var cqfExpression = getCqfExpression(request, item.getExtension(), Constants.CQF_EXPRESSION);
        return cqfExpression != null
                ? cqfExpression
                : getCqfExpression(request, item.getExtension(), Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
    }
}
