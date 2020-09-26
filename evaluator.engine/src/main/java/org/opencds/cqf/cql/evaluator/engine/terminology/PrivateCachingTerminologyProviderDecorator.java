package org.opencds.cqf.cql.evaluator.engine.terminology;

import java.util.HashMap;
import java.util.Map;

import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;

public class PrivateCachingTerminologyProviderDecorator implements TerminologyProvider {

    private Map<String, Iterable<Code>> valueSetIndexById = new HashMap<>();

    private TerminologyProvider innerProvider;

    public PrivateCachingTerminologyProviderDecorator(TerminologyProvider terminologyProvider) {
        this.innerProvider = terminologyProvider;
    }

    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        if (!valueSetIndexById.containsKey(valueSet.getId())) {
            // This will cache the ValueSet;
            this.expand(valueSet);
        }

        Iterable<Code> codes = valueSetIndexById.get(valueSet.getId());

        if (codes == null) {
            return false;
        }

        // TODO: Handle Versions
        for (Code c : codes) {
            if (c.getCode().equals(code.getCode()) && c.getSystem().equals(code.getSystem())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterable<Code> expand(ValueSetInfo valueSet) {
        if (!valueSetIndexById.containsKey(valueSet.getId())) {
            valueSetIndexById.put(valueSet.getId(), this.innerProvider.expand(valueSet));
        }

        return valueSetIndexById.get(valueSet.getId());
    }

    @Override
    public Code lookup(Code code, CodeSystemInfo codeSystem) {
        return this.innerProvider.lookup(code, codeSystem);
    }

}

