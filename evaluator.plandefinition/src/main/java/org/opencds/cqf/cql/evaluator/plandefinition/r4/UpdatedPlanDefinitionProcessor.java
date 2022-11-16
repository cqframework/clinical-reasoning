package org.opencds.cqf.cql.evaluator.plandefinition.r4;

//import ca.uhn.fhir.context.FhirContext;
//import ca.uhn.fhir.fhirpath.IFhirPath;
//import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
//import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
//import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
//import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
//import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
//import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
//import org.opencds.cqf.cql.evaluator.plandefinition.OperationParametersParser;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Objects;
//
//import static java.util.Objects.requireNonNull;

public class UpdatedPlanDefinitionProcessor {

//   private static final Logger logger = LoggerFactory.getLogger(UpdatedPlanDefinitionProcessor.class);
//
//   protected FhirContext fhirContext;
//   protected FhirDal fhirDal;
//   protected IFhirPath fhirPath;
//   protected LibraryProcessor libraryProcessor;
//   protected ExpressionEvaluator expressionEvaluator;
//   protected ActivityDefinitionProcessor activityDefinitionProcessor;
//   protected OperationParametersParser operationParametersParser;
//
//   public UpdatedPlanDefinitionProcessor(
//           FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor,
//           ExpressionEvaluator expressionEvaluator, ActivityDefinitionProcessor activityDefinitionProcessor,
//           OperationParametersParser operationParametersParser) {
//      validateParameters(fhirContext, fhirDal, libraryProcessor, expressionEvaluator,
//              activityDefinitionProcessor, operationParametersParser);
//   }
//
//   private void validateParameters(
//           FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor,
//           ExpressionEvaluator expressionEvaluator, ActivityDefinitionProcessor activityDefinitionProcessor,
//           OperationParametersParser operationParametersParser) {
//      this.fhirContext = Objects.requireNonNullElseGet(fhirContext, FhirContext::forR4Cached);
//      requireNonNull(fhirDal, "fhirDal can not be null");
//      this.fhirDal = fhirDal;
//      if (libraryProcessor != null) {
//         this.libraryProcessor = libraryProcessor;
//      }
//      if (expressionEvaluator != null) {
//         this.expressionEvaluator = expressionEvaluator;
//      }
//      if (libraryProcessor == null && expressionEvaluator == null) {
//         throw new IllegalArgumentException("A LibraryProcessor or ExpressionEvaluator must be provided");
//      }
//      this.activityDefinitionProcessor = Objects.requireNonNullElseGet(activityDefinitionProcessor,
//              () -> new ActivityDefinitionProcessor(fhirContext, fhirDal, libraryProcessor));
//      this.operationParametersParser = Objects.requireNonNullElseGet(operationParametersParser,
//              () -> new OperationParametersParser(new AdapterFactory(),
//                      new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion())));
//   }



}
