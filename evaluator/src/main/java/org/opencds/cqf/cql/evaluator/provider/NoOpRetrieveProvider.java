<<<<<<< HEAD:evaluator.execution/src/main/java/org/opencds/cqf/cql/evaluator/execution/provider/NoOpRetrieveProvider.java
package org.opencds.cqf.cql.evaluator.execution.provider;
=======
package org.opencds.cqf.cql.evaluator.provider;
>>>>>>> 69178e5... Updates to evaluator:evaluator/src/main/java/org/opencds/cqf/cql/evaluator/provider/NoOpRetrieveProvider.java

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class NoOpRetrieveProvider implements RetrieveProvider {

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
            String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
            String dateLowPath, String dateHighPath, Interval dateRange) {
        return null;
    }
}