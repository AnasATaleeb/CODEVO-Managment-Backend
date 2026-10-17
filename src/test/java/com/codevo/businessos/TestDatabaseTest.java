package com.codevo.businessos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseTest {
    @TempDir Path directory;

    @Test void readsOnlyTestSettingsAndPreservesLiteralPasswords() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, """
            # Values use the same literal format as the Python launchers.
            SPRING_DATASOURCE_URL=jdbc:mysql://remote.example/application
            SPRING_DATASOURCE_PASSWORD=application-only-password
            TEST_DATABASE_URL=jdbc:mysql://127.0.0.1:3307/isolated_test?connectionTimeZone=UTC
            TEST_DATABASE_USERNAME='local_test'
            TEST_DATABASE_PASSWORD="literal$word#with=equals\\and${braces}"
            TEST_MIGRATION_DATABASE_URL=jdbc:mysql://localhost:3307/upgrade_test
            """);

        var settings = TestDatabase.settings(Map.of(), directory.resolve("missing.env"), file);
        TestDatabase database = TestDatabase.from(settings);

        assertThat(database.url).isEqualTo("jdbc:mysql://127.0.0.1:3307/isolated_test?connectionTimeZone=UTC");
        assertThat(database.username).isEqualTo("local_test");
        assertThat(database.password).isEqualTo("literal$word#with=equals\\and${braces}");
        assertThat(settings).containsKey("TEST_MIGRATION_DATABASE_URL");
        assertThat(settings).doesNotContainKeys("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_PASSWORD");
    }

    @Test void processEnvironmentOverridesFileValues() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "TEST_DATABASE_PASSWORD=file-password\nTEST_DATABASE_USERNAME=file_user\n");

        var settings = TestDatabase.settings(Map.of("TEST_DATABASE_PASSWORD", "process-password"), file);
        TestDatabase database = TestDatabase.from(settings);

        assertThat(database.password).isEqualTo("process-password");
        assertThat(database.username).isEqualTo("file_user");
    }

    @Test void explicitEmptyProcessPasswordFailsInsteadOfUsingFilePassword() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "TEST_DATABASE_PASSWORD=file-password\n");
        var settings = TestDatabase.settings(Map.of("TEST_DATABASE_PASSWORD", ""), file);

        assertThatThrownBy(() -> TestDatabase.from(settings))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Set TEST_DATABASE_PASSWORD")
            .hasMessageNotContaining("file-password");
    }

    @Test void missingPasswordFailsBeforeDatabaseStartupWithoutUsingApplicationCredentials() {
        var settings = TestDatabase.settings(Map.of("SPRING_DATASOURCE_PASSWORD", "application-password"),
            directory.resolve("missing.env"));

        assertThatThrownBy(() -> TestDatabase.from(settings))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("TEST_DATABASE_PASSWORD", ".env", "process environment")
            .hasMessageNotContaining("application-password");
    }

    @Test void rejectsRemoteHostsAndApplicationSchemasEvenWhenCredentialsExist() {
        for (String url : new String[] {
                "jdbc:mysql://remote.example/codevo_test", "jdbc:mysql://127.0.0.1/codevo",
                "jdbc:mysql://localhost/codevo_test/other", "", "jdbc:postgresql://localhost/codevo_test"}) {
            assertThatThrownBy(() -> TestDatabase.from(Map.of(
                "TEST_DATABASE_URL", url, "TEST_DATABASE_PASSWORD", "test-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback MySQL schema ending in _test");
        }
    }

    @Test void rejectsEmptyUsernameBeforeDatabaseStartup() {
        assertThatThrownBy(() -> TestDatabase.from(Map.of(
            "TEST_DATABASE_USERNAME", "", "TEST_DATABASE_PASSWORD", "test-password")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Set TEST_DATABASE_USERNAME");
    }

    @Test void malformedEnvironmentReportsLineNumberWithoutItsContents() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "# comment\nsecret-without-separator\n");

        assertThatThrownBy(() -> TestDatabase.settings(Map.of(), file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("line 2")
            .hasMessageNotContaining("secret-without-separator");
    }
}
