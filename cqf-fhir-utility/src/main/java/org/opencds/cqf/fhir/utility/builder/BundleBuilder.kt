package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public class BundleBuilder<T extends IBaseBundle> extends BaseResourceBuilder<BundleBuilder<T>, T> {

    private String type;

    private Date timestamp = new Date();

    public BundleBuilder(Class<T> resourceClass) {
        super(resourceClass);
    }

    public BundleBuilder(Class<T> resourceClass, String id) {
        super(resourceClass, id);
    }

    public BundleBuilder(Class<T> resourceClass, String id, String type) {
        this(resourceClass, id);
        checkNotNull(type);

        this.type = type;
    }

    public BundleBuilder<T> withType(String type) {
        checkNotNull(type);

        this.type = type;

        return this;
    }

    public BundleBuilder<T> withTimestamp(Date timestamp) {
        this.timestamp = timestamp;

        return this;
    }

    @Override
    public T build() {
        checkNotNull(type);

        return super.build();
    }

    @Override
    protected void initializeDstu3(T resource) {
        super.initializeDstu3(resource);
        org.hl7.fhir.dstu3.model.Bundle bundle = (org.hl7.fhir.dstu3.model.Bundle) resource;

        bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.valueOf(type));

        bundle.setIdentifier(new org.hl7.fhir.dstu3.model.Identifier()
                .setSystem(getIdentifier().getKey())
                .setValue(getIdentifier().getValue()));
    }

    @Override
    protected void initializeR4(T resource) {
        super.initializeR4(resource);
        org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) resource;

        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.valueOf(type));

        bundle.setIdentifier(new org.hl7.fhir.r4.model.Identifier()
                .setSystem(getIdentifier().getKey())
                .setValue(getIdentifier().getValue()));

        bundle.setTimestamp(timestamp);
    }

    @Override
    protected void initializeR5(T resource) {
        super.initializeR5(resource);
        org.hl7.fhir.r5.model.Bundle bundle = (org.hl7.fhir.r5.model.Bundle) resource;

        bundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.valueOf(type));

        bundle.setIdentifier(new org.hl7.fhir.r5.model.Identifier()
                .setSystem(getIdentifier().getKey())
                .setValue(getIdentifier().getValue()));

        bundle.setTimestamp(timestamp);
    }
}
