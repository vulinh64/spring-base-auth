package com.vulinh.configuration;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
  @ConfigurationProperties("spring.datasource")
  DataSourceProperties authDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  DataSource authDataSource(DataSourceProperties authDataSourceProperties) {
    return authDataSourceProperties.initializeDataSourceBuilder().build();
  }

  @Bean
  @DependsOn("peerDatabaseBootstrap")
  DataSource springBaseDataSource(DataSourceProperties authDataSourceProperties) {
    return authDataSourceProperties
        .initializeDataSourceBuilder()
        .url(swapDatabase(authDataSourceProperties.getUrl(), "spring-base"))
        .build();
  }

  @Bean
  @DependsOn("peerDatabaseBootstrap")
  DataSource springBaseEventDataSource(DataSourceProperties authDataSourceProperties) {
    return authDataSourceProperties
        .initializeDataSourceBuilder()
        .url(swapDatabase(authDataSourceProperties.getUrl(), "spring-base-event"))
        .build();
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
    liquibase.setChangeLog(CHANGELOG_ROOT + service + "/db.changelog.yaml");
    return liquibase;
  }

  // Swap the database-name segment of a JDBC URL while preserving any query string.
  // Example: jdbc:postgresql://host:5432/spring-base-auth?sslmode=require
  //       -> jdbc:postgresql://host:5432/spring-base?sslmode=require
  private static String swapDatabase(String jdbcUrl, String databaseName) {
    int queryStart = jdbcUrl.indexOf('?');
    String base = queryStart == -1 ? jdbcUrl : jdbcUrl.substring(0, queryStart);
    String query = queryStart == -1 ? "" : jdbcUrl.substring(queryStart);
    int lastSlash = base.lastIndexOf('/');
    return base.substring(0, lastSlash + 1) + databaseName + query;
  }
}
