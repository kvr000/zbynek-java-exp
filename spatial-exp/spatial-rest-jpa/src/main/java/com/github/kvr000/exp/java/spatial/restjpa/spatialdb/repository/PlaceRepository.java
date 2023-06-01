package com.github.kvr000.exp.java.spatial.restjpa.spatialdb.repository;

import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.PlaceDb;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Consumer;


@Repository
public interface PlaceRepository extends JpaRepository<PlaceDb, Long>
{
	PlaceDb findByName(String name);

	@Query("SELECT p from PlaceDb p WHERE ST_DistanceSpheroid(p.location, ?1) <= ?2")
	List<PlaceDb> listByDistance(Point location, double distance);

	@Modifying
	default PlaceDb updateAndFlush(long id, Consumer<PlaceDb> updater)
	{
		PlaceDb db = findById(id).orElse(null);
		if (db == null) {
			return null;
		}
		updater.accept(db);
		return saveAndFlush(db);
	}
}
