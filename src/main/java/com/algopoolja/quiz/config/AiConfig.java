package com.algopoolja.quiz.config;

import com.algopoolja.quiz.service.QuizTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatClient quizChatClient(ChatClient.Builder builder, QuizTools tools) {
        return builder
                .defaultSystem("""
                        당신은 알고리즘 빈칸 학습 서비스의 검증 Agent입니다.
                        문제와 코드는 요청한 난이도 및 알고리즘 유형을 정확히 따라야 합니다.
                        필요한 경우 등록된 도구로 문자열을 정리하고 결과를 검증하세요.
                        """)
                .defaultTools(tools)
                .build();
    }
}
