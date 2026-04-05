package com.vulinh.configuration;

import org.hsqldb.server.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HsqldbServerConfig {

  @Bean(initMethod = "start", destroyMethod = "stop")
  Server hsqldbServer() {
    var server = new Server();
    server.setDatabaseName(0, "spring-base-auth");
    server.setDatabasePath(
        0, "file:" + System.getProperty("user.home") + "/.hsqldb/spring-base-auth/db");
    server.setPort(9001);
    server.setSilent(true);
    return server;
  }
}
