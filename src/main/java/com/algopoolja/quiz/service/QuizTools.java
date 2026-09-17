package com.algopoolja.quiz.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class QuizTools {

    @Tool(description = "테스트케이스에서 마크다운 코드블록과 입력/출력 라벨을 제거합니다")
    public String cleanTestcaseText(String value) {
        if (value == null) return "";
        String text = value.replace("```text", "").replace("```python", "").replace("```", "");
        List<String> cleaned = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String current = line.strip();
            if (current.isEmpty()) continue;
            for (String label : List.of("입력:", "출력:", "입력", "출력")) {
                if (current.startsWith(label)) {
                    current = current.substring(label.length()).strip();
                    break;
                }
            }
            if (!current.isEmpty()) cleaned.add(current);
        }
        return String.join("\n", cleaned);
    }

    @Tool(description = "빈칸 수가 1~5개이고 정답 블록 수와 같은지 검증합니다")
    public String validateBlankCount(String blankCode, List<String> correctAnswers) {
        int count = countBlanks(blankCode);
        if (count == 0) return "ERROR: 빈칸이 없습니다.";
        if (count > 5) return "ERROR: 빈칸이 5개를 초과했습니다.";
        if (correctAnswers == null || count != correctAnswers.size()) {
            return "ERROR: 빈칸은 %d개인데 정답 블록은 %d개입니다."
                    .formatted(count, correctAnswers == null ? 0 : correctAnswers.size());
        }
        return "OK";
    }

    @Tool(description = "정답과 오답 선택지를 합쳐 중복 제거 후 무작위로 섞습니다")
    public List<String> buildChoiceBlocks(List<String> correctAnswers, List<String> wrongAnswers) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (correctAnswers != null) correctAnswers.stream().filter(this::hasText).forEach(unique::add);
        if (wrongAnswers != null) wrongAnswers.stream().filter(this::hasText).forEach(unique::add);
        List<String> result = new ArrayList<>(unique);
        Collections.shuffle(result);
        return result;
    }

    public int countBlanks(String code) {
        return code == null ? 0 : (code.length() - code.replace("<BLANK>", "").length()) / 7;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
