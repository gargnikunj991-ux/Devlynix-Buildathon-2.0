package com.devtinder.security;

import java.util.regex.Pattern;

public final class SanitizationUtil {

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private SanitizationUtil() {
    }

    public static String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        String stripped = SCRIPT_TAG_PATTERN.matcher(input).replaceAll("");
        stripped = HTML_TAG_PATTERN.matcher(stripped).replaceAll("");
        return stripped.trim();
    }
}
