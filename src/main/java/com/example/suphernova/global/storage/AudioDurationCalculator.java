package com.example.suphernova.global.storage;

import com.mpatric.mp3agic.Mp3File;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * mp3 바이트를 임시 파일로 내려받아 실제 프레임을 디코딩해 재생 길이(초)를 계산한다.
 * mp3agic이 스트림이 아닌 파일 경로만 지원해서 임시 파일을 거친다.
 */
@Slf4j
@Component
public class AudioDurationCalculator {

    public Integer calculateSeconds(byte[] mp3Bytes) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("tts-duration-", ".mp3");
            Files.write(tempFile, mp3Bytes);
            Mp3File mp3File = new Mp3File(tempFile.toFile());
            return (int) Math.round(mp3File.getLengthInSeconds());
        } catch (Exception e) {
            log.warn("TTS 오디오 길이 계산 실패", e);
            return null;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
