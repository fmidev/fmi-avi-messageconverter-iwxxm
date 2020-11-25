package fi.fmi.avi.converter.iwxxm;

import java.math.BigInteger;
import java.util.List;

import net.opengis.gml32.AbstractGeometryType;
import net.opengis.gml32.DirectPositionType;

public final class CRSMappers {
    private CRSMappers() {
        throw new AssertionError();
    }

    public static CRSMapper<AbstractGeometryType> abstractGeometryType() {
        return AbstractGeometryTypeMapper.INSTANCE;
    }

    public static CRSMapper<DirectPositionType> directPositionType() {
        return DirectPositionTypeMapper.INSTANCE;
    }

    private enum AbstractGeometryTypeMapper implements CRSMapper<AbstractGeometryType> {
        INSTANCE;

        @Override
        public String getName(final AbstractGeometryType type) {
            return type.getSrsName();
        }

        @Override
        public void setName(final AbstractGeometryType type, final String value) {
            type.setSrsName(value);
        }

        @Override
        public BigInteger getDimension(final AbstractGeometryType type) {
            return type.getSrsDimension();
        }

        @Override
        public void setDimension(final AbstractGeometryType type, final BigInteger value) {
            type.setSrsDimension(value);
        }

        @Override
        public List<String> getAxisLabels(final AbstractGeometryType type) {
            return type.getAxisLabels();
        }

        @Override
        public List<String> getUomLabels(final AbstractGeometryType type) {
            return type.getUomLabels();
        }
    }

    private enum DirectPositionTypeMapper implements CRSMapper<DirectPositionType> {
        INSTANCE;

        @Override
        public String getName(final DirectPositionType type) {
            return type.getSrsName();
        }

        @Override
        public void setName(final DirectPositionType type, final String value) {
            type.setSrsName(value);
        }

        @Override
        public BigInteger getDimension(final DirectPositionType type) {
            return type.getSrsDimension();
        }

        @Override
        public void setDimension(final DirectPositionType type, final BigInteger value) {
            type.setSrsDimension(value);
        }

        @Override
        public List<String> getAxisLabels(final DirectPositionType type) {
            return type.getAxisLabels();
        }

        @Override
        public List<String> getUomLabels(final DirectPositionType type) {
            return type.getUomLabels();
        }
    }
}
