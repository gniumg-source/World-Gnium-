package com.jmhreif.spring_ai_rag_advisors;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GraphQueryAugmenter implements QueryAugmenter {

    private final BookRepository bookRepository;

    public GraphQueryAugmenter(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public Query augment(Query query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return query;
        }

        // Extract document IDs for graph lookup
        List<String> docIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        // Get books from graph
        List<Book> books = bookRepository.findBooks(docIds);

        if (books.isEmpty()) {
            return query;
        }

        // Augment query with book context
        String bookContext = books.stream()
                .map(Book::toString)
                .collect(Collectors.joining("\n"));

        String augmentedText = query.text() + "\n\nAdditional book context:\n" + bookContext;
        return new Query(augmentedText);
    }
}