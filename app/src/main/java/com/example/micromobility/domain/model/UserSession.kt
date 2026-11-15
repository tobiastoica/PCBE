package com.example.micromobility.domain.model

data class UserSession (
    val userId: String,
    val fullName: String,
    val email: String,
    val accessToken: String
)