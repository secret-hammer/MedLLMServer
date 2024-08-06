package com.vipa.medllm.exception;

import lombok.Getter;

@Getter
public enum CustomError {
    INVALID_AUTHENTICATION_ERROR(700, "Invalid Authorization header"),
    USERNAME_NOT_FOUND_ERROR(701, "User not exists by Username or Email"),
    USERNAME_ALREADY_EXISTS(702, "Username already exists"),
    EMAIL_ALREADY_EXISTS(703, "Email already exists"),
    INVALID_JWT_TOKEN(704, "Invalid JWT token"),
    JWT_TOKEN_EXPIRED(705, "JWT token is expired"),
    UNSUPPORTED_JWT_TOKEN(706, "Unsupported JWT token"),
    EMPTY_JWT_CLAIM(707, "Empty JWT claim"),
    UNAUTHORIZED_REQUEST(708, "Unauthorized request"),
    IMAGE_NOT_FOUND(709, "Image not found"),
    INVALID_IMAGE_URL(709, "Invalid image url"),


    PROJECT_NOT_FOUND(710, "Project not found"),
    GROUP_NOT_FOUND(711, "Group not found"),
    INVALID_GROUP_DATA(712, "Invalid group data"),

    ARGUMENT_NOT_VALID(801, "Argument not valid"),
    INTERNAL_SERVER_ERROR(800, "Internal server error");

    private final String message;
    private final Integer code;

    CustomError(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
