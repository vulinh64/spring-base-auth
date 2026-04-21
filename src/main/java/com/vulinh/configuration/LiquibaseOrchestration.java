package com.vulinh.configuration;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

// Runs Liquibase against three databases in a fixed order:
//   peerDatabaseBootstrap (CREATE DATABASE for peers)
//     -> authLiquibase        (spring-base-auth schema)
//       -> springBaseLiquibase (spring-base schema)
//         -> springBaseEventLiquibase (spring-base-event schema)
// Spring Boot's Liquibase auto-config is disabled (spring.liquibase.enabled=false)
// this class owns the full lifecycle instead.
@Configuration
public class LiquibaseOrchestration {

  private static final String CHANGELOG_ROOT = "classpath:db/changelog/";

  @Bean
  @Primary
  DataSource authDataSource(DataSourceProperties authDataSourceProperties) {
    return authDataSourceProperties.initializeDataSourceBuilder().build();
  }

  @Bean
  @DependsOn("peerDatabaseBootstrap")
  DataSource springBaseDataSource(DataSourceProperties authDataSourceProperties) {
    return initializeDataSource(authDataSourceProperties, "spring-base");
  }

  @Bean
  @DependsOn("peerDatabaseBootstrap")
  DataSource springBaseEventDataSource(DataSourceProperties authDataSourceProperties) {
    return initializeDataSource(authDataSourceProperties, "spring-base-event");
  }

  @Bean
  @DependsOn("peerDatabaseBootstrap")
  SpringLiquibase authLiquibase(@Qualifier("authDataSource") DataSource dataSource) {
    return liquibase(dataSource, "spring-base-auth");
  }

  @Bean
  @DependsOn("authLiquibase")
  SpringLiquibase springBaseLiquibase(@Qualifier("springBaseDataSource") DataSource dataSource) {
    return liquibase(dataSource, "spring-base");
  }

  @Bean
  @DependsOn("springBaseLiquibase")
  SpringLiquibase springBaseEventLiquibase(
      @Qualifier("springBaseEventDataSource") DataSource dataSource) {
    return liquibase(dataSource, "spring-base-event");
  }

  private static SpringLiquibase liquibase(DataSource dataSource, String service) {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("%s%s/db.changelog.yaml".formatted(CHANGELOG_ROOT, service));
    return liquibase;
  }

  private static DataSource initializeDataSource(
      DataSourceProperties authDataSourceProperties, String databaseName) {
    return authDataSourceProperties
        .initializeDataSourceBuilder()
        .url(interpolateDatabaseName(authDataSourceProperties.getUrl(), databaseName))
        .build();
  }

  // Swap the database-name segment of a JDBC URL while preserving any query string.
  private static String interpolateDatabaseName(String jdbcUrl, String databaseName) {
    int queryStart = jdbcUrl.indexOf('?');

    var base = queryStart == -1 ? jdbcUrl : jdbcUrl.substring(0, queryStart);

    return "%s%s%s"
        .formatted(
            base.substring(0, base.lastIndexOf('/') + 1),
            databaseName,
            queryStart == -1 ? StringUtils.EMPTY : jdbcUrl.substring(queryStart));
  }
}
