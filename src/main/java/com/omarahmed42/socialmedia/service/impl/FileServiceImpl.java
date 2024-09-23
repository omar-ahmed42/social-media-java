package com.omarahmed42.socialmedia.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.service.FileService;

@Service
public class FileServiceImpl implements FileService {

    public long copy(InputStream in, Path target, CopyOption... options) throws IOException {
        FileUtils.forceMkdir(target.getParent().toFile());
        return Files.copy(in, target, options);
    }
}
