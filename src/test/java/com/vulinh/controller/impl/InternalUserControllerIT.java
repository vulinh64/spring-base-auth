package com.vulinh.controller.impl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vulinh.AuthIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

class InternalUserControllerIT extends AuthIntegrationTestBase {

  private static final String ACCOUNT_ID = "3cb0f25e-37d0-4cc8-8e79-02cb285cc0c1";

  @Test
  @Sql(
      statements =
          "INSERT INTO account (id, username, email, first_name, last_name, is_enabled,created_by, updated_by, created_date_time, updated_date_time) VALUES ("
              + "'"
              + ACCOUNT_ID
              + "'"
              + ",'internal-api-user', 'internal-api-user@example.test', 'Internal', 'Api User', TRUE, 'integration-test', 'integration-test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);",
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(
      statements = "DELETE FROM account WHERE id = '" + ACCOUNT_ID + "';",
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  void getUserReturnsSeededAccountForLocalInterServiceCall() throws Exception {
    mockMvc
        .perform(get("/internal/users/{id}", ACCOUNT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ACCOUNT_ID))
        .andExpect(jsonPath("$.username").value("internal-api-user"))
        .andExpect(jsonPath("$.email").value("internal-api-user@example.test"))
        .andExpect(jsonPath("$.firstName").value("Internal"))
        .andExpect(jsonPath("$.lastName").value("Api User"))
        .andExpect(jsonPath("$.isEnabled").value(true));
  }
}
