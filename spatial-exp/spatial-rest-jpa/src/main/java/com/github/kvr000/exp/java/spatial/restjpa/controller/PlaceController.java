package com.github.kvr000.exp.java.spatial.restjpa.controller;

import com.github.kvr000.exp.java.spatial.restjpa.model.GeoLocation;
import com.github.kvr000.exp.java.spatial.restjpa.model.Place;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.PlaceDb;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.repository.PlaceRepository;
import com.google.common.base.Preconditions;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
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

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/place")
public class PlaceController
{
	@Inject
	private PlaceRepository placeRepository;

	@GetMapping("/")
	public @ResponseBody Iterable<Place> listPlaces(
		@RequestParam(required = false) Double lon,
		@RequestParam(required = false) Double lat)
	{
		if ((lon == null) != (lat == null)) {
			throw new BadRequestException("Both lon and lat query parameters must be specified or neither");
		}
		List<PlaceDb> result;
		if (lon != null && lat != null) {
			result = placeRepository.listByDistance(GeoLocation.ofLonLat(lon, lat).toPoint(), 1000);
		}
		else {
			result = placeRepository.findAll();
		}
		return result.stream()
			.map(Place::fromDb)
			.collect(Collectors.toList());
	}

	@PostMapping("/")
	public @ResponseBody Place createPlace(@RequestBody Place place)
	{
		try {
			Preconditions.checkArgument(place.getId() == null, "id must not be provided");
			Preconditions.checkArgument(place.getVersion() == null, "version must not be provided");
			Preconditions.checkNotNull(place.getName(), "name must be provided");
			Preconditions.checkNotNull(place.getLocation(), "location must be provided");
			Preconditions.checkArgument(place.getVersion() == null, "version must not be provided");
		}
		catch (IllegalArgumentException|NullPointerException ex) {
			throw new BadRequestException(ex.getMessage());
		}

		return Place.fromDb(placeRepository.save(place.toDb()));
	}

	@Transactional("spatialdb-TransactionManager")
	@PutMapping("/{id}")
	public @ResponseBody Place updatePlace(@PathVariable("id") long id, @RequestBody Place place)
	{
		try {
			Preconditions.checkArgument(place.getId() == null || place.getId() == id, "id must not be provided or must match the id from URL");
			Preconditions.checkNotNull(place.getVersion(), "version must be provided");
			Preconditions.checkNotNull(place.getName(), "name must be provided");
			Preconditions.checkNotNull(place.getLocation(), "location must be provided");
		}
		catch (IllegalArgumentException|NullPointerException ex) {
			throw new BadRequestException(ex.getMessage());
		}

		PlaceDb placeDb = placeRepository.updateAndFlush(id, (placeDb0) -> {
			if (!Objects.equals(placeDb0.getVersion(), place.getVersion())) {
				throw new BadRequestException("version does not match");
			}
			placeDb0.setName(place.getName());
			placeDb0.setLocation(place.getLocation().toPoint());
		});
		if (placeDb == null) {
			throw new NotFoundException();
		}

		return Place.fromDb(placeDb);
	}

	@DeleteMapping("/{id}")
	public void deletePlace(@PathVariable("id") long id)
	{
		PlaceDb placeDb = placeRepository.findById(id).orElseThrow(NotFoundException::new);

		placeRepository.deleteById(placeDb.getId());
	}
}
