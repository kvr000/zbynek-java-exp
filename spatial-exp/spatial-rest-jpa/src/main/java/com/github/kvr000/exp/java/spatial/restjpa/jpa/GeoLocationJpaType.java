package com.github.kvr000.exp.java.spatial.restjpa.jpa;

import com.github.kvr000.exp.java.spatial.restjpa.model.GeoLocation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserTypeSupport;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;


public class GeoLocationJpaType extends UserTypeSupport<GeoLocation>
{
	private static final WKBReader WKB_READER = new WKBReader();

	private static final WKBWriter WKB_WRITER = new WKBWriter();

	public GeoLocationJpaType()
	{
		super(GeoLocation.class, Types.OTHER);
	}

	@Override
	public int getSqlType() {
		return Types.OTHER;
	}

	@Override
	public Class<GeoLocation> returnedClass() {
		return GeoLocation.class;
	}

	@Override
	public GeoLocation nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		PGobject value = (PGobject) rs.getObject(position);
		if (value == null) {
			return null;
		}
		Geometry geometry;
		try {
			geometry = WKB_READER.read(WKBReader.hexToBytes(value.getValue()));
			if (!(geometry instanceof Point)) {
				throw new ParseException("Expected Point, got: " + geometry.getGeometryType());
			}
		}
		catch (ParseException e) {
			throw new SQLException("Cannot convert PGObject to Geometry");
		}
		if (rs.wasNull()) {
			return null;
		}
		final Point point = (Point) geometry;
		double lon = point.getX();
		double lat = point.getY();
		return GeoLocation.ofLonLat(lon, lat);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, GeoLocation value, int index, SharedSessionContractImplementor session) throws SQLException {
		if (value == null) {
			st.setNull(index, Types.OTHER);
		} else {
			PGobject object = new PGobject();
			Point point = value.toPoint();
			object.setValue(WKBWriter.toHex(WKB_WRITER.write(point)));
			object.setType("geometry");
			st.setObject(index, object);
		}
	}
}
