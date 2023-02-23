package org.opencds.cqf.fhir.utility;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.uhn.fhir.model.api.IQueryParameterType;

public class Searches {

    public static final Map<String, List<IQueryParameterType>> ALL = Collections.emptyMap();

    private Searches() {}

    public static SearchBuilder builder() {
        return new SearchBuilder();
    }

    public static Map<String, List<IQueryParameterType>> byUrl(String url) {
        return builder().withToken("url", url).build();
    }

    public static Map<String, List<IQueryParameterType>> byName(String name) {
        return builder().withToken("name", name).build();
    }

    public static Map<String, List<IQueryParameterType>> byNameAndVersion(String name,
            String version) {
        return builder().withToken("name", name).withToken("version", version).build();
    }

    public static class SearchBuilder {
        private Map<String, List<IQueryParameterType>> values;

        public Map<String, List<IQueryParameterType>> build() {
            return this.values;
        }

        SearchBuilder withToken(String name, String value) {
            return this;
        }

        SearchBuilder withToken(String name, String value, String... values) {
            return this;
        }
    }
}
