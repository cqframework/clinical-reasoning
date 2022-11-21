package org.opencds.cqf.cql.evaluator.fhir.npm;

import org.hl7.fhir.r5.context.IWorkerContext;
import org.slf4j.Logger;

public class LoggerAdapter implements IWorkerContext.ILoggingService {
    private Logger innerLogger;

    public LoggerAdapter(Logger innerLogger) {
        this.innerLogger = innerLogger;
    }
    @Override
    public void logMessage(String s) {
        innerLogger.info(s);
    }

    @Override
    public void logDebugMessage(LogCategory logCategory, String s) {
        innerLogger.debug(String.format("%s: %s", logCategory.toString(), s));
    }
}
