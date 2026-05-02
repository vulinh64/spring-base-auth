package com.vulinh.configuration;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
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

      var appUser = connection.getMetaData().getUserName();

      if (StringUtils.isEmpty(appUser)
          || !StringUtils.containsOnly(appUser, SAFE_IDENTIFIER_CHARS)) {
        throw new IllegalStateException("Unsafe datasource username: " + appUser);
      }

      for (var database : databaseBootstrap.peerDatabases()) {
        if (exists(connection, database)) {
          log.info("Peer database '{}' already exists, skipping...", database);
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
    if (StringUtils.isEmpty(databaseName)
        || !StringUtils.containsOnly(databaseName, SAFE_IDENTIFIER_CHARS)) {
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
