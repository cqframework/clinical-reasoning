package org.opencds.cqf.fhir.utility.fhirpath;

import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.fhirpath.IFhirPathEvaluationContext;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;

public class CrFhirPath implements IFhirPath {
    private final IFhirPath inner;

    public CrFhirPath(IFhirPath inner) {
        this.inner = inner;
    }

    //    public <T extends IBase> List<T> evaluate() {
    //
    //    }

    @Override
    public <T extends IBase> List<T> evaluate(IBase input, String path, Class<T> returnType) {
        return inner.evaluate(input, path, returnType);
    }

    @Override
    public <T extends IBase> List<T> evaluate(IBase input, IParsedExpression parsedExpression, Class<T> returnType) {
        return inner.evaluate(input, parsedExpression, returnType);
    }

    @Override
    public <T extends IBase> Optional<T> evaluateFirst(IBase input, String path, Class<T> returnType) {
        return inner.evaluateFirst(input, path, returnType);
    }

    @Override
    public <T extends IBase> Optional<T> evaluateFirst(
            IBase input, IParsedExpression parsedExpression, Class<T> returnType) {
        return inner.evaluateFirst(input, parsedExpression, returnType);
    }

    @Override
    public IParsedExpression parse(String expression) throws Exception {
        return inner.parse(expression);
    }

    @Override
    public void setEvaluationContext(@Nonnull IFhirPathEvaluationContext iFhirPathEvaluationContext) {}
}
