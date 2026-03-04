package com.techfork.evaluation.recommendation.setup.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JSON 파일 export를 위한 공통 유틸리티 클래스
 */
@Slf4j
@Component
public class FileExporter {

    private static final String OUTPUT_DIR = "src/test/resources/fixtures/evaluation";

    private final ObjectMapper objectMapper;

    public FileExporter() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void ensureOutputDirectory() throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outputPath);
        log.info("출력 디렉토리: {}", outputPath.toAbsolutePath());
    }

    public String getOutputDir() {
        return OUTPUT_DIR;
    }

    public void writeJsonFile(String filename, Object data) throws IOException {
        File outputFile = new File(OUTPUT_DIR, filename);
        objectMapper.writeValue(outputFile, data);
        log.debug("파일 작성: {}", outputFile.getAbsolutePath());
    }
}
