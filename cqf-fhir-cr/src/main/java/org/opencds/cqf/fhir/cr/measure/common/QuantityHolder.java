package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Objects;
import java.util.StringJoiner;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.Quantity;

public class QuantityHolder<T extends ICompositeType> {

    private final String id;

    @Nullable
    private final T quantity;

    public QuantityHolder(String id, @Nullable T quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    @Nullable
    public T getQuantity() {
        return this.quantity;
    }

    public boolean hasValueQuantity() {
        return this.quantity != null;
    }

    public Quantity getValueQuantity() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuantityHolder<?> that = (QuantityHolder<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", QuantityHolder.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("quantity=" + quantity)
                .toString();
    }
}
