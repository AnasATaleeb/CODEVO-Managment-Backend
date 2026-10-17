package com.codevo.businessos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Test-only configuration. Application/RDS credentials are never used as fallbacks. */
final class TestDatabase {
    private static final Set<String> KEYS = Set.of(
        "TEST_DATABASE_URL", "TEST_DATABASE_USERNAME", "TEST_DATABASE_PASSWORD", "TEST_MIGRATION_DATABASE_URL");
    private static final String DEFAULT_URL =
        "jdbc:mysql://127.0.0.1:3306/codevo_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true";

    final String url;
    final String username;
    final String password;

    private TestDatabase(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    static void configure(DynamicPropertyRegistry properties) {
        TestDatabase database = from(settings());
        properties.add("spring.datasource.url", () -> database.url);
        properties.add("spring.datasource.username", () -> database.username);
        properties.add("spring.datasource.password", () -> database.password);
        properties.add("app.storage-path", () -> "target/test-private-files");
    }

    static Map<String, String> settings() {
        // Maven/IDE runs use backend as cwd; -f backend/pom.xml may start at the project root.
        return settings(System.getenv(), Path.of(".env"), Path.of("../.env"));
    }

    static Map<String, String> settings(Map<String, String> environment, Path... candidates) {
        Map<String, String> values = new HashMap<>();
        for (Path path : candidates) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                int number = 0;
                for (String raw : Files.readAllLines(path)) {
                    number++;
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int separator = line.indexOf('=');
                    if (separator < 1) {
                        throw new IllegalArgumentException("Invalid environment line " + number + " in " + path);
                    }
                    String key = line.substring(0, separator).trim();
                    if (!KEYS.contains(key)) {
                        continue;
                    }
                    String value = line.substring(separator + 1).trim();
                    if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    // Preserve $, #, backslashes and = literally, as scripts/env_config.py does.
                    values.put(key, value);
                }
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot read test environment file " + path, exception);
            }
            break;
        }
        for (String key : KEYS) {
            if (environment.containsKey(key)) {
                values.put(key, environment.get(key));
            }
        }
        return values;
    }

    static TestDatabase from(Map<String, String> values) {
        String url = values.getOrDefault("TEST_DATABASE_URL", DEFAULT_URL);
        validateUrl(url);
        String username = values.getOrDefault("TEST_DATABASE_USERNAME", "codevo_test");
        requireValue("TEST_DATABASE_USERNAME", username);
        String password = values.get("TEST_DATABASE_PASSWORD");
        requireValue("TEST_DATABASE_PASSWORD", password);
        return new TestDatabase(url, username, password);
    }

    static void validateUrl(String url) {
        if (url == null || !url.matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost)(:[0-9]+)?/[a-zA-Z0-9_]+_test(\\?.*)?")) {
            throw new IllegalArgumentException("Integration tests require a loopback MySQL schema ending in _test");
        }
    }

    private static void requireValue(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + key
                + " in the project .env or process environment before running MySQL tests");
        }
    }
}
