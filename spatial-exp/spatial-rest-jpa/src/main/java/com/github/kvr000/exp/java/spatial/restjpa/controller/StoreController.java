package com.github.kvr000.exp.java.spatial.restjpa.controller;

import com.github.kvr000.exp.java.spatial.restjpa.model.Store;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.StoreDb;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.repository.StoreRepository;
import com.google.common.base.Preconditions;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import net.dryuf.geo.model.GeoLocation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/store")
public class StoreController
{
	@Inject
	private StoreRepository storeRepository;

	@GetMapping("/")
	public @ResponseBody Iterable<Store> listPlaces(
		@RequestParam(required = false) Double lon,
		@RequestParam(required = false) Double lat)
	{
		if ((lon == null) != (lat == null)) {
			throw new BadRequestException("Both lon and lat query parameters must be specified or neither");
		}
		List<StoreDb> result;
		if (lon != null && lat != null) {
			result = storeRepository.listByDistance(GeoLocation.ofLonLat(lon, lat), 1000);
		}
		else {
			result = storeRepository.findAll();
		}
		return result.stream()
			.map(Store::fromDb)
			.collect(Collectors.toList());
	}

	@PostMapping("/")
	public @ResponseBody Store createPlace(@RequestBody Store store)
	{
		try {
			Preconditions.checkArgument(store.getId() == null, "id must not be provided");
			Preconditions.checkArgument(store.getVersion() == null, "version must not be provided");
			Preconditions.checkNotNull(store.getName(), "name must be provided");
			Preconditions.checkNotNull(store.getLocation(), "location must be provided");
			Preconditions.checkArgument(store.getVersion() == null, "version must not be provided");
		}
		catch (IllegalArgumentException|NullPointerException ex) {
			throw new BadRequestException(ex.getMessage());
		}

		StoreDb storeDb = store.toDb();
		storeDb.setVersion(1L);
		return Store.fromDb(storeRepository.save(storeDb));
	}

	@Transactional("spatialdb-TransactionManager")
	@PutMapping("/{id}")
	public @ResponseBody Store updatePlace(@PathVariable("id") long id, @RequestBody Store target)
	{
		try {
			Preconditions.checkArgument(target.getId() == null || target.getId() == id, "id must not be provided or must match the id from URL");
			Preconditions.checkNotNull(target.getVersion(), "version must be provided");
			Preconditions.checkNotNull(target.getName(), "name must be provided");
			Preconditions.checkNotNull(target.getLocation(), "location must be provided");
		}
		catch (IllegalArgumentException|NullPointerException ex) {
			throw new BadRequestException(ex.getMessage());
		}

		StoreDb storeDb = storeRepository.updateAndFlush(id, (storeDb0) -> {
			if (!Objects.equals(storeDb0.getVersion(), target.getVersion())) {
				throw new BadRequestException("version does not match");
			}
			storeDb0.setName(target.getName());
			storeDb0.setLocation(target.getLocation());
		});
		if (storeDb == null) {
			throw new NotFoundException();
		}

		return Store.fromDb(storeDb);
	}

	@DeleteMapping("/{id}")
	public void deletePlace(@PathVariable("id") long id)
	{
		StoreDb placeDb = storeRepository.findById(id).orElseThrow(NotFoundException::new);

		storeRepository.deleteById(placeDb.getId());
	}
}
