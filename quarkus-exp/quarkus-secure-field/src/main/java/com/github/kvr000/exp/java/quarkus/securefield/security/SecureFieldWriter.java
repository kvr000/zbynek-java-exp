package com.github.kvr000.exp.java.quarkus.securefield.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Applies JSON input to an entity, enforcing {@link SecureWriteField} restrictions against the passed
 * {@link SecurityIdentity}.
 *
 * The identity is passed explicitly (not injected), so the writer does not depend on an active request context and
 * can be called from any thread.  Obtain the real (unproxied) instance via
 * {@link io.quarkus.security.identity.CurrentIdentityAssociation#getIdentity()}.
 *
 * A restricted field present in the input is rejected (403) unless its value equals the current value, so clients
 * can send back what they received from GET (e.g. admin echoing {@code views}).
 */
@ApplicationScoped
public class SecureFieldWriter
{
	public enum Mode
	{
		/** New object: absent fields keep their defaults. */
		CREATE,
		/** PUT: absent fields writable by caller are reset to null/0, restricted fields are preserved. */
		REPLACE,
		/** PATCH (JSON merge patch): only present fields are modified. */
		MERGE,
	}

	private final Map<Class<?>, Map<String, Field>> fieldsCache = new ConcurrentHashMap<>();

	@Inject
	ObjectMapper objectMapper;

	public <T> T apply(T target, ObjectNode input, Mode mode, SecurityIdentity identity)
	{
		Map<String, Field> fields = fieldsCache.computeIfAbsent(target.getClass(), SecureFieldWriter::collectFields);

		ObjectNode accepted = objectMapper.createObjectNode();
		List<String> denied = new ArrayList<>();
		for (Iterator<Map.Entry<String, JsonNode>> it = input.fields(); it.hasNext(); ) {
			Map.Entry<String, JsonNode> entry = it.next();
			Field field = fields.get(entry.getKey());
			if (field == null) {
				throw error(Response.Status.BAD_REQUEST, "Unknown field: " + entry.getKey());
			}
			if (canWrite(field, identity)) {
				accepted.set(entry.getKey(), entry.getValue());
			}
			else if (!sameValue(objectMapper.valueToTree(readField(field, target)), entry.getValue())) {
				denied.add(entry.getKey());
			}
		}
		if (!denied.isEmpty()) {
			throw error(Response.Status.FORBIDDEN, "Not allowed to modify fields " + denied + " with roles " + identity.getRoles());
		}
		if (mode == Mode.REPLACE) {
			fields.forEach((name, field) -> {
				if (!input.has(name) && canWrite(field, identity)) {
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

	public static boolean canWrite(Field field, SecurityIdentity identity)
	{
		SecureWriteField annotation = field.getAnnotation(SecureWriteField.class);
		return annotation == null || Arrays.stream(annotation.rolesAllowed()).anyMatch(identity::hasRole);
	}

	public static WebApplicationException error(Response.Status status, String message)
	{
		return new WebApplicationException(message, Response.status(status)
			.type(MediaType.APPLICATION_JSON_TYPE)
			.entity(Map.of("status", status.getStatusCode(), "error", message))
			.build());
	}

	private static boolean sameValue(JsonNode current, JsonNode input)
	{
		if (current.isNumber() && input.isNumber()) {
			return current.decimalValue().compareTo(input.decimalValue()) == 0;
		}
		return current.equals(input);
	}

	private static Object readField(Field field, Object target)
	{
		try {
			return field.get(target);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static Map<String, Field> collectFields(Class<?> clazz)
	{
		Map<String, Field> result = new LinkedHashMap<>();
		for (Field field : clazz.getFields()) {
			if (!Modifier.isStatic(field.getModifiers())) {
				result.put(field.getName(), field);
			}
		}
		return result;
	}
}
