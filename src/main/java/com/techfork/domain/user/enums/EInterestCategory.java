package com.techfork.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EInterestCategory {

    IOS("iOS"),
    ANDROID("Android"),

    FRONTEND("Frontend"),

    BACKEND("Backend"),

    DATA_ENGINEERING("Data Engineering"),
    DATA_SCIENCE("Data Science"),
    DATABASE("Database"),

    AI_ML("AI/ML"),

    DEVOPS("DevOps"),
    CLOUD("Cloud"),
    SYSTEMS_OS("Systems/OS"),
    NETWORKING("Networking"),

    SECURITY("Security"),

    GAME_DEV("Game Dev"),
    AR_VR_XR("AR/VR/XR"),

    EMBEDDED_IOT("Embedded/IoT"),

    BLOCKCHAIN_WEB3("Blockchain/Web3"),

    QA_TEST("QA/Test"),

    PRODUCT_UX("Product/UX"),

    ARCHITECTURE("Architecture");

    private final String displayName;
}
