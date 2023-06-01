package com.github.kvr000.exp.java.spatial.restjpa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Value;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;


@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = GeoLocation.Builder.class)
public class GeoLocation
{
	public static final GeometryFactory GEO_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	private final double lon;

	private final double lat;

	public static GeoLocation ofLonLat(double lon, double lat)
	{
		return new GeoLocation(lon, lat);
	}

	public static GeoLocation fromPoint(Point point)
	{
		if (point == null) {
			return null;
		}

		Preconditions.checkArgument(!point.isEmpty());
		Preconditions.checkArgument(Double.isNaN(point.getCoordinate().getZ()));

		return GeoLocation.builder()
			.lon(point.getX())
			.lat(point.getY())
			.build();
	}

	public Point toPoint()
	{
		return GEO_FACTORY.createPoint(new Coordinate(lon, lat));
	}

	@JsonPOJOBuilder(withPrefix = "")
	public static final class Builder
	{
	}
}
