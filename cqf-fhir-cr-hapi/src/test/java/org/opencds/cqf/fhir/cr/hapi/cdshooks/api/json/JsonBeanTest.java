package org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.test.util.HasGetterOrSetterForAllJsonFieldsAssert;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

public class JsonBeanTest {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(JsonBeanTest.class);

	@Test
	public void testAllCdsHooksJsonClasses() {
		Reflections reflections = new Reflections("org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json");

		Set<Class<? extends IModelJson>> allJsonClasses =
			reflections.getSubTypesOf(IModelJson.class);

		assertThat(allJsonClasses).contains(CdsServiceJson.class);
		for (Class<? extends IModelJson> item : allJsonClasses) {
			HasGetterOrSetterForAllJsonFieldsAssert.assertThat(item).hasGetterOrSetterForAllJsonFields();
		}

		ourLog.info("Tested {} Json classes", allJsonClasses.size());
	}


}
