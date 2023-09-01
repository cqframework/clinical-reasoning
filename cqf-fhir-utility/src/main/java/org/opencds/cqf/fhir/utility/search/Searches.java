package org.opencds.cqf.fhir.utility.search;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This is a utility class to help construct search maps, which are then passed into the Repository
 * to perform a search. If you find that you have a certain type of search that you are repeating
 * frequently consider adding it to this class.
 */
public class Searches {

    public static final Map<String, List<IQueryParameterType>> ALL = Collections.emptyMap();

    private Searches() {}

    public static SearchBuilder builder() {
        return new SearchBuilder();
    }

    public static Map<String, List<IQueryParameterType>> byCanonical(String canonical) {
        if (canonical.contains("|")) {
            var split = canonical.split("|");
            return byUrlAndVersion(split[0], split[1]);
        } else {
            return byUrl(canonical);
        }
    }

    public static Map<String, List<IQueryParameterType>> byCodeAndSystem(String code, String system) {
        return builder().withTokenParam("code", code, system).build();
    }

    public static Map<String, List<IQueryParameterType>> byUrl(String url) {
        return builder().withUriParam("url", url).build();
    }

    public static Map<String, List<IQueryParameterType>> byUrlAndVersion(String url, String version) {
        return builder()
                .withUriParam("url", url)
                .withTokenParam("version", version)
                .build();
    }

    public static Map<String, List<IQueryParameterType>> byName(String name) {
        return builder().withStringParam("name", name).build();
    }

    public static Map<String, List<IQueryParameterType>> byNameAndVersion(String name, String version) {
        if (version == null || version.isEmpty()) {
            return builder().withStringParam("name", name).build();
        } else {

            return builder()
                    .withStringParam("name", name)
                    .withTokenParam("version", version)
                    .build();
        }
    }

    public static class SearchBuilder {
        private Map<String, List<IQueryParameterType>> values;

        public Map<String, List<IQueryParameterType>> build() {
            return this.values;
        }

        public SearchBuilder withStringParam(String name, String value) {
            if (values == null) {
                values = new HashMap<>();
            }
            values.put(name, Collections.singletonList(new StringParam(value)));

            return this;
        }

        public SearchBuilder withTokenParam(String name, String value) {
            if (values == null) {
                values = new HashMap<>();
            }
            values.put(name, Collections.singletonList(new TokenParam(value)));

            return this;
        }

        public SearchBuilder withTokenParam(String name, String value, String system) {
            if (values == null) {
                values = new HashMap<>();
            }
            values.put(name, Collections.singletonList(new TokenParam(system, value)));

            return this;
        }

        public SearchBuilder withUriParam(String name, String value) {
            if (values == null) {
                values = new HashMap<>();
            }
            values.put(name, Collections.singletonList(new UriParam(value)));

            return this;
        }

        SearchBuilder withTokenParam(String name, String value, String... values) {
            if (this.values == null) {
                this.values = new HashMap<>();
            }

            var params = new ArrayList<IQueryParameterType>(1 + values.length);
            params.add(new TokenParam(value));

            for (var v : values) {
                params.add(new TokenParam(v));
            }

            this.values.put(name, params);

            return this;
        }
    }
}
