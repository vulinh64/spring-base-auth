package com.vulinh.data.dto;

import java.util.UUID;

public record AccountBasicResponse(UUID id, String username, String firstName, String lastName, String email) {}
