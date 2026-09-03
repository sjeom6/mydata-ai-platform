package com.sjeom.mydata.platform.support.api;

public record ApiErrorResponse(
        String status,
        String code,
        String message
) {
}
