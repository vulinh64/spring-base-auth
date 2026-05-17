package com.vulinh.configuration;

import module java.sql;

import com.vulinh.configuration.ApplicationProperties.PeerDatabaseBootstrap.PeerDatabase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnBooleanProperty(
    prefix = "application-properties.peer-database-bootstrap",
    name = "enabled")
public class PeerDatabaseBootstrap {

  // Guard DDL injection since CREATE DATABASE / GRANT cannot use bind parameters.
  private static final String SAFE_IDENTIFIER_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";

  private final DataSource dataSource;
  private final ApplicationProperties applicationProperties;

  @PostConstruct
  void createPeerDatabases() throws SQLException {
    var databaseBootstrap = applicationProperties.peerDatabaseBootstrap();

    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(true);

      for (var peerDatabase : databaseBootstrap.peerDatabases()) {
        var databaseName = peerDatabase.databaseName();

        if (exists(connection, databaseName)) {
          log.info("Peer database '{}' already exists, skipping...", databaseName);
        } else {
          create(connection, databaseName);

          log.info("Created peer database '{}'", databaseName);
        }

        grant(connection, peerDatabase);
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
    checkIdentifier(databaseName);

    try (var statement = connection.createStatement()) {
      statement.execute(
          """
          CREATE DATABASE "%s"
          """
              .formatted(databaseName));
    }
  }

  private static void grant(Connection connection, PeerDatabase peerDatabase) throws SQLException {
    var user = peerDatabase.user();

    checkIdentifier(user);

    try (var statement = connection.createStatement()) {
      statement.execute(
          """
          GRANT ALL PRIVILEGES ON DATABASE "%s" TO "%s"
          """
              .formatted(peerDatabase.databaseName(), user));
    }
  }

  private static void checkIdentifier(String identifier) {
    if (StringUtils.isEmpty(identifier)
        || !StringUtils.containsOnly(identifier, SAFE_IDENTIFIER_CHARS)) {
      throw new IllegalArgumentException("Invalid identifier: " + identifier);
    }
  }
}
