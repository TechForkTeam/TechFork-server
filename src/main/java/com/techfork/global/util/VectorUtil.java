package com.techfork.global.util;

import java.util.List;

/**
 * 벡터 유틸리티
 * 임베딩 벡터 변환 및 유사도 계산 기능 제공
 */
public class VectorUtil {

    private VectorUtil() {
        // 유틸리티 클래스, 인스턴스화 방지
    }

    /**
     * List<Float>를 float[] 배열로 변환
     *
     * @param embedding Float 리스트
     * @return float 배열 (null이거나 비어있으면 null 반환)
     */
    public static float[] convertToFloatArray(List<Float> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return null;
        }

        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i);
        }
        return vector;
    }

    /**
     * 코사인 유사도 계산 (float[] vs float[])
     * 두 벡터 간의 코사인 각도를 기반으로 유사도 측정 (-1.0 ~ 1.0)
     *
     * @param vectorA 첫 번째 벡터
     * @param vectorB 두 번째 벡터
     * @return 코사인 유사도 (0.0 ~ 1.0, 벡터가 null이거나 길이가 다르면 0.0)
     */
    public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length || vectorA.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += (double) vectorA[i] * vectorB[i];
            normA += (double) vectorA[i] * vectorA[i];
            normB += (double) vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 코사인 유사도 계산 (float[] vs List<Float>)
     *
     * @param vector1 첫 번째 벡터 (float 배열)
     * @param vector2 두 번째 벡터 (Float 리스트)
     * @return 코사인 유사도 (0.0 ~ 1.0)
     */
    public static double cosineSimilarity(float[] vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.length == 0 || vector2.isEmpty()) {
            return 0.0;
        }

        if (vector1.length != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2.get(i);
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2.get(i) * vector2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 코사인 유사도 계산 (List<Float> vs List<Float>)
     *
     * @param vector1 첫 번째 벡터 (Float 리스트)
     * @param vector2 두 번째 벡터 (Float 리스트)
     * @return 코사인 유사도 (0.0 ~ 1.0)
     */
    public static double cosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        if (vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
