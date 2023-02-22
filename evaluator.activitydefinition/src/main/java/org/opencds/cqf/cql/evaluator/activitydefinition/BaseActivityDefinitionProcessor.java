package org.opencds.cqf.cql.evaluator.activitydefinition;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.util.ParametersUtil;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public abstract class BaseActivityDefinitionProcessor<T> {
   private static final Logger logger = LoggerFactory.getLogger(BaseActivityDefinitionProcessor.class);
   public static final String TARGET_STATUS_URL = "http://hl7.org/fhir/us/ecr/StructureDefinition/targetStatus";
   public static final String PRODUCT_ERROR_PREAMBLE = "Product does not map to ";
   public static final String DOSAGE_ERROR_PREAMBLE = "Dosage does not map to ";
   public static final String BODYSITE_ERROR_PREAMBLE = "BodySite does not map to ";
   public static final String CODE_ERROR_PREAMBLE = "Code does not map to ";
   public static final String QUANTITY_ERROR_PREAMBLE = "Quantity does not map to ";
   public static final String MISSING_CODE_PROPERTY = "Missing required code property";
   private final FhirContext fhirContext;
   protected final FhirDal fhirDal;
   private final LibraryProcessor libraryProcessor;
   private final IFhirPath fhirPath;
   private final ModelResolver modelResolver;

   protected BaseActivityDefinitionProcessor(FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor) {
      requireNonNull(fhirContext, "fhirContext can not be null");
      requireNonNull(fhirDal, "fhirDal can not be null");
      requireNonNull(libraryProcessor, "LibraryProcessor can not be null");
      this.fhirContext = fhirContext;
      this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
      this.fhirDal = fhirDal;
      this.libraryProcessor = libraryProcessor;
      modelResolver = new FhirModelResolverFactory().create(fhirContext.getVersion().getVersion().getFhirVersionString());
   }

   protected String subjectId;
   private IBaseParameters parameters;
   private IBaseResource contentEndpoint;
   private IBaseResource terminologyEndpoint;
   private IBaseResource dataEndpoint;

   @SuppressWarnings("unchecked")
   public IBaseResource apply(
           IIdType theId, String subjectId, String encounterId, String practitionerId,
           String organizationId, String userType, String userLanguage, String userTaskContext,
           String setting, String settingContext, IBaseParameters parameters,
           IBaseResource contentEndpoint, IBaseResource terminologyEndpoint, IBaseResource dataEndpoint) {
      this.subjectId = subjectId;
      this.parameters = parameters;
      this.contentEndpoint = contentEndpoint;
      this.terminologyEndpoint = terminologyEndpoint;
      this.dataEndpoint = dataEndpoint;
      T activityDefinition = (T) this.fhirDal.read(theId);
      if (activityDefinition == null) {
         throw new IllegalArgumentException("Couldn't find ActivityDefinition " + theId);
      }
      return resolveActivityDefinition(activityDefinition, subjectId, practitionerId, organizationId);
   }

   public abstract IBaseResource resolveActivityDefinition(
           T activityDefinition, String patientId, String practitionerId, String organizationId);

   public abstract Object resolveParameterValue(IBase value);

   public abstract IBaseResource getSubject(String subjectType);

   public void resolveDynamicValue(String language, String expression, String libraryUrl,
                                   String path, IBaseResource resource, String subjectType) {
      if (language == null) {
         logger.error("Missing language type for the dynamicValue");
         throw new IllegalArgumentException("Missing language type for the dynamicValue");
      }
      else if (expression == null) {
         logger.error("Missing expression for the dynamicValue");
         throw new IllegalArgumentException("Missing expression for the dynamicValue");
      }
      else if (path == null) {
         logger.error("Missing element path for the dynamicValue");
         throw new IllegalArgumentException("Missing element path for the dynamicValue");
      }
      IBase value = null;
      switch (language) {
         case "text/cql":
         case "text/cql.expression":
         case "text/cql-expression":
            logger.warn("CQL expressions in ActivityDefinition dynamicActions is not supported.");
            break;
         case "text/cql.name":
         case "text/cql-name":
         case "text/cql.identifier":
         case "text/cql-identifier":
            IBaseParameters parametersResult = libraryProcessor.evaluate(libraryUrl, subjectId,
                    parameters, contentEndpoint, terminologyEndpoint, dataEndpoint, null, Collections.singleton(expression));
            if (parametersResult == null) {
               break;
            }
            // TODO: Lists are represented as repeating parameter elements.
            value = (IBase) resolveParameterValue(ParametersUtil.getNamedParameter(fhirContext, parametersResult, expression).orElse(null));
            break;
         case "text/fhirpath":
            List<IBase> outputs;
            try {
               outputs = fhirPath.evaluate(getSubject(subjectType), expression, IBase.class);
            } catch (FhirPathExecutionException e) {
               throw new IllegalArgumentException("Error evaluating FHIRPath expression", e);
            }
            if (outputs != null && outputs.size() == 1) {
               value = outputs.get(0);
            }
            else {
               throw new IllegalArgumentException("Expected only one value when evaluating FHIRPath expression: " + expression);
            }
            break;
         default:
            logger.warn("An action language other than CQL was found: {}", language);
            break;
      }
      modelResolver.setValue(resource, path, value);
   }

}
