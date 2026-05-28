package org.opencds.cqf.fhir.cr.dev.server.search;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts raw URL query parameters (as HAPI exposes them via
 * {@code RequestDetails.getParameters()}) into the typed {@code IQueryParameterType} form that
 * {@link ca.uhn.fhir.repository.IRepository#search} expects.
 *
 * <p>Skips response-shaping params ({@code _format}, {@code _count}, etc.) since those are HAPI's
 * concern, not the repository's. Resolves each remaining param's type via the
 * {@link FhirContext}'s search-parameter registry; unknown names default to {@link StringParam}.
 */
public final class SearchParameterTranslator {

    /** Response-shaping params handled by HAPI; the repository doesn't see them. */
    private static final Set<String> RESPONSE_SHAPING = Set.of(
            "_format",
            "_pretty",
            "_summary",
            "_elements",
            "_count",
            "_offset",
            "_total",
            "_sort",
            "_include",
            "_revinclude");

    private SearchParameterTranslator() {}

    public static Multimap<String, List<IQueryParameterType>> translate(
            FhirContext fhirContext, String resourceType, Map<String, String[]> rawParams) {
        var def = fhirContext.getResourceDefinition(resourceType);
        var out = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        for (var entry : rawParams.entrySet()) {
            translateEntry(fhirContext, def, entry.getKey(), entry.getValue(), out);
        }
        return out;
    }

    private static void translateEntry(
            FhirContext fhirContext,
            RuntimeResourceDefinition def,
            String fullName,
            String[] values,
            Multimap<String, List<IQueryParameterType>> out) {
        if (values == null || values.length == 0) {
            return;
        }
        int colonIdx = fullName.indexOf(':');
        String name = colonIdx < 0 ? fullName : fullName.substring(0, colonIdx);
        String qualifier = colonIdx < 0 ? null : fullName.substring(colonIdx);
        if (RESPONSE_SHAPING.contains(name)) {
            return;
        }
        var type = resolveType(name, def);
        for (String raw : values) {
            IQueryParameterType param = create(type, fhirContext, name, qualifier, raw);
            if (param != null) {
                out.put(name, List.of(param));
            }
        }
    }

    private static RestSearchParameterTypeEnum resolveType(String name, RuntimeResourceDefinition def) {
        // _id is universally a token (FHIR R4 spec).
        if ("_id".equals(name)) return RestSearchParameterTypeEnum.TOKEN;
        if ("_profile".equals(name)) return RestSearchParameterTypeEnum.URI;
        if ("_tag".equals(name) || "_security".equals(name) || "_source".equals(name))
            return RestSearchParameterTypeEnum.TOKEN;
        if ("_lastUpdated".equals(name)) return RestSearchParameterTypeEnum.DATE;

        RuntimeSearchParam runtime = def.getSearchParam(name);
        return runtime != null ? runtime.getParamType() : RestSearchParameterTypeEnum.STRING;
    }

    private static IQueryParameterType create(
            RestSearchParameterTypeEnum type, FhirContext fhirContext, String name, String qualifier, String rawValue) {
        IQueryParameterType param =
                switch (type) {
                    case TOKEN -> new TokenParam();
                    case STRING -> new StringParam();
                    case REFERENCE -> new ReferenceParam();
                    case URI -> new UriParam();
                    case DATE -> new DateParam();
                    case NUMBER -> new NumberParam();
                    case QUANTITY -> new QuantityParam();
                    // COMPOSITE / HAS / SPECIAL — fall back to a string match; refine when needed.
                    default -> new StringParam();
                };
        try {
            param.setValueAsQueryToken(fhirContext, name, qualifier, rawValue);
            return param;
        } catch (RuntimeException ex) {
            // Unparseable value for the chosen type — fall back to a permissive string match
            // so the request fails with empty results rather than a 500.
            var fallback = new StringParam();
            fallback.setValueAsQueryToken(fhirContext, name, qualifier, rawValue);
            return fallback;
        }
    }
}
