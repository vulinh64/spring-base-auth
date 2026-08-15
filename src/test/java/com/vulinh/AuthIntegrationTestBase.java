package com.vulinh;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "application-properties.peer-database-bootstrap.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(ContainersConfiguration.class)
public abstract class AuthIntegrationTestBase {

  @Autowired protected MockMvc mockMvc;
}
