package com.github.kvr000.exp.java.spatial.restjpa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.StoreDb;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.TargetDb;
import lombok.Builder;
import lombok.Value;
import net.dryuf.geo.model.GeoLocation;


@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Store.Builder.class)
public class Store
{
	private Long id;

	private Long version;

	private String name;

	private GeoLocation location;

	public static Store fromDb(StoreDb db)
	{
		if (db == null) {
			return null;
		}
		return Store.builder()
			.id(db.getId())
			.version(db.getVersion())
			.name(db.getName())
			.location(db.getLocation())
			.build();
	}

	public StoreDb toDb()
	{
		StoreDb db = new StoreDb();
		db.setId(getId());
		db.setVersion(getVersion());
		db.setName(getName());
		db.setLocation(getLocation());
		return db;
	}

	@JsonPOJOBuilder(withPrefix = "")
	public static final class Builder
	{
	}
}
