package com.ust.security.dto;

import java.util.Set;

public record RegisterRequest(
        String UserName,
        String email,
        String password,
        String location,
        String designation,
        String role,
        Set<String> skills,
        Set<String> roles
) {

}