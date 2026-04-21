package com.vulinh.data.dto;

import module java.base;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record RoleResponse(List<String> roles) {}
