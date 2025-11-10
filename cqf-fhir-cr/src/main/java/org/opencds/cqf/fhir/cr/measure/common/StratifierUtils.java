package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;

/**
 * Various FHIR version-agnostic utilities for working with Stratifiers.
 */
public class StratifierUtils {

    private StratifierUtils() {
        // Static utility class
    }

    public static List<Class<?>> extractClassesFromSingleOrListResult(Object result) {
        if (result == null) {
            return Collections.emptyList();
        }

        if (result instanceof Class<?> clazz) {
            return List.of(clazz);
        }

        if (!(result instanceof Iterable<?> iterable)) {
            return List.of(result.getClass());
        }

        // Need to this to return List<Class<?>> and get rid of Sonar warnings.
        final Stream<Class<?>> classStream =
                getStream(iterable).filter(Objects::nonNull).map(Object::getClass);

        return classStream.toList();
    }

    private static Stream<?> getStream(Iterable<?> iterable) {
        if (iterable instanceof List<?> list) {
            return list.stream();
        }

        // It's entirely possible CQL returns an Iterable that is not a List, so we need to handle that case
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
