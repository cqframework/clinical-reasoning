package org.opencds.cqf.fhir.cql.engine.terminology;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.util.BundleUtil;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.engine.utility.ValueSets;
import org.opencds.cqf.fhir.utility.FhirPathCache;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The implementation of this class uses a sorted list to perform terminology membership operations.
 * I tried changing the implementation to use a TreeSet rather than a sorted list and a binary
 * search, but found that the performance was reduced by ~33%. Please run the benchmarks to verify
 * that changes to this class do not result in significant performance degradation.
 */
public class RepositoryTerminologyProvider implements TerminologyProvider {

    // eventually, we want to be able to detect expansion capabilities from the
    // capability statement. For now, we hard code based on the our knowledge
    // of where we set this terminology provider up.
    public enum EXPANSION_MODE {
        INTERNAL,
        REPOSITORY,
        AUTO
    }

    private static final Logger logger = LoggerFactory.getLogger(RepositoryTerminologyProvider.class);

    private static final Comparator<Code> CODE_COMPARATOR =
            (x, y) -> x.getCode().compareTo(y.getCode());
    private final Repository repository;
    private final FhirContext fhirContext;
    private final IFhirPath fhirPath;
    private final Map<String, List<Code>> valueSetIndex;
    private final EXPANSION_MODE expansionMode;

    // The cached expansions are sorted by code order
    // This is used determine the range of codes to check
    private static class Range {

        public static final Range EMPTY = new Range(-1, -1);

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public final int start;
        public final int end;
    }

    public RepositoryTerminologyProvider(Repository repository, EXPANSION_MODE expansionMode) {
        this(repository, new HashMap<>(), expansionMode);
    }

    public RepositoryTerminologyProvider(
            Repository repository, Map<String, List<Code>> valueSetIndex, EXPANSION_MODE expansionMode) {
        this.repository = requireNonNull(repository, "repository can not be null.");
        this.valueSetIndex = requireNonNull(valueSetIndex, "valueSetIndex can not be null.");
        this.expansionMode = requireNonNull(expansionMode, "expansionMode can not be null.");

        this.fhirContext = repository.fhirContext();
        this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    }

    public RepositoryTerminologyProvider(Repository repository) {
        this(repository, new HashMap<>(), EXPANSION_MODE.AUTO);
    }

    /**
     * This method checks for membership of a Code in a ValueSet
     *
     * @param code The Code to check.
     * @param valueSet The ValueSetInfo for the ValueSet to check membership of. Can not be null.
     * @return True if code is in the ValueSet.
     */
    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        // Implementation note: This function should be considered inner loop
        // code. It's called thousands or millions of times by the CQL engine
        // during evaluation
        requireNonNull(code, "code can not be null when using 'expand'");
        requireNonNull(valueSet, "valueSet can not be null when using 'expand'");

        List<Code> codes = this.expand(valueSet);

        // This range includes all codes that have an equivalent code value,
        // So we only need to check the code system.
        Range range = this.getSearchRange(code, codes);
        for (int i = range.start; i < range.end; i++) {
            var c = codes.get(i);
            if (c.getSystem().equals(code.getSystem())) {
                return true;
            }
        }

