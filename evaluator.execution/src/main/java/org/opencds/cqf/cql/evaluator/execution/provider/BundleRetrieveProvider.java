package org.opencds.cqf.cql.evaluator.execution.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.cql.evaluator.execution.util.CodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.util.BundleUtil;

@SuppressWarnings("rawtypes")
public class BundleRetrieveProvider implements RetrieveProvider {

	private static final Logger logger = LoggerFactory.getLogger(BundleRetrieveProvider.class);

	private IBaseBundle bundle;
	private FhirModelResolver modelResolver;
	private TerminologyProvider terminologyProvider;

	public BundleRetrieveProvider(FhirModelResolver modelResolver, IBaseBundle bundle) {
		this(modelResolver, bundle, null);
	}

	public BundleRetrieveProvider(FhirModelResolver modelResolver, IBaseBundle bundle,
			TerminologyProvider terminologyProvider) {
		Objects.requireNonNull(bundle, "bundle can not be null.");
		Objects.requireNonNull(modelResolver, "modelResolver can not be null.");

		this.modelResolver = modelResolver;
		this.bundle = bundle;
		this.terminologyProvider = terminologyProvider;
	}

	@Override
	public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
			String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
			String dateLowPath, String dateHighPath, Interval dateRange) {

		List<? extends IBaseResource> resources = BundleUtil.toListOfResourcesOfType(
				this.modelResolver.getFhirContext(), this.bundle,
				this.modelResolver.getFhirContext().getResourceDefinition(dataType).getImplementingClass());

		resources = this.filterToContext(dataType, context, contextPath, contextValue, resources);
		resources = this.filterToTerminology(dataType, codePath, codes, valueSet, resources);

		return resources.stream().map(x -> (Object) x).collect(Collectors.toList());
	}

	private boolean anyCodeMatch(Iterable<Code> left, Iterable<Code> right) {
		if (left == null || right == null) {
			return false;
		}

		for (Code code : left) {
			for (Code otherCode : right) {
				if (code.getCode() != null && code.getCode().equals(otherCode.getCode()) && code.getSystem() != null && code.getSystem().equals(otherCode.getSystem())) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean anyCodeInValueSet(Iterable<Code> codes, String valueSet) {
		if (codes == null || valueSet == null) {
			return false;
		}

		if (this.terminologyProvider == null) {
			throw new IllegalStateException(String.format(
					"Unable to check code membership for in ValueSet %s. terminologyProvider is null.", valueSet));
		}

		ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
		for (Code code : codes) {
			if (this.terminologyProvider.in(code, valueSetInfo)) {
				return true;
			}
		}

		return false;
	}

	// Special case filtering to handle "codes" that are actually ids. This is a
	// workaround to handle filtering by Id.
	private boolean isPrimitiveMatch(String dataType, IPrimitiveType<?> code, Iterable<Code> codes) {
		if (code == null || codes == null) {
			return false;
		}

		// This handles the case that the value is a reference such as
		// "Medication/med-id"
		String primitiveString = code.getValueAsString().replace(dataType + "/", "");
		for (Object c : codes) {
			if (c instanceof String) {
				String s = (String) c;
				if (s.equals(primitiveString)) {
					return true;
				}
			}
		}

		return false;
	}

	private List<? extends IBaseResource> filterToTerminology(String dataType, String codePath, Iterable<Code> codes,
			String valueSet, List<? extends IBaseResource> resources) {
		if (codes == null && valueSet == null) {
			return resources;
		}

		if (codePath == null) {
			return resources;
		}

		List<IBaseResource> filtered = new ArrayList<>();

		for (IBaseResource res : resources) {
			Object value = this.modelResolver.resolvePath(res, codePath);

			if (value instanceof IPrimitiveType) {
				if (isPrimitiveMatch(dataType, (IPrimitiveType<?>) value, codes)) {
					filtered.add(res);
				}
				continue;
			}

			List<Code> resourceCodes = CodeUtil.getElmCodesFromObject(value, this.modelResolver.getFhirContext());
			if (resourceCodes == null) {
				continue;
			}

			if (anyCodeMatch(resourceCodes, codes)) {
				filtered.add(res);
				continue;
			}

			if (anyCodeInValueSet(resourceCodes, valueSet)) {
				filtered.add(res);
				continue;
			}
		}

		return filtered;
	}

	private List<? extends IBaseResource> filterToContext(String dataType, String context, String contextPath,
			Object contextValue, List<? extends IBaseResource> resources) {
		if (context == null || contextValue == null || contextPath == null) {
			logger.info(
					"Unable to relate {} to {} context with contextPath: {} and contextValue: {}. Returning all resources.",
					dataType, context, contextPath, contextValue);
			return resources;
		}

		List<IBaseResource> filtered = new ArrayList<>();

		for (IBaseResource res : resources) {
			Object resContextValue = this.modelResolver.resolvePath(res, contextPath);
			if (resContextValue instanceof IIdType) {
				String id = ((IIdType)resContextValue).getValue();
				if (id == null) {
					logger.info("Found null id for {} resource. Skipping.", dataType);
					continue;
				}

				if (id.contains("/")) {
					id = id.split("/")[1];
				}
				
				if (!id.equals(contextValue)) {
					logger.info("Found {} with id  {}. Skipping.", dataType, id);
					continue;
				}
			} else {
				IPrimitiveType<?> referenceValue = (IPrimitiveType<?>) this.modelResolver.resolvePath(resContextValue,
						"reference");
				if (referenceValue == null) {
					logger.info("Found {} resource unrelated to context. Skipping.", dataType);
					continue;
				}

				String referenceString = referenceValue.getValueAsString();
				if (referenceString.contains("/")) {
					referenceString = referenceString.substring(referenceString.indexOf("/") + 1,
							referenceString.length());
				}

				if (!referenceString.equals((String) contextValue)) {
					logger.info("Found {} resource for context value: {} when expecting: {}. Skipping.", dataType,
							referenceString, (String) contextValue);
					continue;
				}
			}

			filtered.add(res);
		}

		return filtered;
	}
}