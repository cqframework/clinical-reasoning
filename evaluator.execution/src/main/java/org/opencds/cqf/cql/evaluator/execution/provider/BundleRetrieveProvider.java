package org.opencds.cqf.cql.evaluator.execution.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.cql.evaluator.execution.util.CodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;

public class BundleRetrieveProvider implements RetrieveProvider {

	private static final Logger logger = LoggerFactory.getLogger(BundleRetrieveProvider.class);

	private FhirContext fhirContext;
	private IBaseBundle bundle;
	private ModelResolver modelResolver;
	private TerminologyProvider terminologyProvider;

	public BundleRetrieveProvider(FhirContext fhirContext, IBaseBundle bundle, ModelResolver modelResolver) {
		this(fhirContext, bundle, modelResolver, null);
	}

	public BundleRetrieveProvider(FhirContext fhirContext, IBaseBundle bundle, ModelResolver modelResolver,
			TerminologyProvider terminologyProvider) {
		Objects.requireNonNull(fhirContext, "fhirContext can not be null.");
		Objects.requireNonNull(bundle, "bundle can not be null.");
		Objects.requireNonNull(modelResolver, "modelResolver can not be null.");

		this.fhirContext = fhirContext;
		this.modelResolver = modelResolver;
		this.bundle = bundle;
		this.terminologyProvider = terminologyProvider;
	}

	@Override
	public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
			String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
			String dateLowPath, String dateHighPath, Interval dateRange) {

		List<? extends IBaseResource> resources = BundleUtil.toListOfResourcesOfType(this.fhirContext, this.bundle,
				this.fhirContext.getResourceDefinition(dataType).getImplementingClass());

		resources = this.filterToContext(dataType, context, contextPath, contextValue, resources);

		resources = this.filterToTerminology(codePath, codes, valueSet, resources);

		return resources.stream().map(x -> (Object) x).collect(Collectors.toList());
	}

	private boolean isCodeMatch(Code code, Iterable<Code> codes) {
		for (Code otherCode : codes) {
			if (code.getCode().equals(otherCode.getCode()) && code.getSystem().equals(otherCode.getSystem())) {
				return true;
			}
		}

		return false;
	}

	public boolean isInValueSet(Code code, ValueSetInfo valueSetInfo) {
		if (this.terminologyProvider == null) {
			throw new IllegalStateException(String.format(
					"Unable to check code membership for code %s in valueset %s. terminologyProvider is null.",
					code.toString(), valueSetInfo.getId()));
		}

		return this.terminologyProvider.in(code, valueSetInfo);
	}

	private List<? extends IBaseResource> filterToTerminology(String codePath, Iterable<Code> codes, String valueSet,
			List<? extends IBaseResource> resources) {
		if (codes == null && valueSet == null) {
			return resources;
		}

		if (codePath == null) {
			return resources;
		}

		List<IBaseResource> filtered = new ArrayList<>();

		for (IBaseResource res : resources) {
			Object resCodes = this.modelResolver.resolvePath(res, codePath);
			List<Code> elmCodes = CodeUtil.getElmCodesFromObject(resCodes, fhirContext);
			if (elmCodes == null) {
				continue;
			}

			if (valueSet != null) {
				ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
				for (Code code : elmCodes) {
					if (isInValueSet(code, valueSetInfo)) {
						filtered.add(res);
						break;
					}
				}
			}

			if (codes != null) {
				for (Code code : codes) {
					if (isCodeMatch(code, elmCodes)) {
						filtered.add(res);
						break;
					}
				}
			}
		}

		return filtered;
	}

	private List<? extends IBaseResource> filterToContext(String dataType, String context, String contextPath,
			Object contextValue, List<? extends IBaseResource> resources) {
		if (context == null || contextValue == null || contextPath == null) {
			logger.warn(
					"Unable to relate {} to {} context with contextPath: {} and contextValue: {}. Returning all resources.",
					dataType, context, contextPath, contextValue);
			return resources;
		}

		List<IBaseResource> filtered = new ArrayList<>();

		for (IBaseResource res : resources) {
			try {
				Object resContextValue = this.modelResolver.resolvePath(res, contextPath);
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

				filtered.add(res);
			} catch (Exception e) {
				continue;
			}
		}

		return filtered;
	}
}