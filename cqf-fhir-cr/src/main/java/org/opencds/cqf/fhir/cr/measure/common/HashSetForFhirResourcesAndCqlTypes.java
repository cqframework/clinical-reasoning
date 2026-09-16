package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Value;

/**
 * A Set that identifies FHIR resources by resource type and logical ID, so two objects describing
 * the same resource are one element even when they are different instances - or different
 * representations, since a resource reaches this pipeline either as a HAPI object or as the CQL
 * engine's native {@link org.opencds.cqf.cql.engine.runtime.ClassInstance}. Values that are not
 * resources keep CQL {@code =} semantics, and everything else its own {@code equals}.
 * <p/>
 * Elements are stored under an {@link IdentityKey}, which is what makes that one relation apply to
 * every operation. The class this replaces compared elements pairwise, which had two consequences
 * worth stating, since both are the reason for the rewrite rather than incidental to it:
 * <ul>
 *   <li>{@code add} and {@code remove} routed CQL values into {@code HashSet}'s own equality while
 *       {@code contains} and {@code retainAll} routed them into CQL {@code =}. Under CQL 5 every
 *       expression result is a {@code ClassInstance}, which those two relations need not agree
 *       about, so a set could fail to contain a value it had just added.</li>
 *   <li>Every operation was a linear scan, and under CQL 5 each comparison in that scan walked a
 *       resource graph. Building an n-element set was O(n²) in deep structural comparisons.</li>
 * </ul>
 *
 * <p/>For a wrapper-aware sister type used by {@code PopulationDef.subjectResources}, see
 * {@link HashSetForCqlExpressionValues}.
 *
 * @param <T> the type of elements in this set, which may or may not be a {@link IBaseResource}
 *           or a {@link Value}
 */
public class HashSetForFhirResourcesAndCqlTypes<T> extends AbstractSet<T> {

    /** Insertion-ordered so iteration is stable, as it was when this extended {@code HashSet}. */
    private final Map<IdentityKey, T> elementsByKey = new LinkedHashMap<>();

    public HashSetForFhirResourcesAndCqlTypes() {
        super();
    }

    public HashSetForFhirResourcesAndCqlTypes(Collection<T> collection) {
        this((Iterable<T>) collection);
    }

    public HashSetForFhirResourcesAndCqlTypes(Iterable<T> iterable) {
        super();
        for (T value : iterable) {
            this.add(value);
        }
    }

    public HashSetForFhirResourcesAndCqlTypes(T singleValue) {
        super();
        this.add(singleValue);
    }

    /**
     * The key {@code element} is stored under. Subclasses that hold wrappers rather than the values
     * themselves override this to key on what they wrap.
     * <p/>
     * Package-private rather than protected: {@link IdentityKey} is an implementation detail of this
     * package, so a subclass outside it could neither name the return type nor override this.
     */
    IdentityKey keyFor(Object element) {
        return IdentityKey.of(element);
    }

    @Override
    public boolean add(T newElement) {
        var key = keyFor(newElement);
        if (elementsByKey.containsKey(key)) {
            return false;
        }
        elementsByKey.put(key, newElement);
        return true;
    }

    @Override
    public boolean contains(Object other) {
        return elementsByKey.containsKey(keyFor(other));
    }

    @Override
    public boolean remove(Object removalCandidate) {
        var key = keyFor(removalCandidate);
        if (!elementsByKey.containsKey(key)) {
            return false;
        }
        elementsByKey.remove(key);
        return true;
    }

    /**
     * Retains the elements whose identity appears in {@code otherCollection}.
     * <p/>
     * The inherited implementation would ask {@code otherCollection} what it contains, and a plain
     * {@code List} or {@code HashSet} answers that by Java object identity - which is how a
     * population intersection silently drops resources. Keying the other collection first means the
     * comparison runs in this set's relation regardless of what the other collection is.
     */
    @Override
    public boolean retainAll(@Nonnull Collection<?> otherCollection) {
        Objects.requireNonNull(otherCollection);

        var retainedKeys = new HashSet<IdentityKey>();
        for (Object other : otherCollection) {
            retainedKeys.add(keyFor(other));
        }
        return elementsByKey.keySet().retainAll(retainedKeys);
    }

    /**
     * Removes the elements whose identity appears in {@code otherCollection}. Overridden for the
     * same reason as {@link #retainAll(Collection)}: the inherited implementation delegates to the
     * other collection's {@code contains} once this set is the smaller of the two.
     */
    @Override
    public boolean removeAll(@Nonnull Collection<?> otherCollection) {
        Objects.requireNonNull(otherCollection);

        boolean modified = false;
        for (Object other : otherCollection) {
            modified |= remove(other);
        }
        return modified;
    }

    @Override
    @Nonnull
    public Iterator<T> iterator() {
        return elementsByKey.values().iterator();
    }

    @Override
    public int size() {
        return elementsByKey.size();
    }

    @Override
    public void clear() {
        elementsByKey.clear();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        final T firstElement = iterator().next();

        if (firstElement instanceof IBaseResource) {
            return stream()
                    .map(IBaseResource.class::cast)
                    .map(resource -> resource.getIdElement().getValueAsString())
                    .collect(Collectors.joining(",", "[", "]"));
        }

        return super.toString();
    }
}
