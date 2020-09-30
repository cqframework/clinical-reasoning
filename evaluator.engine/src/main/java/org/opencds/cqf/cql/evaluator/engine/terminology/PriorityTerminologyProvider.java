package org.opencds.cqf.cql.evaluator.engine.terminology;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriorityTerminologyProvider implements TerminologyProvider {

    Logger logger = LoggerFactory.getLogger(PriorityTerminologyProvider.class);

    List<TerminologyProvider> terminologyProviders;

    public PriorityTerminologyProvider(List<TerminologyProvider> terminologyProviders) {
        this.terminologyProviders = requireNonNull(terminologyProviders, "terminologyProviders can not be null");
    }

    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        for (TerminologyProvider terminologyProvider : terminologyProviders) {
            try {
                boolean in = terminologyProvider.in(code, valueSet);
                if (in) {
                    return true;
                }
            }
            catch (Exception e) {
                logger.warn("inner provider threw an Exception, continuing: %s", e.getMessage());
            }
        }

        return false;
    }

    @Override
    public Iterable<Code> expand(ValueSetInfo valueSet) {
        return this.terminologyProviders.stream().map(x -> x.expand(valueSet)).findFirst().orElseGet(null);
    }

    @Override
    public Code lookup(Code code, CodeSystemInfo codeSystem) {
        return this.terminologyProviders.stream().map(x -> x.lookup(code, codeSystem)).findFirst().orElseGet(null);
    }
}
