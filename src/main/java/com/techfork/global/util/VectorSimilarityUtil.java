package com.techfork.global.util;

/**
 * 벡터 유사도 계산 유틸리티
 * 임베딩 벡터 간의 유사도를 측정하는 다양한 메트릭 제공
 */
public class VectorSimilarityUtil {

    private VectorSimilarityUtil() {
        // 유틸리티 클래스, 인스턴스화 방지
    }

    /**
     * 코사인 유사도 계산
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
     * 유클리드 거리 계산
     * 두 벡터 간의 직선 거리 측정 (값이 작을수록 유사)
     *
     * @param vectorA 첫 번째 벡터
     * @param vectorB 두 번째 벡터
     * @return 유클리드 거리 (0 이상, 벡터가 null이거나 길이가 다르면 Double.MAX_VALUE)
     */
    public static double euclideanDistance(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length || vectorA.length == 0) {
            return Double.MAX_VALUE;
        }

        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            double diff = vectorA[i] - vectorB[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * 내적(Dot Product) 계산
     * 두 벡터의 내적 값 (값이 클수록 유사)
     *
     * @param vectorA 첫 번째 벡터
     * @param vectorB 두 번째 벡터
     * @return 내적 값 (벡터가 null이거나 길이가 다르면 0.0)
     */
    public static double dotProduct(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length || vectorA.length == 0) {
            return 0.0;
        }

        double product = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            product += (double) vectorA[i] * vectorB[i];
        }

        return product;
    }
}
