package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.concurrent.ThreadFactory;

public class CqlThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        return new CqlThread(r);
    }

    private static class CqlThread extends Thread {
        private CqlThread(Runnable runnable) {
            super(runnable);
            // set the correct classloader here
            setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }
    }
}
