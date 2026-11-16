package co.edu.udistrital.mdp.pets.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class NewmanEnvironmentGenerator {

	private NewmanEnvironmentGenerator() {
	}

	public static void main(String[] args) throws IOException {
		String adminEmail = System.getenv("POSTMAN_ADMIN_EMAIL");
		String adminPassword = System.getenv("POSTMAN_ADMIN_PASSWORD");
		if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.length() < 8)
			throw new IllegalStateException(
					"Set POSTMAN_ADMIN_EMAIL and POSTMAN_ADMIN_PASSWORD (at least 8 characters) before running verify");

		ObjectMapper mapper = new ObjectMapper();
		Path source = Path.of("collections", "EntornoIT.postman_environment.json");
		Path destination = Path.of("target", "EntornoIT.postman_environment.json");
		ObjectNode environment = (ObjectNode) mapper.readTree(source.toFile());
		ArrayNode values = (ArrayNode) environment.get("values");
		setValue(values, "adminEmail", adminEmail);
		setValue(values, "adminPassword", adminPassword);
		Files.createDirectories(destination.getParent());
		mapper.writerWithDefaultPrettyPrinter().writeValue(destination.toFile(), environment);
		destination.toFile().deleteOnExit();
	}

	private static void setValue(ArrayNode values, String key, String value) {
		for (int index = 0; index < values.size(); index++) {
			ObjectNode entry = (ObjectNode) values.get(index);
			if (key.equals(entry.path("key").asText())) {
				entry.put("value", value);
				entry.put("enabled", true);
				return;
			}
		}
		ObjectNode entry = values.addObject();
		entry.put("key", key);
		entry.put("value", value);
		entry.put("enabled", true);
	}
}
