package org.opencds.cqf.fhir.cr.dev.server;

import org.opencds.cqf.fhir.cr.dev.server.config.ServerR4Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * CQF Clinical Reasoning server. Mounts HAPI's {@code RestfulServer} on Spring Boot's embedded
 * Tomcat, registers operation providers from {@code cqf-fhir-cr-hapi}, and bridges plain CRUD
 * to an in-memory {@code IRepository}.
 *
 * <p>Spring Boot's JPA / DataSource / JDBC auto-configurations are explicitly excluded — the
 * server has no datasource, and Hibernate must stay dormant on the classpath even though it's
 * pulled transitively from {@code hapi-fhir-jpaserver-base}. See spike measurements: with these
 * exclusions, no Hibernate code initializes during startup.
 */
@SpringBootApplication(
        exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            QuartzAutoConfiguration.class
        })
@Import(ServerR4Config.class)
public class Application {

    public static void main(String[] args) {
        new Application().run(args);
    }

    void run(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
