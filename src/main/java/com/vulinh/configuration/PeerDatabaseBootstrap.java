package com.vulinh.configuration;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PeerDatabaseBootstrap {

  private static final List<String> PEER_DATABASES = List.of("spring-base", "spring-base-event");

  // Guard DDL injection since CREATE DATABASE / GRANT cannot use bind parameters.
  private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_-]+");

  private final DataSource dataSource;
  private final ApplicationProperties applicationProperties;

  @PostConstruct
  void createPeerDatabases() throws SQLException {
    if (!applicationProperties.bootstrap().peerDatabase()) {
      log.info("Peer database bootstrapping disabled, skipping");
      return;
    }

    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(true);

      var appUser = connection.getMetaData().getUserName();
      if (!SAFE_IDENTIFIER.matcher(appUser).matches()) {
        throw new IllegalStateException("Unsafe datasource username: " + appUser);
      }

      for (var database : PEER_DATABASES) {
        if (exists(connection, database)) {
          log.debug("Peer database '{}' already exists", database);
        } else {
          create(connection, database);
          log.info("Created peer database '{}'", database);
        }
        grant(connection, database, appUser);
      }
    }
  }

  private static boolean exists(Connection connection, String databaseName) throws SQLException {
    try (var ps = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
      ps.setString(1, databaseName);
      try (var rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private static void create(Connection connection, String databaseName) throws SQLException {
    if (!SAFE_IDENTIFIER.matcher(databaseName).matches()) {
      throw new IllegalArgumentException("Invalid database name: " + databaseName);
    }

    try (var statement = connection.createStatement()) {
      statement.execute(
          """
          CREATE DATABASE "%s"
          """
              .formatted(databaseName));
    }
  }

  private static void grant(Connection connection, String databaseName, String username)
      throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.execute(
          """
          GRANT ALL PRIVILEGES ON DATABASE "%s" TO "%s"
          """
              .formatted(databaseName, username));
    }
  }
}
