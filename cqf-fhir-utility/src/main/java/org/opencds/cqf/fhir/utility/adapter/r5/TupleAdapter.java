package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.LinkedHashMap;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.Tuple;
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter;
import org.opencds.cqf.fhir.utility.adapter.ITupleAdapter;

public class TupleAdapter extends BaseElementAdapter implements ITupleAdapter {

    public TupleAdapter(IBase tuple) {
        super(FhirVersionEnum.R5, tuple);
        if (!(tuple instanceof Tuple)) {
            throw new IllegalArgumentException("object passed as tuple argument is not a Tuple data type");
        }
    }

    @Override
    public Tuple get() {
        return (Tuple) element;
    }

    @Override
    public Object getProperty(String name) {
        return get().children().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var properties = new LinkedHashMap<String, Object>();
        get().children().forEach(c -> properties.put(c.getName(), c.getValues()));
        return properties;
    }

    @Override
    public Object resolvePath(Object target, String path) {
        // A Tuple resolves its named members directly rather than through the FHIR runtime
        // definitions used for structured elements.
        var values = get().children().stream()
                .filter(c -> c.getName().equals(path))
                .filter(org.hl7.fhir.r5.model.Property::hasValues)
                .map(org.hl7.fhir.r5.model.Property::getValues)
                .findFirst()
                .orElse(null);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.size() == 1 ? values.get(0) : values;
    }
}
