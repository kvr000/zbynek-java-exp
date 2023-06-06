package com.github.kvr000.exp.java.spatial.restjpa.controller;

import com.github.kvr000.exp.java.spatial.restjpa.model.Target;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.TargetDb;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.repository.TargetRepository;
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

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/target")
public class TargetController
{
	@Inject
	private TargetRepository targetRepository;

	@GetMapping("/")
	public @ResponseBody Iterable<Target> listPlaces(
		@RequestParam(required = false) Double lon,
		@RequestParam(required = false) Double lat)
	{
		if ((lon == null) != (lat == null)) {
			throw new BadRequestException("Both lon and lat query parameters must be specified or neither");
		}
		List<TargetDb> result;
		if (lon != null && lat != null) {
			result = targetRepository.listByDistance(GeoLocation.ofLonLat(lon, lat), 1000);
		}
		else {
			result = targetRepository.findAll();
		}
		return result.stream()
			.map(Target::fromDb)
			.collect(Collectors.toList());
	}

	@PostMapping("/")
	public @ResponseBody Target createPlace(@RequestBody Target target)
	{
		try {
			Preconditions.checkArgument(target.getId() == null, "id must not be provided");
			Preconditions.checkArgument(target.getVersion() == null, "version must not be provided");
			Preconditions.checkNotNull(target.getName(), "name must be provided");
			Preconditions.checkNotNull(target.getLocation(), "location must be provided");
			Preconditions.checkArgument(target.getVersion() == null, "version must not be provided");
		}
		catch (IllegalArgumentException|NullPointerException ex) {
			throw new BadRequestException(ex.getMessage());
		}

		return Target.fromDb(targetRepository.save(target.toDb()));
	}

	@Transactional("spatialdb-TransactionManager")
	@PutMapping("/{id}")
	public @ResponseBody Target updatePlace(@PathVariable("id") long id, @RequestBody Target target)
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

		TargetDb placeDb = targetRepository.updateAndFlush(id, (targetDb0) -> {
			if (!Objects.equals(targetDb0.getVersion(), target.getVersion())) {
				throw new BadRequestException("version does not match");
			}
			targetDb0.setName(target.getName());
			targetDb0.setLocation(target.getLocation());
		});
		if (placeDb == null) {
			throw new NotFoundException();
		}

		return Target.fromDb(placeDb);
	}

	@DeleteMapping("/{id}")
	public void deletePlace(@PathVariable("id") long id)
	{
		TargetDb placeDb = targetRepository.findById(id).orElseThrow(NotFoundException::new);

		targetRepository.deleteById(placeDb.getId());
	}
}
