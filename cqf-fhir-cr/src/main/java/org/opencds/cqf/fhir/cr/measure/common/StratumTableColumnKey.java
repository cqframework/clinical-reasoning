package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.HashBasedTable;

/**
 * Column key for the subject-results table. Pairs component identity with the stratum value so two
 * components that resolve to the same value (e.g. Race and Ethnicity both AskedButNoAnswer) do not
 * overwrite each other in the underlying {@link HashBasedTable} (CDO-789).
 */
record StratumTableColumnKey(StratifierComponentDef component, StratumValueWrapper value) {}
