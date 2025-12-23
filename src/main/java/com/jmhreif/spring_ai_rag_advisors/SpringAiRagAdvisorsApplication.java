package com.jmhreif.spring_ai_rag_advisors;

import org.neo4j.driver.Driver;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringAiRagAdvisorsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiRagAdvisorsApplication.class, args);
	}

	@Bean("neo4jReviews")
	public Neo4jVectorStore neo4jVectorStoreReview(Driver driver, EmbeddingModel embeddingModel) {
		return Neo4jVectorStore.builder(driver, embeddingModel)
				.indexName("review-text")
				.label("Review")
				.initializeSchema(false)
				.build();
	}

	@Bean("neo4jBooks")
	public Neo4jVectorStore neo4jVectorStoreBook(Driver driver, EmbeddingModel embeddingModel) {
		return Neo4jVectorStore.builder(driver, embeddingModel)
				.indexName("book-descriptions")
				.label("Book")
				.initializeSchema(false)
				.build();
	}
}
