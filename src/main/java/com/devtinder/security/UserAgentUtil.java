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
            return "Web Browser";
        }

        // 1. Identify Operating System & Hardware Device
        String os = "Desktop PC";
        if (userAgent.contains("iPhone")) {
            os = "Apple iPhone";
        } else if (userAgent.contains("iPad")) {
            os = "Apple iPad";
        } else if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS X")) {
            os = "Apple Mac (macOS)";
        } else if (userAgent.contains("Windows NT 10.0") || userAgent.contains("Windows NT 11.0")) {
            os = "Windows 11 / 10 PC";
        } else if (userAgent.contains("Windows NT 6.3")) {
            os = "Windows 8.1 PC";
        } else if (userAgent.contains("Windows NT 6.1")) {
            os = "Windows 7 PC";
        } else if (userAgent.contains("Windows")) {
            os = "Windows PC";
        } else if (userAgent.contains("Android")) {
            if (userAgent.contains("SM-") || userAgent.contains("Samsung")) {
                os = "Samsung Galaxy (Android)";
            } else if (userAgent.contains("Pixel")) {
                os = "Google Pixel (Android)";
            } else if (userAgent.contains("OnePlus")) {
                os = "OnePlus (Android)";
            } else if (userAgent.contains("Xiaomi") || userAgent.contains("Redmi") || userAgent.contains("POCO")) {
                os = "Xiaomi / Redmi (Android)";
            } else if (userAgent.contains("Tablet")) {
                os = "Android Tablet";
            } else {
                os = "Android Phone";
            }
        } else if (userAgent.contains("CrOS")) {
            os = "Chromebook (ChromeOS)";
        } else if (userAgent.contains("Ubuntu")) {
            os = "Ubuntu Linux";
        } else if (userAgent.contains("Fedora")) {
            os = "Fedora Linux";
        } else if (userAgent.contains("Debian")) {
            os = "Debian Linux";
        } else if (userAgent.contains("Linux")) {
            os = "Linux PC";
        } else if (userAgent.contains("Mobile")) {
            os = "Mobile Device";
        }

        // 2. Identify Web Browser / Client
        String browser = "Web Browser";
        if (userAgent.contains("PostmanRuntime")) {
            return "Postman API Client (" + os + ")";
        } else if (userAgent.startsWith("curl/")) {
            return "cURL Developer Tool (" + os + ")";
        } else if (userAgent.contains("Insomnia")) {
            return "Insomnia REST Client (" + os + ")";
        } else if (userAgent.contains("Arc/")) {
            browser = "Arc Browser";
        } else if (userAgent.contains("Brave/")) {
            browser = "Brave Browser";
        } else if (userAgent.contains("OPR/") || userAgent.contains("Opera/")) {
            browser = "Opera";
        } else if (userAgent.contains("Edg/") || userAgent.contains("Edge/") || userAgent.contains("EdgiOS/") || userAgent.contains("EdgA/")) {
            browser = "Microsoft Edge";
        } else if (userAgent.contains("SamsungBrowser/")) {
            browser = "Samsung Internet";
        } else if (userAgent.contains("Vivaldi/")) {
            browser = "Vivaldi";
        } else if (userAgent.contains("DuckDuckGo/")) {
            browser = "DuckDuckGo Browser";
        } else if (userAgent.contains("Firefox/") || userAgent.contains("FxiOS/")) {
            browser = "Mozilla Firefox";
        } else if (userAgent.contains("CriOS/")) {
            browser = "Google Chrome (iOS)";
        } else if (userAgent.contains("Chrome/")) {
            browser = "Google Chrome";
        } else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) {
            browser = "Apple Safari";
        }

        return browser + " on " + os;
    }
}
