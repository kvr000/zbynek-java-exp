package com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import net.dryuf.geo.model.GeoLocation;


@Entity
@Table(name = "Store", indexes = @Index(name = "idx_Store_location", columnList = "location", unique = false))
@Data
public class StoreDb
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Version
	private Long version;

	private String name;

	@Column(columnDefinition = "geometry(POINT, 4326)")
	private GeoLocation location;
}
