// package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

// import static com.google.common.base.Preconditions.checkNotNull;

// import org.hl7.fhir.r4.model.ActivityDefinition;
// import org.hl7.fhir.r4.model.Task;
// import org.hl7.fhir.r4.model.Task.TaskStatus;
// import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
// import org.opencds.cqf.fhir.cr.common.ICpgRequest;

// public class CustomActivityResolver extends BaseRequestResourceResolver {
//     private final ActivityDefinition activityDefinition;

//     public CustomActivityResolver(ActivityDefinition activityDefinition) {
//         checkNotNull(activityDefinition);
//         this.activityDefinition = activityDefinition;
//     }

//     @Override
//     public Task resolve(ICpgRequest request) {
//         // Do custom logic

//         var task = new Task();
//         task.setStatus(TaskStatus.COMPLETED);
//         task.addOutput(new Task.TaskOutputComponent());

//         return task;
//     }
// }
