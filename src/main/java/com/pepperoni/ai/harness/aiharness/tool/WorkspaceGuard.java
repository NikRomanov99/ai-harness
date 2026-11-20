package com.pepperoni.ai.harness.aiharness.tool;

import com.pepperoni.ai.harness.aiharness.agent.AgentProperties;
import lombok.Getter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class WorkspaceGuard {

    @Getter
    private final Path root;

    public WorkspaceGuard(AgentProperties agentProperties) {
        this.root = Path.of(agentProperties.workspaceRoot()).toAbsolutePath().normalize();
    }

    public Path resolve(String rawPath) {
        if (Strings.isBlank(rawPath)) {
            throw new WorkSpaceViolationException("Empty path");
        }

        final var candidate = Path.of(rawPath);
        final var resolved = Optional.of(candidate).filter(Path::isAbsolute).orElseGet(() -> root.resolve(candidate).normalize());

        if (!resolved.startsWith(root)) {
            throw new WorkSpaceViolationException("Invalid path, path out working directory: %s. Working directory: %s".formatted(resolved, root));
        }

        try {
            final var realPath = resolved.toRealPath();
            if (!realPath.startsWith(root.toRealPath())) {
                throw new WorkSpaceViolationException("Invalid path, path lead to out with simple link: " + rawPath);
            }
        } catch (NoSuchFileException ex) {
            //Пути ещё нет: модель ошиблась именем файла или файл только предстоит создать
            //Симплинка у несуществующего пути быть не может - проверять нечего
        } catch (IOException cannotVerify) {
            throw new WorkSpaceViolationException("Can not verify path: " + rawPath + " error: " + cannotVerify);
        }

        return resolved;
    }

    public String relative(Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }


    //TODO Excep
    public class WorkSpaceViolationException extends RuntimeException {
        public WorkSpaceViolationException(String string) {
            super(string);
        }
    }
}
