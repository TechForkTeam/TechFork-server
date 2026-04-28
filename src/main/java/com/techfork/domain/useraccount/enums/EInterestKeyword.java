package com.techfork.domain.useraccount.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum EInterestKeyword {

    // iOS
    SWIFT(EInterestCategory.IOS, "Swift"),
    SWIFTUI(EInterestCategory.IOS, "SwiftUI"),
    UIKIT(EInterestCategory.IOS, "UIKit"),
    XCODE(EInterestCategory.IOS, "Xcode"),

    // Android
    KOTLIN(EInterestCategory.ANDROID, "Kotlin"),
    ANDROID_JAVA(EInterestCategory.ANDROID, "Java"),
    JETPACK_COMPOSE(EInterestCategory.ANDROID, "Jetpack Compose"),
    ANDROID_STUDIO(EInterestCategory.ANDROID, "Android Studio"),

    // Frontend
    REACT(EInterestCategory.FRONTEND, "React"),
    VUE_JS(EInterestCategory.FRONTEND, "Vue.js"),
    ANGULAR(EInterestCategory.FRONTEND, "Angular"),
    JAVASCRIPT(EInterestCategory.FRONTEND, "JavaScript"),
    TYPESCRIPT(EInterestCategory.FRONTEND, "TypeScript"),

    // Backend
    JAVA(EInterestCategory.BACKEND, "Java"),
    SPRING(EInterestCategory.BACKEND, "Spring"),
    NODE_JS(EInterestCategory.BACKEND, "Node.js"),
    PYTHON(EInterestCategory.BACKEND, "Python"),
    DJANGO(EInterestCategory.BACKEND, "Django"),

    // Data Engineering
    APACHE_SPARK(EInterestCategory.DATA_ENGINEERING, "Apache Spark"),
    APACHE_KAFKA(EInterestCategory.DATA_ENGINEERING, "Apache Kafka"),
    AIRFLOW(EInterestCategory.DATA_ENGINEERING, "Airflow"),
    ETL(EInterestCategory.DATA_ENGINEERING, "ETL"),

    // Data Science
    DS_PYTHON(EInterestCategory.DATA_SCIENCE, "Python"),
    PANDAS(EInterestCategory.DATA_SCIENCE, "Pandas"),
    NUMPY(EInterestCategory.DATA_SCIENCE, "NumPy"),
    JUPYTER(EInterestCategory.DATA_SCIENCE, "Jupyter"),
    SQL(EInterestCategory.DATA_SCIENCE, "SQL"),

    // Database
    MYSQL(EInterestCategory.DATABASE, "MySQL"),
    POSTGRESQL(EInterestCategory.DATABASE, "PostgreSQL"),
    MONGODB(EInterestCategory.DATABASE, "MongoDB"),
    REDIS(EInterestCategory.DATABASE, "Redis"),
    ORACLE(EInterestCategory.DATABASE, "Oracle"),

    // AI/ML
    TENSORFLOW(EInterestCategory.AI_ML, "TensorFlow"),
    PYTORCH(EInterestCategory.AI_ML, "PyTorch"),
    MACHINE_LEARNING(EInterestCategory.AI_ML, "Machine Learning"),
    DEEP_LEARNING(EInterestCategory.AI_ML, "Deep Learning"),

    // DevOps
    DOCKER(EInterestCategory.DEVOPS, "Docker"),
    KUBERNETES(EInterestCategory.DEVOPS, "Kubernetes"),
    DEVOPS_AWS(EInterestCategory.DEVOPS, "AWS"),
    CI_CD(EInterestCategory.DEVOPS, "CI/CD"),
    JENKINS(EInterestCategory.DEVOPS, "Jenkins"),

    // Cloud
    AWS(EInterestCategory.CLOUD, "AWS"),
    AZURE(EInterestCategory.CLOUD, "Azure"),
    GCP(EInterestCategory.CLOUD, "GCP"),
    FIREBASE(EInterestCategory.CLOUD, "Firebase"),

    // Systems/OS
    LINUX(EInterestCategory.SYSTEMS_OS, "Linux"),
    UNIX(EInterestCategory.SYSTEMS_OS, "Unix"),
    WINDOWS_SERVER(EInterestCategory.SYSTEMS_OS, "Windows Server"),
    SYSTEM_PROGRAMMING(EInterestCategory.SYSTEMS_OS, "시스템 프로그래밍"),

    // Networking
    TCP_IP(EInterestCategory.NETWORKING, "TCP/IP"),
    HTTP_HTTPS(EInterestCategory.NETWORKING, "HTTP/HTTPS"),
    RESTFUL_API(EInterestCategory.NETWORKING, "RESTful API"),
    WEBSOCKET(EInterestCategory.NETWORKING, "WebSocket"),

    // Security
    NETWORK_SECURITY(EInterestCategory.SECURITY, "네트워크 보안"),
    WEB_SECURITY(EInterestCategory.SECURITY, "웹 보안"),
    ENCRYPTION(EInterestCategory.SECURITY, "암호화"),
    AUTHENTICATION(EInterestCategory.SECURITY, "인증"),

    // Game Dev
    UNITY(EInterestCategory.GAME_DEV, "Unity"),
    UNREAL_ENGINE(EInterestCategory.GAME_DEV, "Unreal Engine"),
    GAME_CSHARP(EInterestCategory.GAME_DEV, "C#"),
    GAME_CPP(EInterestCategory.GAME_DEV, "C++"),

    // AR/VR/XR
    ARKIT(EInterestCategory.AR_VR_XR, "ARKit"),
    REALITYKIT(EInterestCategory.AR_VR_XR, "RealityKit"),
    UNITY_AR(EInterestCategory.AR_VR_XR, "Unity AR"),
    VR_DEVELOPMENT(EInterestCategory.AR_VR_XR, "VR Development"),

    // Embedded/IoT
    C(EInterestCategory.EMBEDDED_IOT, "C"),
    CPP(EInterestCategory.EMBEDDED_IOT, "C++"),
    ARDUINO(EInterestCategory.EMBEDDED_IOT, "Arduino"),
    RASPBERRY_PI(EInterestCategory.EMBEDDED_IOT, "Raspberry Pi"),
    RTOS(EInterestCategory.EMBEDDED_IOT, "RTOS"),

    // Blockchain/Web3
    ETHEREUM(EInterestCategory.BLOCKCHAIN_WEB3, "이더리움"),
    SMART_CONTRACT(EInterestCategory.BLOCKCHAIN_WEB3, "스마트 컨트랙트"),
    SOLIDITY(EInterestCategory.BLOCKCHAIN_WEB3, "Solidity"),
    WEB3(EInterestCategory.BLOCKCHAIN_WEB3, "Web3"),
    BLOCKCHAIN_BASICS(EInterestCategory.BLOCKCHAIN_WEB3, "블록체인 기초"),
    DAPP(EInterestCategory.BLOCKCHAIN_WEB3, "DApp"),
    NFT(EInterestCategory.BLOCKCHAIN_WEB3, "NFT"),
    CRYPTOCURRENCY(EInterestCategory.BLOCKCHAIN_WEB3, "암호화폐"),

    // QA/Test
    JUNIT(EInterestCategory.QA_TEST, "JUnit"),
    SELENIUM(EInterestCategory.QA_TEST, "Selenium"),
    TEST_AUTOMATION(EInterestCategory.QA_TEST, "Test Automation"),
    TDD(EInterestCategory.QA_TEST, "TDD"),

    // Product/UX
    FIGMA(EInterestCategory.PRODUCT_UX, "Figma"),
    SKETCH(EInterestCategory.PRODUCT_UX, "Sketch"),
    ADOBE_XD(EInterestCategory.PRODUCT_UX, "Adobe XD"),
    PROTOTYPING(EInterestCategory.PRODUCT_UX, "프로토타이핑"),

    // Architecture
    MICROSERVICES(EInterestCategory.ARCHITECTURE, "Microservices"),
    DDD(EInterestCategory.ARCHITECTURE, "DDD"),
    DESIGN_PATTERNS(EInterestCategory.ARCHITECTURE, "Design Patterns"),
    CLEAN_ARCHITECTURE(EInterestCategory.ARCHITECTURE, "Clean Architecture");

    private final EInterestCategory category;
    private final String displayName;

    public static List<EInterestKeyword> getKeywordsByCategory(EInterestCategory category) {
        return Arrays.stream(values())
                .filter(keyword -> keyword.category == category)
                .collect(Collectors.toList());
    }
}
