package com.vulinh.data.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vulinh.utils.CollectionHelper;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public record IntrospectResponse(boolean active, Map<String, Object> claims, String error) {

  public static IntrospectResponse valid(Map<String, Object> claims) {
    return new IntrospectResponse(true, claims, null);
  }

  public static IntrospectResponse invalid(String reason) {
    return new IntrospectResponse(false, null, reason);
  }

  public IntrospectResponse {
    claims = CollectionHelper.emptyMapIfNull(claims);
  }
}
