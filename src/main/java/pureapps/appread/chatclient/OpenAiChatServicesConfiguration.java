package pureapps.appread.chatclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenAiChatServicesConfiguration {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Bean
    ChatClient openAiClient() {
        return ChatClient.builder(
                        openAiChatModel())
                .build();
    }

    @Bean
    OpenAiChatModel openAiChatModel() {
        var options = OpenAiChatOptions.builder()
                .streamUsage(true)
                .model(model)
                .build();
        return new OpenAiChatModel(openAiApi(), options);
    }

    @Bean
    OpenAiApi openAiApi() {
        return new OpenAiApi(apiKey);
    }
}
