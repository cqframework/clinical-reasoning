package org.opencds.cqf.cql.evaluator.fhir.helper;

import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.cql.engine.model.ModelResolver;

import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;

public class NestedValueResolver {
  private final FhirContext fhirContext;
  private final ModelResolver modelResolver;

  public NestedValueResolver(FhirContext fhirContext, ModelResolver modelResolver) {
    this.fhirContext = fhirContext;
    this.modelResolver = modelResolver;
  }

  public void setNestedValue(IBase target, String path, IBase value) {
    var def = (BaseRuntimeElementCompositeDefinition<?>) fhirContext
        .getElementDefinition(target.getClass());
    var identifiers = path.split("\\.");
    for (int i = 0; i < identifiers.length; i++) {
      var identifier = identifiers[i];
      var isList = identifier.contains("[");
      var isLast = i == identifiers.length - 1;
      var index =
          isList ? Character.getNumericValue(identifier.charAt(identifier.indexOf("[") + 1)) : 0;
      var targetPath = isList ? identifier.replaceAll("\\[\\d\\]", "") : identifier;
      var targetDef = def.getChildByName(targetPath);

      var targetValues = targetDef.getAccessor().getValues(target);
      IBase targetValue;
      if (targetValues.size() >= index + 1 && !isLast) {
        targetValue = targetValues.get(index);
      } else {
        var elementDef = targetDef.getChildByName(targetPath);
        if (isLast) {
          targetValue = (IBase) modelResolver.as(value, elementDef.getImplementingClass(), false);
        } else {
          targetValue = elementDef.newInstance(targetDef.getInstanceConstructorArguments());
        }
        targetDef.getMutator().addValue(target, targetValue);
      }
      target = targetValue;
      if (!isLast) {
        var nextDef = fhirContext.getElementDefinition(target.getClass());
        def = (BaseRuntimeElementCompositeDefinition<?>) nextDef;
      }
    }
  }


}
