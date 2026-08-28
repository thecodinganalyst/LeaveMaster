package com.practical.leavemaster.assistant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AssistantResponseFormatter {

    private static final Pattern TEXT_COMMAND = Pattern.compile("\\\\text\\{([^{}]*)}");
    private static final Pattern FRACTION_COMMAND = Pattern.compile("\\\\frac\\{([^{}]*)}\\{([^{}]*)}");

    private AssistantResponseFormatter() {
    }

    static String toDisplayMarkdown(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String formatted = content
                .replace("\\[", "")
                .replace("\\]", "")
                .replace("\\(", "")
                .replace("\\)", "")
                .replace("$$", "")
                .replace("\\left", "")
                .replace("\\right", "")
                .replace("\\times", "×")
                .replace("\\div", "÷")
                .replace("\\cdot", "×");

        formatted = replaceAll(formatted, TEXT_COMMAND, "$1");
        formatted = replaceAll(formatted, FRACTION_COMMAND, "$1 / $2");
        return formatted;
    }

    private static String replaceAll(String value, Pattern pattern, String replacement) {
        String current = value;
        while (pattern.matcher(current).find()) {
            Matcher matcher = pattern.matcher(current);
            current = matcher.replaceAll(replacement);
        }
        return current;
    }
}
