package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantResponseFormatterTest {

    @Test
    void shouldConvertLeaveProrationLatexToPlainMarkdown() {
        String latex = "$$\\text{Annual Leave Entitlement} = 14 \\text{ days} \\times \\frac{151}{365} = 5.79 \\text{ days}$$";

        String formatted = AssistantResponseFormatter.toDisplayMarkdown(latex);

        assertThat(formatted)
                .isEqualTo("Annual Leave Entitlement = 14 days × 151 / 365 = 5.79 days")
                .doesNotContain("$$", "\\text", "\\frac", "\\times");
    }

    @Test
    void shouldPreserveExistingMarkdownFormatting() {
        String markdown = "**Proration calculation:**\n14 days × 151 ÷ 365 = 5.79 days";

        assertThat(AssistantResponseFormatter.toDisplayMarkdown(markdown)).isEqualTo(markdown);
    }

    @Test
    void shouldNormalizeCommonInlineAndDisplayMathDelimiters() {
        assertThat(AssistantResponseFormatter.toDisplayMarkdown("\\(14 \\times 151 \\div 365\\)"))
                .isEqualTo("14 × 151 ÷ 365");
        assertThat(AssistantResponseFormatter.toDisplayMarkdown("\\[14 \\cdot 151 / 365\\]"))
                .isEqualTo("14 × 151 / 365");
    }

    @Test
    void chatResponseShouldApplyDisplayFormattingBeforeReturningToClient() {
        var response = new AssistantDtos.ChatResponse(
                "conversation-1",
                "**Calculation:** $$14 \\times \\frac{151}{365} = 5.79$$",
                List.of(),
                List.of());

        assertThat(response.message()).isEqualTo("**Calculation:** 14 × 151 / 365 = 5.79");
    }

    @Test
    void shouldLeaveNullAndBlankMessagesUnchanged() {
        assertThat(AssistantResponseFormatter.toDisplayMarkdown(null)).isNull();
        assertThat(AssistantResponseFormatter.toDisplayMarkdown("   ")).isEqualTo("   ");
    }
}
