package com.techfork.personalization.application.generation;

import com.techfork.global.llm.EmbeddingClient;
import com.techfork.personalization.application.activity.UserActivityCollector;
import com.techfork.personalization.application.activity.UserActivityData;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocument;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalizedProfileGenerator {

    private final UserActivityCollector userActivityCollector;
    private final PersonalizationProfileAnalyzer personalizationProfileAnalyzer;
    private final EmbeddingClient embeddingClient;
    private final PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    public PersonalizationProfileDocument generate(Long userId) {
        UserActivityData activityData = userActivityCollector.collect(userId);
        PersonalizationProfileAnalysis analysis = personalizationProfileAnalyzer.analyze(activityData);
        float[] profileVector = generateEmbeddingVector(analysis.profileText());

        PersonalizationProfileDocument profileDocument = PersonalizationProfileDocument.create(
                userId,
                analysis.profileText(),
                profileVector,
                activityData.interests(),
                analysis.keyKeywords()
        );

        return personalizationProfileDocumentRepository.save(profileDocument);
    }

    private float[] generateEmbeddingVector(String profileText) {
        List<Float> embedding = embeddingClient.embed(profileText);

        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i);
        }
        return vector;
    }
}
