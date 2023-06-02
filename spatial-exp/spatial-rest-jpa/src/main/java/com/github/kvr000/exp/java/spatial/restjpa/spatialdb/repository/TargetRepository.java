package com.github.kvr000.exp.java.spatial.restjpa.spatialdb.repository;

import com.github.kvr000.exp.java.spatial.restjpa.model.GeoLocation;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.PlaceDb;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.TargetDb;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.Consumer;


@Repository
public interface TargetRepository extends JpaRepository<TargetDb, Long>
{
	TargetDb findByName(String name);

	default List<TargetDb> listByDistance(GeoLocation location, double distance)
	{
		return listByDistance(location.toPoint(), distance);
	}

	@Query("SELECT t from TargetDb t WHERE ST_DistanceSpheroid(t.location, ?1) <= ?2")
	List<TargetDb> listByDistance(Point location, double distance);

	@Modifying
	default TargetDb updateAndFlush(long id, Consumer<TargetDb> updater)
	{
		TargetDb db = findById(id).orElse(null);
		if (db == null) {
			return null;
		}
		updater.accept(db);
		return saveAndFlush(db);
	}
}
