package com.omarahmed42.socialmedia.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.service.FileService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    public long copy(InputStream in, Path target, CopyOption... options) throws IOException {
        FileUtils.forceMkdir(target.getParent().toFile());
        return Files.copy(in, target, options);
    }

    @Retryable(retryFor = { IOException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1500), recover = "recoverRemoveFile"

    )
    public boolean remove(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    @Recover
    public boolean recoverRemoveFile(IOException e, Path path) {
        log.error("Failed to delete file after retries: {}", path, e);
        return false;
    }
}
