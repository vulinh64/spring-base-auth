package com.vulinh.data.dto;

import com.vulinh.utils.CollectionHelper;
import java.util.Map;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record IntrospectResponse(boolean active, Map<String, Object> claims, String error) {

  public static IntrospectResponse valid(Map<String, Object> claims) {
    return new IntrospectResponse(true, claims, null);
  }

  public static IntrospectResponse invalid(String reason) {
    return IntrospectResponse.builder().error(reason).build();
  }

  public IntrospectResponse {
    claims = CollectionHelper.emptyMapIfNull(claims);
  }
}
