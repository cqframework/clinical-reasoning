package org.opencds.cqf.fhir.cr.hapi.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.test.BaseJpaDstu3Test;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.client.interceptor.SimpleRequestHeaderInterceptor;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.test.utilities.JettyUtil;
import java.util.concurrent.TimeUnit;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.opencds.cqf.fhir.cr.hapi.IResourceLoader;
import org.opencds.cqf.fhir.cr.hapi.config.dstu3.ApplyOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.dstu3.EvaluateOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.test.TestCrStorageSettingsConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {TestCrDstu3Config.class, ApplyOperationConfig.class, EvaluateOperationConfig.class})
public abstract class BaseCrDstu3TestServer extends BaseJpaDstu3Test implements IResourceLoader {

    public static IGenericClient ourClient;
    public static FhirContext ourCtx;
    public static CloseableHttpClient ourHttpClient;
    public static Server ourServer;
    public static String ourServerBase;
    public static DatabaseBackedPagingProvider ourPagingProvider;
    public static IParser ourParser;

    @Autowired
    protected DaoRegistry myDaoRegistry;

    @Autowired
    private TestCrStorageSettingsConfigurer myTestCrStorageSettingsConfigurer;

    private SimpleRequestHeaderInterceptor mySimpleHeaderInterceptor;

    @Autowired
    RestfulServer ourRestfulServer;

    @BeforeEach
    public void beforeStartServer() throws Exception {
        myTestCrStorageSettingsConfigurer.setUpConfiguration();

        ourServer = new Server(0);

        ServletContextHandler proxyHandler = new ServletContextHandler();
        proxyHandler.setContextPath("/");

        ServletHolder servletHolder = new ServletHolder();
        servletHolder.setServlet(ourRestfulServer);
        proxyHandler.addServlet(servletHolder, "/fhir/*");

        ourCtx = ourRestfulServer.getFhirContext();

        ourServer.setHandler(proxyHandler);
        JettyUtil.startServer(ourServer);
        int port = JettyUtil.getPortForStartedServer(ourServer);
        ourServerBase = "http://localhost:" + port + "/fhir";

        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(5000, TimeUnit.MILLISECONDS);
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionManager(connectionManager);
        ourHttpClient = builder.build();

        ourCtx.getRestfulClientFactory().setSocketTimeout(600 * 1000);
        ourClient = ourCtx.newRestfulGenericClient(ourServerBase);

        var loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestBody(true);
        loggingInterceptor.setLogResponseBody(true);
        ourClient.registerInterceptor(loggingInterceptor);

        ourParser = ourCtx.newJsonParser().setPrettyPrint(true);

        ourRestfulServer.setDefaultResponseEncoding(EncodingEnum.XML);
        ourPagingProvider = myAppCtx.getBean(DatabaseBackedPagingProvider.class);
        ourRestfulServer.setPagingProvider(ourPagingProvider);

        mySimpleHeaderInterceptor = new SimpleRequestHeaderInterceptor();
        ourClient.registerInterceptor(mySimpleHeaderInterceptor);
        myStorageSettings.setIndexMissingFields(JpaStorageSettings.IndexEnabledEnum.DISABLED);
    }

    @Override
    public DaoRegistry getDaoRegistry() {
        return myDaoRegistry;
    }

    @Override
    public FhirContext getFhirContext() {
        return ourCtx;
    }

    public Bundle loadBundle(String theLocation) {
        return loadBundle(Bundle.class, theLocation);
    }

    protected RequestDetails setupRequestDetails() {
        var requestDetails = new ServletRequestDetails();
        requestDetails.setServletRequest(new MockHttpServletRequest());
        requestDetails.setServer(ourRestfulServer);
        requestDetails.setFhirServerBase(ourServerBase);
        return requestDetails;
    }
}
