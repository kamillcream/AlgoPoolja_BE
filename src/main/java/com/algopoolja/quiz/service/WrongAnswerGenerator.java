package com.algopoolja.quiz.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Creates deterministic distractors from a correct answer block.
 * The semantic problem and the correct code remain AI responsibilities;
 * simple syntactic distractors are cheaper and more predictable locally.
 */
public class WrongAnswerGenerator {
    public List<String> generate(List<String> correctAnswers) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String answer : correctAnswers == null ? List.<String>of() : correctAnswers) {
            if (answer == null || answer.isBlank()) continue;
            addMutations(result, answer);
        }
        return new ArrayList<>(result);
    }

    private void addMutations(LinkedHashSet<String> result, String answer) {
        replaceIfDifferent(result, answer, "+", "-");
        replaceIfDifferent(result, answer, "-", "+");
        replaceIfDifferent(result, answer, "*", "/");
        replaceIfDifferent(result, answer, "/", "*");
        replaceIfDifferent(result, answer, "<=", "<");
        replaceIfDifferent(result, answer, ">=", ">");
        replaceIfDifferent(result, answer, "<", "<=");
        replaceIfDifferent(result, answer, ">", ">=");

        String trimmed = answer.trim();
        if (trimmed.matches("-?\\d+")) {
            try {
                long value = Long.parseLong(trimmed);
                result.add(answer.replace(trimmed, Long.toString(value + 1)));
                result.add(answer.replace(trimmed, Long.toString(value - 1)));
            } catch (NumberFormatException ignored) {
                // Keep the original answer when the numeric literal is outside long range.
            }
        }
        if (result.size() >= 8) return;
        result.add(answer + " + 1");
        result.add("(" + answer + ")");
    }

    private void replaceIfDifferent(LinkedHashSet<String> result, String source, String from, String to) {
        if (source.contains(from)) result.add(source.replaceFirst(java.util.regex.Pattern.quote(from), to));
    }
}
