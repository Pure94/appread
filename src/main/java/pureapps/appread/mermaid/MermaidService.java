package pureapps.appread.mermaid;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class MermaidService {

    private final ChatClient chatClient;

    public MermaidService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generateMermaidSyntax(String description) {
        String prompt = "Na podstawie poniższego opisu wygeneruj tylko i wyłącznie kod diagramu Mermaid, bez żadnych dodatkowych komentarzy:\n" + description;
        return chatClient.prompt(new Prompt(prompt)).call().content();
    }
}