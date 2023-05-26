package org.opencds.cqf.cql.evaluator.questionnaire.r4.nestedquestionnaireitem;

import org.hl7.fhir.instance.model.api.IBaseDatatype;

import java.util.List;

public class MockIBaseDatatype implements IBaseDatatype {
  static final long serialVersionUID = -3387516993124229948L;
  private String mockValue;
  public void setMockValue(String theValue) {
    this.mockValue = theValue;
  }
  @Override
  public String toString() {return mockValue;}
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
