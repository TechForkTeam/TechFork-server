package com.techfork.domain.search.service;

public class SearchConstants {
    static final String POSTS_INDEX = "posts";
    static final String TITLE_FIELD_FORMAT = "title^%.1f";
    static final String SUMMARY_FIELD_FORMAT = "summary^%.1f";
    static final String CONTENT_CHUNKS_PATH = "contentChunks";
    static final String CHUNK_TEXT_FIELD = "contentChunks.chunkText";
    static final String MINIMUM_SHOULD_MATCH = "0";
    static final String SCRIPT_SOURCE_SEMANTIC = "cosineSimilarity(params.query_vector, 'summaryEmbedding') + 1.0";
    static final String QUERY_VECTOR_PARAM = "query_vector";
}
