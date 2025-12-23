package com.jmhreif.spring_ai_rag_advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GraphRetrievalAdvisor implements CallAdvisor {
    private final BookRepository repo;

    public GraphRetrievalAdvisor(BookRepository repo) {
        this.repo = repo;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // Get documents from context and extract IDs in one step
        var documents = findDocumentsInContext(chatClientRequest.context());

        if (documents == null || documents.isEmpty()) {
            System.out.println("No documents found in context, skipping graph retrieval");
            return callAdvisorChain.nextCall(chatClientRequest);
        }

        // Run graph retrieval query directly with document IDs
        List<Book> bookList = repo.findBooks(
            documents.stream().map(Document::getId).collect(Collectors.toList())
        );

        // Create system message with book context
        String bookContext = bookList.stream()
                .map(Book::toString)
                .collect(Collectors.joining("\n"));
        SystemMessage systemMessage = new SystemMessage("Use these books for context: " + bookContext);

        // Create new prompt with system message and original user message
        var updatedPrompt = new Prompt(List.of(chatClientRequest.prompt().getUserMessage(), systemMessage));

        var updatedRequest = ChatClientRequest.builder()
                .prompt(updatedPrompt)
                .context(Map.of()) //Clear the context
                .build();
        System.out.println("----- Updated request -----");
        System.out.println(updatedRequest);

        return callAdvisorChain.nextCall(updatedRequest);

    }

    private List<Document> findDocumentsInContext(Map<String, Object> context) {
        var documents = (List<Document>) context.get("qa_retrieved_documents");
        if (documents == null || documents.isEmpty()) {
            documents = (List<Document>) context.get("rag_document_context");
        }
        return documents;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
