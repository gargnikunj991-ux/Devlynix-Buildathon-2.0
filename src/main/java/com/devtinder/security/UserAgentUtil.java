package com.devtinder.security;

import jakarta.servlet.http.HttpServletRequest;

public final class UserAgentUtil {

    private UserAgentUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    public static String extractDeviceInfo(HttpServletRequest request) {
        if (request == null) {
            return "Unknown Device";
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "Web Client";
        }

        String os = "Unknown OS";
        if (userAgent.contains("Windows NT 10.0")) {
            os = "Windows 10/11";
        } else if (userAgent.contains("Windows")) {
            os = "Windows";
        } else if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS X")) {
            os = "macOS";
        } else if (userAgent.contains("iPhone")) {
            os = "iOS (iPhone)";
        } else if (userAgent.contains("iPad")) {
            os = "iOS (iPad)";
        } else if (userAgent.contains("Android")) {
            os = "Android";
        } else if (userAgent.contains("Linux")) {
            os = "Linux";
        }

        String browser = "Browser";
        if (userAgent.contains("Edg/")) {
            browser = "Edge";
        } else if (userAgent.contains("Chrome/") && !userAgent.contains("Edg/")) {
            browser = "Chrome";
        } else if (userAgent.contains("Firefox/")) {
            browser = "Firefox";
        } else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) {
            browser = "Safari";
        } else if (userAgent.contains("PostmanRuntime")) {
            browser = "Postman";
        } else if (userAgent.contains("curl")) {
            browser = "cURL";
        }

        return browser + " on " + os;
    }
}
