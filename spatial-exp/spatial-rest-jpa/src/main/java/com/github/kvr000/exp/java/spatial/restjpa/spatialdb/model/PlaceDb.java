package com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import org.locationtech.jts.geom.Point;


@Entity
@Table(name = "Place", indexes = @Index(name = "idx_location", columnList = "location", unique = false))
@Data
public class PlaceDb
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Version
	private Long version;

	private String name;

	private Point location;
}
