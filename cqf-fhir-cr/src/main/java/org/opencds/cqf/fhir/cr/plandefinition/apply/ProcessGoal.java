package org.opencds.cqf.fhir.cr.plandefinition.apply;

import java.util.Collections;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public class ProcessGoal {
    public ProcessGoal() {}

    public IBaseResource convertGoal(ApplyRequest request, IBaseBackboneElement goal) {
        var fhirVersion = request.getFhirVersion();
        return switch (fhirVersion) {
            case DSTU3 -> convertDstu3Goal(request.getSubjectId(), goal);
            case R4 -> convertR4Goal(request.getSubjectId(), goal);
            case R5 -> convertR5Goal(request.getSubjectId(), goal);
            default -> null;
        };
    }

    private IBaseResource convertDstu3Goal(IIdType subjectId, IBaseBackboneElement element) {
        var goal = (org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionGoalComponent) element;
        var myGoal = new org.hl7.fhir.dstu3.model.Goal();
        myGoal.setCategory(Collections.singletonList(goal.getCategory()));
        myGoal.setDescription(goal.getDescription());
        myGoal.setPriority(goal.getPriority());
        myGoal.setStart(goal.getStart());
        myGoal.setStatus(org.hl7.fhir.dstu3.model.Goal.GoalStatus.PROPOSED);
        myGoal.setSubject(new org.hl7.fhir.dstu3.model.Reference(subjectId));

        var goalTarget = goal.hasTarget()
                ? goal.getTarget().stream()
                        .map(target -> {
                            var myTarget = new org.hl7.fhir.dstu3.model.Goal.GoalTargetComponent();
                            myTarget.setDetail(target.getDetail());
                            myTarget.setMeasure(target.getMeasure());
                            myTarget.setDue(target.getDue());
                            myTarget.setExtension(target.getExtension());
                            return myTarget;
                        })
                        .toList()
                        .get(0)
                : null;
        myGoal.setTarget(goalTarget);
        return myGoal;
    }

    private IBaseResource convertR4Goal(IIdType subjectId, IBaseBackboneElement element) {
        var goal = (org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalComponent) element;
        var myGoal = new org.hl7.fhir.r4.model.Goal();
        myGoal.setCategory(Collections.singletonList(goal.getCategory()));
        myGoal.setDescription(goal.getDescription());
        myGoal.setPriority(goal.getPriority());
        myGoal.setStart(goal.getStart());
        myGoal.setLifecycleStatus(org.hl7.fhir.r4.model.Goal.GoalLifecycleStatus.PROPOSED);
        myGoal.setSubject(new org.hl7.fhir.r4.model.Reference(subjectId));

        myGoal.setTarget(goal.getTarget().stream()
                .map(target -> {
                    org.hl7.fhir.r4.model.Goal.GoalTargetComponent myTarget =
                            new org.hl7.fhir.r4.model.Goal.GoalTargetComponent();
                    myTarget.setDetail(target.getDetail());
                    myTarget.setMeasure(target.getMeasure());
                    myTarget.setDue(target.getDue());
                    myTarget.setExtension(target.getExtension());
                    return myTarget;
                })
                .collect(Collectors.toList()));
        return myGoal;
    }

    private IBaseResource convertR5Goal(IIdType subjectId, IBaseBackboneElement element) {
        var goal = (org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionGoalComponent) element;
        var myGoal = new org.hl7.fhir.r5.model.Goal();
        myGoal.setCategory(Collections.singletonList(goal.getCategory()));
        myGoal.setDescription(goal.getDescription());
        myGoal.setPriority(goal.getPriority());
        myGoal.setStart(goal.getStart());
        myGoal.setLifecycleStatus(org.hl7.fhir.r5.model.Goal.GoalLifecycleStatus.PROPOSED);
        myGoal.setSubject(new org.hl7.fhir.r5.model.Reference(subjectId));

        myGoal.setTarget(goal.getTarget().stream()
                .map(target -> {
                    org.hl7.fhir.r5.model.Goal.GoalTargetComponent myTarget =
                            new org.hl7.fhir.r5.model.Goal.GoalTargetComponent();
                    myTarget.setDetail(target.getDetail());
                    myTarget.setMeasure(target.getMeasure());
                    myTarget.setDue(target.getDue());
                    myTarget.setExtension(target.getExtension());
                    return myTarget;
                })
                .collect(Collectors.toList()));
        return myGoal;
    }
}
