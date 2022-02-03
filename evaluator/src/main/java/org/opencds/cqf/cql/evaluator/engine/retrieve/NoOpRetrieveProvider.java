package org.opencds.cqf.cql.evaluator.engine.retrieve;

import java.util.Collections;

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpRetrieveProvider implements RetrieveProvider {

    private static final Logger logger = LoggerFactory.getLogger(NoOpRetrieveProvider.class);

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
            String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
            String dateLowPath, String dateHighPath, Interval dateRange) {

            logger.info("Attempted retrieve of {}. Returning empty set.", dataType);
            return Collections.emptySet();
    }
}