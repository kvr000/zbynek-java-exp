package com.github.kvr000.exp.java.spatial.restjpa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.PlaceDb;
import lombok.Builder;
import lombok.Value;

import java.util.Optional;


@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Place.Builder.class)
public class Place
{
	private Long id;

	private Long version;

	private String name;

	private GeoLocation location;

	public static Place fromDb(PlaceDb db)
	{
		if (db == null) {
			return null;
		}
		return Place.builder()
			.id(db.getId())
			.version(db.getVersion())
			.name(db.getName())
			.location(GeoLocation.fromPoint(db.getLocation()))
			.build();
	}

	public PlaceDb toDb()
	{
		PlaceDb db = new PlaceDb();
		db.setId(getId());
		db.setVersion(getVersion());
		db.setName(getName());
		db.setLocation(Optional.ofNullable(getLocation()).map(GeoLocation::toPoint).orElse(null));
		return db;
	}

	@JsonPOJOBuilder(withPrefix = "")
	public static final class Builder
	{
	}
}
