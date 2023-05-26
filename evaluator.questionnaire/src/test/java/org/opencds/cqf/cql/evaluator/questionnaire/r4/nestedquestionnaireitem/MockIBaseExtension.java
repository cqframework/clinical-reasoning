package org.opencds.cqf.cql.evaluator.questionnaire.r4.nestedquestionnaireitem;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;

import java.util.List;

public class MockIBaseExtension<T, D> implements IBaseExtension<T, D> {
  IBaseDatatype value;
  static final long serialVersionUID = -3387516993124229948L;
  @Override
  public List<T> getExtension() {
    return null;
  }

  @Override
  public String getUrl() {
    return null;
  }

  @Override
  public IBaseDatatype getValue() {
    return this.value;
  }

  @Override
  public T setUrl(String theUrl) {
    return null;
  }

  @Override
  public T setValue(IBaseDatatype theValue) {
    this.value = theValue;
    return null;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean hasFormatComment() {
    return false;
  }

  @Override
  public List<String> getFormatCommentsPre() {
    return null;
  }

  @Override
  public List<String> getFormatCommentsPost() {
    return null;
  }

  @Override
  public Object getUserData(String theName) {
    return null;
  }

  @Override
  public void setUserData(String theName, Object theValue) {

  }
}
