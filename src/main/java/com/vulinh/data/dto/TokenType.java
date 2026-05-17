package com.vulinh.data.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
  ACCESS("access"),
  REFRESH("refresh");

  @JsonValue private final String typeName;
}