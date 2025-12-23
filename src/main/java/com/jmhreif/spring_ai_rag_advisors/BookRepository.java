package com.jmhreif.spring_ai_rag_advisors;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface BookRepository extends Neo4jRepository<Book, String> {
    @Query("MATCH (b:Book)<-[rel:WRITTEN_FOR]-(r:Review) " +
            "WHERE r.id IN $documentIds OR b.id IN $documentIds " +
            "OPTIONAL MATCH (b)<-[rel2:AUTHORED]-(a:Author)" +
            "WITH b, substring(b.text,0,250)+'...' as text, COLLECT(r{.text, .rating}) as matchedReviews, COLLECT(a{.name}) as authors " +
            "RETURN b { .id, text, " +
            "   metadata: b { .title, .average_rating, .url, " +
            "       authors: authors, " +
            "       reviews: matchedReviews}};")
    List<Book> findBooks(List<String> documentIds);
}
