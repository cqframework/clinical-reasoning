package org.opencds.cqf.cql.evaluator.builder.implementation.string;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

public class StringTerminologyProviderBuilder {
	public TerminologyProvider build(Map<String, Pair<String, String>> models, List<String> terminologyBundles) {
		throw new NotImplementedException("String Representations of Terminology Bundles is not yet supported.");
	}
}