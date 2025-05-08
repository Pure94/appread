package pureapps.appread.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ConversationMemoryService {

    private final Map<String, List<ConversationMessage>> conversations = new HashMap<>();

    private static final int MAX_HISTORY_LENGTH = 10;

    public void addMessage(String conversationId, String role, String content) {
        log.debug("Adding message to conversation {}: {} - {}", conversationId, role, content);
        List<ConversationMessage> history = conversations.computeIfAbsent(
                conversationId, k -> new ArrayList<>());
        history.add(new ConversationMessage(role, content));
        if (history.size() > MAX_HISTORY_LENGTH) {
            history.remove(0);
        }
    }

    public List<ConversationMessage> getConversationHistory(String conversationId) {
        return conversations.getOrDefault(conversationId, new ArrayList<>());
    }

    public List<String> getConversationHistoryAsStrings(String conversationId) {
        List<ConversationMessage> history = getConversationHistory(conversationId);
        List<String> result = new ArrayList<>();

        for (ConversationMessage message : history) {
            result.add(message.getRole() + ": " + message.getContent());
        }

        return result;
    }

    public void clearConversationHistory(String conversationId) {
        log.debug("Clearing conversation history for {}", conversationId);
        conversations.remove(conversationId);
    }

    public static class ConversationMessage {
        private final String role;
        private final String content;

        public ConversationMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}