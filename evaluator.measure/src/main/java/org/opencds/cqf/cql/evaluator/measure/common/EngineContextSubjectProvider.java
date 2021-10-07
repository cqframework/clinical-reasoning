package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineContextSubjectProvider<SubjectT> implements SubjectProvider {

    private final static Logger logger = LoggerFactory.getLogger(EngineContextSubjectProvider.class);

    protected Context context;
    protected String modelUri;
    protected String subjectType;

    protected Function<SubjectT, String> getId;
    
    public EngineContextSubjectProvider(Context context, String modelUri, Function<SubjectT, String> getId) {
        this.context = context;
        this.modelUri = modelUri;
        this.getId = getId;
    }

    public List<String> getSubjects(MeasureEvalType type, String subjectId) {
        switch (type) {
            case PATIENT:
            case SUBJECT:
                return getIndividualSubjectId(subjectId);
            case SUBJECTLIST:
            case PATIENTLIST:
                return this.getPractitionerSubjectIds(subjectId);
            case POPULATION:
                return this.getAllSubjectIds();
            default:
                if (subjectId != null) {
                    return getIndividualSubjectId(subjectId);
                } else {
                    return getAllSubjectIds();
                }
        }
    }


    protected DataProvider getDataProvider() {
        return this.context.resolveDataProviderByModelUri(this.modelUri);
    }

    protected List<String> getIndividualSubjectId(String subjectId) {
        String parsedSubjectId = null;
        if (subjectId != null && subjectId.contains("/")) {
            String[] subjectIdParts = subjectId.split("/");
            parsedSubjectId = subjectIdParts[1];
        } else {
            parsedSubjectId = subjectId;
            logger.info("Could not determine subjectType. Defaulting to Patient");
        }

        if (parsedSubjectId == null) {
            throw new IllegalArgumentException("subjectId is required for individual reports.");
        }

        return Collections.singletonList(this.subjectType + "/" + parsedSubjectId);
    }



    @SuppressWarnings("unchecked")
    protected List<String> getAllSubjectIds() {
        this.subjectType = "Patient";
        List<String> subjectIds = new ArrayList<>();
        Iterable<Object> subjectRetrieve = this.getDataProvider().retrieve(null, null, null, subjectType, null, null,
                null, null, null, null, null, null);
        subjectRetrieve.forEach(x -> subjectIds.add(this.getId.apply((SubjectT) x)));
        return subjectIds;
    }

    @SuppressWarnings("unchecked")
    protected List<String> getPractitionerSubjectIds(String practitionerRef) {
        this.subjectType = "Patient";

        if (practitionerRef == null) {
            return getAllSubjectIds();
        }

        List<String> subjectIds = new ArrayList<>();

        if (!practitionerRef.contains("/")) {
            practitionerRef = "Practitioner/" + practitionerRef;
        }

        Iterable<Object> subjectRetrieve = this.getDataProvider().retrieve("Practitioner", "generalPractitioner",
                practitionerRef, subjectType, null, null, null, null, null, null, null, null);
        subjectRetrieve.forEach(x -> subjectIds.add(this.getId.apply((SubjectT) x)));
        return subjectIds;
    }
}
