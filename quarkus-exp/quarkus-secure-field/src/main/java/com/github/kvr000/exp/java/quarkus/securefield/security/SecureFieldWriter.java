package com.github.kvr000.exp.java.quarkus.securefield.security;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;


/**
 * Applies JSON input to a bean, enforcing {@link SecureWriteField} restrictions against the passed
 * {@link SecurityIdentity}.
 *
 * Properties are discovered by Jackson bean introspection (public getters / setters, {@code @JsonProperty} names,
 * {@code @JsonIgnore}, ...), the values are written by Jackson as well, so the behavior matches regular
 * deserialization.  A property is writable when Jackson has a mutator for it (setter / visible field) and the caller
 * has one of {@link SecureWriteField#rolesAllowed()}.
 *
 * A restricted property present in the input is rejected (403) unless its value equals the current value, so clients
 * can send back what they received from GET (e.g. admin echoing {@code views}).
 *
 * The identity is passed explicitly (not injected), so the writer does not depend on an active request context and
 * can be called from any thread.  Obtain the real (unproxied) instance via
 * {@link io.quarkus.security.identity.CurrentIdentityAssociation#getIdentity()}.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class SecureFieldWriter
{
	public enum Mode
	{
		/** New object: absent properties keep their defaults. */
		CREATE,
		/** PUT: absent properties writable by caller are reset to null/0, restricted ones are preserved. */
		REPLACE,
		/** PATCH (JSON merge patch): only present properties are modified. */
		MERGE,
	}

	private final ObjectMapper objectMapper;

	private final Map<Class<?>, Map<String, PropertyAccess>> propertiesCache = new ConcurrentHashMap<>();

	public <T> T apply(T target, ObjectNode input, Mode mode, SecurityIdentity identity)
	{
		Map<String, PropertyAccess> properties = propertiesCache.computeIfAbsent(target.getClass(), this::introspect);

		ObjectNode accepted = objectMapper.createObjectNode();
		List<String> denied = new ArrayList<>();
		for (Iterator<Map.Entry<String, JsonNode>> it = input.fields(); it.hasNext(); ) {
			Map.Entry<String, JsonNode> entry = it.next();
			PropertyAccess property = properties.get(entry.getKey());
			if (property == null) {
				throw error(Response.Status.BAD_REQUEST, "Unknown field: " + entry.getKey());
			}
			if (property.canWrite(identity)) {
				accepted.set(entry.getKey(), entry.getValue());
			}
			else if (!sameValue(property.read(target, objectMapper), entry.getValue())) {
				denied.add(entry.getKey());
			}
		}
		if (!denied.isEmpty()) {
			throw error(Response.Status.FORBIDDEN, "Not allowed to modify fields " + denied + " with roles " + identity.getRoles());
		}
		if (mode == Mode.REPLACE) {
			properties.forEach((name, property) -> {
				if (!input.has(name) && property.canWrite(identity)) {
					accepted.set(name, NullNode.getInstance());
				}
			});
		}
		try {
			return objectMapper.readerForUpdating(target).readValue(accepted);
		}
		catch (IOException ex) {
			throw error(Response.Status.BAD_REQUEST, "Invalid input: " + ex.getMessage());
		}
	}

	public static WebApplicationException error(Response.Status status, String message)
	{
		return new WebApplicationException(message, Response.status(status)
			.type(MediaType.APPLICATION_JSON_TYPE)
			.entity(Map.of("status", status.getStatusCode(), "error", message))
			.build());
	}

	private Map<String, PropertyAccess> introspect(Class<?> clazz)
	{
		BeanDescription description = objectMapper.getDeserializationConfig()
			.introspect(objectMapper.constructType(clazz));
		Map<String, PropertyAccess> result = new LinkedHashMap<>();
		for (BeanPropertyDefinition definition : description.findProperties()) {
			AnnotatedMember accessor = definition.getAccessor();
			if (accessor != null) {
				accessor.fixAccess(true);
			}
			SecureWriteField annotation = Stream.of(definition.getSetter(), definition.getField(), definition.getGetter())
				.filter(member -> member != null)
				.map(member -> member.getAnnotation(SecureWriteField.class))
				.filter(a -> a != null)
				.findFirst()
				.orElse(null);
			result.put(definition.getName(), new PropertyAccess(
				accessor,
				definition.getMutator() != null,
				annotation == null ? null : annotation.rolesAllowed()
			));
		}
		return Map.copyOf(result);
	}

	private static boolean sameValue(JsonNode current, JsonNode input)
	{
		if (current.isNumber() && input.isNumber()) {
			return current.decimalValue().compareTo(input.decimalValue()) == 0;
		}
		return current.equals(input);
	}

	/**
	 * Write access of single bean property.
	 *
	 * @param accessor
	 * 	getter or field to read current value, null if write-only
	 * @param mutable
	 * 	whether Jackson can set the property
	 * @param rolesAllowed
	 * 	roles allowed to write, null if unrestricted
	 */
	private record PropertyAccess(AnnotatedMember accessor, boolean mutable, String[] rolesAllowed)
	{
		boolean canWrite(SecurityIdentity identity)
		{
			return mutable && (rolesAllowed == null || Arrays.stream(rolesAllowed).anyMatch(identity::hasRole));
		}

		JsonNode read(Object target, ObjectMapper objectMapper)
		{
			if (accessor == null) {
				return MissingNode.getInstance();
			}
			JsonNode value = objectMapper.valueToTree(accessor.getValue(target));
			return value == null ? NullNode.getInstance() : value;
		}
	}
}
