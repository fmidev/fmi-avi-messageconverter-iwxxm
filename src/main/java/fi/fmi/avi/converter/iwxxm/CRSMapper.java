package fi.fmi.avi.converter.iwxxm;

import java.math.BigInteger;
import java.util.List;

import fi.fmi.avi.model.CoordinateReferenceSystem;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;

public interface CRSMapper<T> {
    String getName(T type);

    void setName(T type, String value);

    BigInteger getDimension(T type);

    void setDimension(T type, BigInteger value);

    List<String> getAxisLabels(T type);

    List<String> getUomLabels(T type);

    default CoordinateReferenceSystemImpl.Builder mergeToBuilder(final T type, final CoordinateReferenceSystemImpl.Builder builder) {
        final String srsName = getName(type);
        if (srsName == null) {
            return builder;
        }
        try {
            if (!srsName.equals(builder.getName())) {
                builder.clear();
            }
        } catch (final IllegalStateException ignored) {
            // no-op
        }
        builder.setName(srsName);
        if (getDimension(type) != null) {
            try {
                builder.setDimension(getDimension(type).intValueExact());
            } catch (final ArithmeticException ignored) {
            }
        }
        if (getAxisLabels(type) != null) {
            builder//
                    .clearAxisLabels()//
                    .addAllAxisLabels(getAxisLabels(type));
        }
        if (getUomLabels(type) != null) {
            builder//
                    .clearUomLabels()//
                    .addAllUomLabels(getUomLabels(type));
        }
        return builder;
    }

    default void setCrsToType(final T type, final CoordinateReferenceSystem crs) {
        if (type == null || crs == null) {
            return;
        }
        setName(type, crs.getName());
        crs.getDimension().ifPresent(dimension -> setDimension(type, BigInteger.valueOf(dimension)));
        if (!crs.getAxisLabels().isEmpty()) {
            final List<String> axisLabels = getAxisLabels(type);
            axisLabels.clear();
            axisLabels.addAll(crs.getAxisLabels());
        }
        if (!crs.getUomLabels().isEmpty()) {
            final List<String> uomLabels = getUomLabels(type);
            uomLabels.clear();
            uomLabels.addAll(crs.getUomLabels());
        }
    }
}