        return false;
    }

    /**
     * This method expands a ValueSet into a list of Codes. It will use the "expansion" element of the
     * ValueSet if present. It will fall back the to "compose" element if not present. <b>NOTE:</b>
     * This provider does not provide a full expansion of the "compose" element. If only lists the
     * codes present in the "compose".
     *
     * @param valueSet The ValueSetInfo of the ValueSet to expand
     * @return The Codes in the ValueSet. <b>NOTE:</b> This method never returns null.
     */
    @Override
    public List<Code> expand(ValueSetInfo valueSet) {
        requireNonNull(valueSet, "valueSet can not be null when using 'expand'");

        // create a url|version canonical url from the info
        var url = valueSet.getId() + (valueSet.getVersion() != null ? ("|" + valueSet.getVersion()) : "");

        var expansion = this.valueSetIndex.computeIfAbsent(url, k -> tryExpand(valueSet));
        if (expansion == null) {
            throw new IllegalArgumentException(
                    String.format("Unable to get expansion for ValueSet %s", valueSet.getId()));
        }

        return expansion;
    }

    private Class<? extends IBaseResource> classFor(String resourceType) {
        return this.fhirContext.getResourceDefinition(resourceType).getImplementingClass();
    }

    // Attempts to perform expansion of the referenced ValueSet. It will first use an
    // expansion if one is available, and then ask the underlying repository to perform
    // an expansion if possible, and then fall back to doing a "naive" expansion where
    // possible. A "naive" expansion includes only codes directly referenced in the ValueSet
    // It's not possible to run expansion filters without the support of a terminology server.
    private List<Code> tryExpand(ValueSetInfo valueSet) {

        var search = valueSet.getVersion() != null
                ? Searches.byUrlAndVersion(valueSet.getId(), valueSet.getVersion())
                : Searches.byUrl(valueSet.getId());

        @SuppressWarnings("unchecked")
        var results = this.repository.search(
                (Class<? extends IBaseBundle>) classFor("Bundle"), classFor("ValueSet"), search, null);

        var resources = BundleUtil.toListOfResources(fhirContext, results);

        if (resources.isEmpty()) {
            throw new IllegalArgumentException(String.format("Unable to locate ValueSet %s", valueSet.getId()));
        }

        if (resources.size() > 1) {
            throw new IllegalArgumentException(String.format("Multiple ValueSets resolved for %s", valueSet.getId()));
        }

        var vs = resources.get(0);

        List<Code> codes = ValueSets.getCodesInExpansion(this.fhirContext, vs);
        if (codes != null && Boolean.TRUE.equals(isNaiveExpansion(vs))) {
            logger.warn(
                    "Codes for ValueSet {} expanded without a terminology server, some results may not be correct.",
                    valueSet.getId());
        }

        boolean shouldUseRepoExpansion = this.expansionMode == EXPANSION_MODE.INTERNAL
                || (this.expansionMode == EXPANSION_MODE.REPOSITORY && canRepositoryExpand(valueSet));
        if (codes == null && shouldUseRepoExpansion) {
            vs = this.repository.invoke(vs.getIdElement(), "$expand", null).getResource();
            codes = ValueSets.getCodesInExpansion(this.fhirContext, vs);
        }

        // Still don't have any codes, so try a naive expansion
        if (codes == null) {
            if (containsExpansionLogic(vs)) {
                throw new IllegalArgumentException(String.format(
                        "ValueSet %s requires $expand to support correctly, and $expand is not available",
                        valueSet.getId()));
            }

            logger.warn(
                    "ValueSet {} is not expanded. Falling back to compose definition. This will potentially produce incorrect results. ",
                    valueSet.getId());

            codes = ValueSets.getCodesInCompose(fhirContext, vs);
        }

        if (codes == null) {
            throw new IllegalArgumentException(String.format("Failed to expand ValueSet %s", valueSet.getId()));
        }

        Collections.sort(codes, CODE_COMPARATOR);
        return codes;
    }

    // Given a set of Codes sorted by ".code", find the range that matching codes
    // occur in. This function first performs a binary search to find a matching element
    // and then iterates backwards and forwards from there to get the set of candidate codes
    private Range getSearchRange(Code code, List<Code> expansion) {
        int index = Collections.binarySearch(expansion, code, CODE_COMPARATOR);

        if (index < 0) {
            return Range.EMPTY;
        }

        int first = index;
        int last = index + 1;

        var value = code.getCode();

        while (first > 0 && expansion.get(first - 1).getCode().equals(value)) {
            first--;
        }

        while (last < expansion.size() && expansion.get(last).getCode().equals(value)) {
            last++;
        }

        return new Range(first, last);
    }

    /**
     * Lookup is only partially implemented for this TerminologyProvider. Full implementation requires
     * the ability to access the full CodeSystem. This implementation only checks the code system of
     * the code matches the CodeSystemInfo url, and verifies the version if present.
     *
     * @param code The Code to lookup
     * @param codeSystem The CodeSystemInfo of the CodeSystem to check.
     * @return The Code if the system of the Code (and version if specified) matches the
     *         CodeSystemInfo url (and version)
     */
    @Override
    public Code lookup(Code code, CodeSystemInfo codeSystem) {

        if (code.getSystem() == null) {
            return null;
        }

        if (code.getSystem().equals(codeSystem.getId())
                && (code.getVersion() == null || code.getVersion().equals(codeSystem.getVersion()))) {
            logger.warn("Unvalidated CodeSystem lookup: {} in {}", code, codeSystem.getId());
            return code;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Boolean isNaiveExpansion(IBaseResource resource) {
        IBase expansion = ValueSets.getExpansion(this.fhirContext, resource);
        if (expansion != null) {
            Object object = ValueSets.getExpansionParameters(expansion, fhirPath, ".where(name = 'naive').value");
            if (object instanceof IBase) {
                return resolveNaiveBoolean((IBase) object);
            } else if (object instanceof Iterable) {
                List<IBase> naiveParameters = (List<IBase>) object;
                for (IBase param : naiveParameters) {
                    return resolveNaiveBoolean(param);
                }
            }
        }
        return null;
    }

    private Boolean resolveNaiveBoolean(IBase param) {
        if (param.fhirType().equals("boolean")) {
            return (Boolean) ((IPrimitiveType<?>) param).getValue();
        } else {
            return null;
        }
    }

    private boolean containsExpansionLogic(IBaseResource resource) {
        List<IBase> includeFilters = ValueSets.getIncludeFilters(this.fhirContext, resource);
        if (includeFilters != null && !includeFilters.isEmpty()) {
            return true;
        }
        List<IBase> excludeFilters = ValueSets.getExcludeFilters(this.fhirContext, resource);
        if (excludeFilters != null && !excludeFilters.isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean canRepositoryExpand(ValueSetInfo valueSetInfo) {
        // TODO: check capabilities statement to see if expansion is supported.
        return true;
    }
}
