package com.github.kvr000.exp.java.spatial.restjpa.jpa;

import com.github.kvr000.exp.java.spatial.restjpa.model.GeoLocation;
import org.geolatte.geom.jts.JTS;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;


public class GeoLocationTypeContributor implements TypeContributor
{
	private static final WKBReader WKB_READER = new WKBReader();

	private static final WKBWriter WKB_WRITER = new WKBWriter();

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry)
	{
		typeContributions.contributeJavaType(new GeoLocationJavaType());
	}

	public static class GeoLocationJavaType extends AbstractJavaType<GeoLocation>
	{

		protected GeoLocationJavaType()
		{
			super(GeoLocation.class);
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return indicators.getJdbcType(SqlTypes.GEOMETRY);
		}


		@SuppressWarnings("unchecked")
		@Override
		public <X> X unwrap(GeoLocation value, Class<X> type, WrapperOptions options)
		{
			if (value == null) {
				return null;
			}
			else if (CharSequence.class.isAssignableFrom(type)) {
				return (X) toString(value);
			}
			if ( org.geolatte.geom.Geometry.class.isAssignableFrom(type) ) {
				return (X) JTS.from(value.toPoint());
			}

			if ( org.locationtech.jts.geom.Geometry.class.isAssignableFrom(type) ) {
				return (X) value.toPoint();
			}
			else {
				throw unknownUnwrap(type);
			}
		}

		@Override
		public <X> GeoLocation wrap(X value, WrapperOptions options)
		{
			if (value == null) {
				return null;
			}
			else if (value instanceof CharSequence string) {
				return fromString(string);
			}
			else if (value instanceof org.geolatte.geom.Point<?> point) {
				return GeoLocation.fromPoint(JTS.to(point));
			}
			else if (value instanceof org.locationtech.jts.geom.Point point) {
				return GeoLocation.fromPoint(point);
			}
			else {
				throw unknownWrap(value.getClass());
			}
		}

		@Override
		public String toString(GeoLocation value) {
			Point point = value.toPoint();
			return WKBWriter.toHex(WKB_WRITER.write(point));
		}

		@Override
		public GeoLocation fromString(CharSequence string)
		{
			try {
				Geometry point = WKB_READER.read(WKBReader.hexToBytes(string.toString()));
				if (point != null && !(point instanceof Point)) {
					throw new ParseException("Expected Point but got: " + point.getClass().getName());
				}
				return GeoLocation.fromPoint((Point) point);
			}
			catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
