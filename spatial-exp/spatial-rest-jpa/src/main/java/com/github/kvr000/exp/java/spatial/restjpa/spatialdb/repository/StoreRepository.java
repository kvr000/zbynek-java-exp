package com.github.kvr000.exp.java.spatial.restjpa.spatialdb.repository;

import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.StoreDb;
import net.dryuf.geo.model.GeoLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Consumer;


@Repository
public interface StoreRepository extends JpaRepository<StoreDb, Long>
{
	StoreDb findByName(String name);

	@Query("SELECT t from StoreDb t WHERE ST_DistanceSpheroid(t.location, ?1) <= ?2")
	List<StoreDb> listByDistance(GeoLocation location, double distance);

	@Modifying
	default StoreDb updateAndFlush(long id, Consumer<StoreDb> updater)
	{
		StoreDb db = findById(id).orElse(null);
		if (db == null) {
			return null;
		}
		updater.accept(db);
		return saveAndFlush(db);
	}
}
