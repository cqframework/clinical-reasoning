package org.opencds.cqf.fhir.cr.measure.common;

public class MeasureInfo {
  public static final String EXT_URL = "http://hl7.org/fhir/StructureDefinition/cqf-measureInfo";
  public static final String MEASURE_PREFIX = "http://hl7.org/fhir/us/cqfmeasures/";

  public static final String MEASURE = "measure";
  public static final String GROUP_ID = "groupId";
  public static final String POPULATION_ID = "populationId";

  private String measure;
  private String groupId;
  private String populationId;

  public String getMeasure() {
    return measure;
  }

  public MeasureInfo withMeasure(String canonical) {
    this.measure = canonical;
    return this;
  }

  public String getGroupId() {
    return groupId;
  }

  public MeasureInfo withGroupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  public String getPopulationId() {
    return populationId;
  }

  public MeasureInfo withPopulationId(String populationId) {
    this.populationId = populationId;
    return this;
  }
}
