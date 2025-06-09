package org.opencds.cqf.fhir.cr.hapi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PartitionHelper implements BeforeEachCallback, AfterEachCallback {
    private static final Logger ourLog = LoggerFactory.getLogger(PartitionHelper.class);
    protected final MyTestInterceptor myInterceptor = new MyTestInterceptor();

    @Autowired
    IInterceptorService myIInterceptorService;

    @Autowired
    PartitionSettings partitionSettings;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        partitionSettings.setPartitioningEnabled(true);
        myIInterceptorService.registerInterceptor(myInterceptor);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        myIInterceptorService.unregisterInterceptor(myInterceptor);
        partitionSettings.setPartitioningEnabled(false);
        myInterceptor.clear();
    }

    public void clear() {
        myInterceptor.clear();
    }

    public boolean wasCalled() {
        return myInterceptor.wasCalled();
    }

    public static class MyTestInterceptor {
        private boolean myCalled = false;

        @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
        RequestPartitionId partitionIdentifyRead(RequestDetails requestDetails) {
            myCalled = true;
            if (requestDetails == null) {
                ourLog.info("useful breakpoint :-)");
            }
            assertNotNull(requestDetails);
            return RequestPartitionId.fromPartitionId(null);
        }

        public void clear() {
            myCalled = false;
        }

        public boolean wasCalled() {
            return myCalled;
        }

        @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_CREATE)
        RequestPartitionId partitionIdentifyCreate(RequestDetails requestDetails) {
            return RequestPartitionId.fromPartitionId(null);
        }
    }
}
