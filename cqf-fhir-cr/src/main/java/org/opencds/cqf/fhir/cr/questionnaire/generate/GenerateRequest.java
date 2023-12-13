// package org.opencds.cqf.fhir.cr.questionnaire.generate;

// import org.hl7.fhir.instance.model.api.IBaseBundle;
// import org.hl7.fhir.instance.model.api.IBaseParameters;
// import org.hl7.fhir.instance.model.api.IIdType;
// import org.opencds.cqf.cql.engine.model.ModelResolver;
// import org.opencds.cqf.fhir.cql.LibraryEngine;
// import org.opencds.cqf.fhir.cr.common.IOperationRequest;

// import ca.uhn.fhir.context.FhirVersionEnum;

// public class GenerateRequest implements IOperationRequest {
//     private final IIdType subjectId;
//     private final IBaseParameters parameters;
//     private final IBaseBundle bundle;
//     private final LibraryEngine libraryEngine;
//     private final ModelResolver modelResolver;
//     private final FhirVersionEnum fhirVersion;
//     private final String defaultLibraryUrl;
    
//     public GenerateRequest(
//             IIdType subjectId,
//             IBaseParameters parameters,
//             IBaseBundle bundle,
//             LibraryEngine libraryEngine,
//             ModelResolver modelResolver) {
//         this.subjectId = subjectId;
//         this.parameters = parameters;
//         this.bundle = bundle;
//         this.libraryEngine = libraryEngine;
//         this.modelResolver = modelResolver;
//         this.fhirVersion = FhirVersionEnum.R4;
//         this.defaultLibraryUrl = "";
//     }

//     @Override
//     public IIdType getSubjectId() {
//         return subjectId;
//     }

//     @Override
//     public IBaseBundle getBundle() {
//         return bundle;
//     }

//     @Override
//     public IBaseParameters getParameters() {
//         return parameters;
//     }

//     @Override
//     public LibraryEngine getLibraryEngine() {
//         return libraryEngine;
//     }

//     @Override
//     public ModelResolver getModelResolver() {
//         return modelResolver;
//     }

//     @Override
//     public FhirVersionEnum getFhirVersion() {
//         return fhirVersion;
//     }

//     @Override
//     public String getDefaultLibraryUrl() {
//         return defaultLibraryUrl;
//     }
// }
