package com.jmhreif.spring_ai_rag_advisors;

import org.springframework.data.neo4j.core.schema.Id;

import java.util.Map;

public record Book(@Id String id,
                   String text,
                   Map<String, Object> metadata) {
}
