package com.omarahmed42.socialmedia.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Path;

public interface FileService {
    public long copy(InputStream in, Path target, CopyOption... options) throws IOException;
    public boolean remove(Path path) throws IOException;
}
