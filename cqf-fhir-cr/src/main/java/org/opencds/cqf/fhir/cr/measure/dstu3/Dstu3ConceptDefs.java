package org.opencds.cqf.fhir.cr.measure.dstu3;

import jakarta.annotation.Nullable;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;

public final class Dstu3ConceptDefs {
    private Dstu3ConceptDefs() {}

    @Nullable
    public static CodeableConcept toConcept(@Nullable ConceptDef c) {
        if (c == null) return null;
        var cc = new CodeableConcept().setText(c.text());
        for (var cd : c.codes()) {
            cc.addCoding(toCoding(cd));
        }
        return cc;
    }

    public static Coding toCoding(CodeDef c) {
        var cd = new Coding();
        cd.setSystem(c.system());
        cd.setCode(c.code());
        cd.setVersion(c.version());
        cd.setDisplay(c.display());
        return cd;
    }
}
