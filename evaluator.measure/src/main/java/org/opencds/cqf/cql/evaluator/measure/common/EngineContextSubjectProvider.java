package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineContextSubjectProvider<SubjectT> implements SubjectProvider {

  private static final Logger logger = LoggerFactory.getLogger(EngineContextSubjectProvider.class);

  protected Context context;
  protected String modelUri;
  protected String subjectType;

  protected Function<SubjectT, String> getId;
  Function<Object, String> idGet;

  public EngineContextSubjectProvider(Context context, String modelUri,
      Function<SubjectT, String> getId) {
    this.context = context;
    this.modelUri = modelUri;
    this.getId = getId;
  }

  public Iterable<String> getSubjects(MeasureEvalType type, String subjectId) {
    switch (type) {
      case PATIENT:
      case SUBJECT:
        return getIndividualSubjectId(subjectId);
      case SUBJECTLIST:
      case PATIENTLIST:
        return this.getPractitionerSubjectIds(subjectId); // here
      case POPULATION:
        return this.getAllSubjectIds(); // here
      default:
        if (subjectId != null) {
          return getIndividualSubjectId(subjectId);
        } else {
          return getAllSubjectIds();
        }
    }
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

    return Collections.singletonList(parsedSubjectId);
  }


  protected DataProvider getDataProvider() {
    return this.context.resolveDataProviderByModelUri(this.modelUri);
  }

  protected Iterable<String> getAllSubjectIds() {
    this.subjectType = "Patient";

    Iterable<Object> subjectRetrieve = this.getDataProvider().retrieve(null, null, null,
        subjectType, null, null, null, null, null, null, null, null);

    return new IdExtractingIterable(subjectRetrieve, idGet);
  }

  protected Iterable<String> getPractitionerSubjectIds(String practitionerRef) {
    this.subjectType = "Patient";

    if (!practitionerRef.contains("/")) {
      practitionerRef = "Practitioner/" + practitionerRef;
    }

    Iterable<Object> subjectRetrieve =
        this.getDataProvider().retrieve("Practitioner", "generalPractitioner", practitionerRef,
            subjectType, null, null, null, null, null, null, null, null);

    return new IdExtractingIterable(subjectRetrieve, idGet);
  }

  public class IdExtractingIterable implements Iterable<String> {
    Iterable<Object> iterableToWrap;
    Function<Object, String> idExtractor;

    public IdExtractingIterable(Iterable<Object> iterableToWrap,
        Function<Object, String> idExtractor) {
      this.iterableToWrap = iterableToWrap;
      this.idExtractor = idExtractor;
    }


    @Override
    public Iterator<String> iterator() {
      return new SubjectIterator(this.iterableToWrap.iterator(), this.idExtractor);
    }

    class SubjectIterator implements Iterator<String> {
      Iterator<Object> iteratorToWrap;
      Function<Object, String> idExtractor;

      public SubjectIterator(Iterator<Object> iteratorToWrap,
          Function<Object, String> idExtractor) {
        this.iteratorToWrap = iteratorToWrap;
        this.idExtractor = idExtractor;
      }

      @Override
      public boolean hasNext() {
        return this.iteratorToWrap.hasNext();
      }

      @Override
      public String next() {
        return this.idExtractor.apply(this.iteratorToWrap.next());
      }
    }
  }
}
