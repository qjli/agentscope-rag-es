package io.agentscope.rag.kb.config;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 将 {@code ./data} 等相对路径解析为基于进程启动目录（user.dir）的绝对路径，
 * 避免 IDE / mvn 工作目录不一致导致注册表读写错位。
 */
@Component
public class OpsDataPaths {

    private static final Logger log = LoggerFactory.getLogger(OpsDataPaths.class);

    private final OpsProperties opsProperties;

    private Path dataDir;
    private Path uploadDir;
    private Path registryFile;

    public OpsDataPaths(OpsProperties opsProperties) {
        this.opsProperties = opsProperties;
    }

    @PostConstruct
    void init() throws java.io.IOException {
        Path base = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        dataDir = resolve(base, opsProperties.getDataDir());
        uploadDir = resolve(base, opsProperties.getUploadDir());
        registryFile = resolve(base, opsProperties.getRegistryFile());
        Files.createDirectories(dataDir);
        Files.createDirectories(uploadDir);
        Files.createDirectories(registryFile.getParent());
        log.info("Ops data dir: {}", dataDir);
        log.info("Ops upload dir: {}", uploadDir);
        log.info("Knowledge base registry: {}", registryFile);
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    public Path getRegistryFile() {
        return registryFile;
    }

    private static Path resolve(Path base, String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return base.resolve(path).normalize();
    }
}
