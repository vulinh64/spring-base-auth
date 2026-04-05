package com.vulinh.data.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
  BEARER("Bearer"),
  REFRESH("Refresh");

  private final String typeName;
}
