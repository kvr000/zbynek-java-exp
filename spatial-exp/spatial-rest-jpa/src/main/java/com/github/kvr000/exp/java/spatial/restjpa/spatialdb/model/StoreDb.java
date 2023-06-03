package com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model;

import com.github.kvr000.exp.java.spatial.restjpa.model.GeoLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;


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
