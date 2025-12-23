package com.jmhreif.spring_ai_rag_advisors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class RAGController {
    private final ChatClient chatClient;
    private final Neo4jVectorStore vectorStoreReviews;
    private final Neo4jVectorStore vectorStoreBooks;
    private final GraphRetrievalAdvisor graphRetrievalAdvisor;
    private final GraphQueryAugmenter graphQueryAugmenter;

    String prompt = """
            You are a book expert providing recommendations based on the book reviews provided in the context.
            Please summarize the books provided.
            """;

    public RAGController(ChatClient.Builder builder, @Qualifier("neo4jReviews") Neo4jVectorStore vectorStoreReviews, @Qualifier("neo4jBooks") Neo4jVectorStore vectorStoreBooks, GraphRetrievalAdvisor graphRetrievalAdvisor, GraphQueryAugmenter graphQueryAugmenter) {
        this.chatClient = builder.build();
        this.vectorStoreReviews = vectorStoreReviews;
        this.vectorStoreBooks = vectorStoreBooks;
        this.graphRetrievalAdvisor = graphRetrievalAdvisor;
        this.graphQueryAugmenter = graphQueryAugmenter;
    }

    @GetMapping("/qa")
    public String getQAAdvised(@RequestParam String query) {
        return chatClient.prompt()
                .system(prompt)
                .user(query)
                .advisors(new SimpleLoggerAdvisor(),
                        new QuestionAnswerAdvisor(vectorStoreReviews),
                        graphRetrievalAdvisor)
                .call().content();
    }

    @GetMapping("/retrievalAdvisor")
    public String getRAGAdvised(@RequestParam String query) {
        Advisor retrievalAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStoreReviews)
                        .build())
                .build();

        return chatClient.prompt()
                .system(prompt)
                .user(query)
                .advisors(new SimpleLoggerAdvisor(),
                        retrievalAdvisor,
                        graphRetrievalAdvisor)
                .call()
                .content();
    }

    @GetMapping("/preTransform")
    public String getPreTransformAdvised(@RequestParam String query) {
        Advisor ragPrefilterAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(chatClient.mutate().defaultOptions(ChatOptions.builder().temperature(0.0).build()).build().mutate())
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStoreReviews)
                        .build())
                .build();

        return chatClient.prompt()
                .system(prompt)
                .advisors(new SimpleLoggerAdvisor(),
                        ragPrefilterAdvisor,
                        graphRetrievalAdvisor)
                .user(query)
                .call()
                .content();
    }

    @GetMapping("/retrieveFilter")
    public String getRetrieveFilterAdvised(@RequestParam String query) {
        Advisor ragRetrieveFilterAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStoreReviews)
                        .filterExpression(new FilterExpressionBuilder()
                                .gt("rating", 3)
                                .build())
                        .build())
                .build();

        return chatClient.prompt()
                .system(prompt)
                .advisors(new SimpleLoggerAdvisor(),
                        ragRetrieveFilterAdvisor,
                        graphRetrievalAdvisor)
                .user(query)
                .call()
                .content();
    }

    @GetMapping("/retrieveJoin")
    public String getRetrievalJoinAdvised(@RequestParam String query) {
        Advisor ragRetrieveJoinAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStoreReviews)
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStoreBooks)
                        .build())
                .documentJoiner(new ConcatenationDocumentJoiner())
                .build();

        return chatClient.prompt()
                .system(prompt)
                .advisors(new SimpleLoggerAdvisor(),
                        ragRetrieveJoinAdvisor,
                        graphRetrievalAdvisor)
                .user(query)
                .call()
                .content();
    }

    @GetMapping("/retrieveAugment")
    public String getRetrievalAugmentAdvised(@RequestParam String query) {
        Advisor retrieveAugmentAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStoreReviews)
                        .build())
                .queryAugmenter(graphQueryAugmenter)
                .build();

        return chatClient.prompt()
                .system(prompt)
                .advisors(new SimpleLoggerAdvisor(),
                        retrieveAugmentAdvisor)
                .user(query)
                .call()
                .content();
    }
}
