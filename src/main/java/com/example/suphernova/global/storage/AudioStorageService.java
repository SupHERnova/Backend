package com.example.suphernova.global.storage;

import java.io.IOException;
import java.io.InputStream;

public interface AudioStorageService {

    /**
     * @param directory   저장 하위 경로 (예: "input", "tts")
     * @param fileName    저장 파일명 (확장자 포함)
     * @param content     오디오 바이트 스트림
     * @param contentType MIME 타입
     */
    StoredAudio store(String directory, String fileName, InputStream content, String contentType) throws IOException;
}
