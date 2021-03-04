package org.opencds.cqf.cql.evaluator.engine.execution;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

/** This interface extends LibraryLoader to expose the set of translator options the
 * LibraryLoader is currently configured with.
 */
public interface TranslatorOptionAwareLibraryLoader extends LibraryLoader {
    public CqlTranslatorOptions getCqlTranslatorOptions();
}
