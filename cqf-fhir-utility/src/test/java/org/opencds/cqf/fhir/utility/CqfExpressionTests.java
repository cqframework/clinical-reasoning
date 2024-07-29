package org.opencds.cqf.fhir.utility;

import org.hl7.fhir.r4.model.Extension;
import org.junit.jupiter.api.Test;

class CqfExpressionTests {
    @Test
    void test() {
        var ext = new Extension();
        var expression = CqfExpression.of(ext, "http://test.com/Library/test");
    }
}
