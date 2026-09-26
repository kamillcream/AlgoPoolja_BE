package com.algopoolja.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuizToolsTest {
    private final QuizTools tools = new QuizTools();

    @Test
    void cleansMarkdownAndLabels() {
        assertThat(tools.cleanTestcaseText("```text\n입력: 3 4\n5\n```"))
                .isEqualTo("3 4\n5");
    }

    @Test
    void validatesBlankAndAnswerCounts() {
        assertThat(tools.validateBlankCount("return <BLANK> + <BLANK> + <BLANK>", List.of("a", "b", "0"))).isEqualTo("OK");
        assertThat(tools.validateBlankCount("return <BLANK>", List.of("a", "b"))).startsWith("ERROR:");
    }

    @Test
    void choicesAreUnique() {
        assertThat(tools.buildChoiceBlocks(List.of("a", "b"), List.of("b", "c")))
                .containsExactlyInAnyOrder("a", "b", "c");
    }
}
