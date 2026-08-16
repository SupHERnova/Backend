package com.example.suphernova.global.storage;

import com.example.suphernova.global.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 로컬 파일시스템에 오디오 파일을 저장한다. 추후 S3 등 객체 스토리지로 교체할 때는
 * {@link AudioStorageService} 구현체만 갈아끼우면 되도록 의도적으로 인터페이스와 분리했다.
 */
@Component
@RequiredArgsConstructor
public class LocalAudioStorageService implements AudioStorageService {

    private final AppProperties appProperties;

    @Override
    public StoredAudio store(String directory, String fileName, InputStream content, String contentType) throws IOException {
        Path targetDir = Path.of(appProperties.storage().audioBasePath(), directory);
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(fileName);
        long sizeBytes = Files.copy(content, targetFile, StandardCopyOption.REPLACE_EXISTING);

        String url = "/files/%s/%s".formatted(directory, fileName);
        return new StoredAudio(url, sizeBytes);
    }
}
