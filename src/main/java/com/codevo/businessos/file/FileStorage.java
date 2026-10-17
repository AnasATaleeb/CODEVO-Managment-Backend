package com.codevo.businessos.file;
import java.io.IOException;
import org.springframework.core.io.Resource;
public interface FileStorage {
    String store(byte[] bytes) throws IOException;
    Resource read(String key) throws IOException;
    void remove(String key) throws IOException;
}
