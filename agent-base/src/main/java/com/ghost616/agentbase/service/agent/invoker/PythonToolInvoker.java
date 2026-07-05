package com.ghost616.agentbase.service.agent.invoker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PythonToolInvoker implements ToolInvoker {

    private static final long TIMEOUT_SECONDS = 30;
    private static final String RUNNER_FILE_NAME = "_runner.py";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path scriptDir;
    private final Path runnerPath;
    private final boolean python3Available;
    private final boolean pythonAvailable;

    public PythonToolInvoker(String scriptPath) {
        this.scriptDir = Path.of(scriptPath).toAbsolutePath();
        if (!Files.isDirectory(this.scriptDir)) {
            throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR,
                    "脚本路径不是目录: " + scriptPath);
        }
        this.runnerPath = this.scriptDir.resolve(RUNNER_FILE_NAME);
        this.python3Available = isCommandAvailable("python3");
        this.pythonAvailable = isCommandAvailable("python");
        if (!python3Available && !pythonAvailable) {
            throw new BusinessException(ErrorCode.TOOL_RUNTIME_NOT_FOUND,
                    "python 和 python3 运行时均不可用，请安装 Python 3.10+");
        }
        log.info("Python运行时检测: python3={}, python={}, scriptDir={}",
                python3Available, pythonAvailable, scriptDir);
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows");
    }

    private boolean isCommandAvailable(String command) {
        try {
            List<String> cmd;
            if (isWindows()) {
                cmd = List.of("cmd", "/c", command, "--version");
            } else {
                cmd = List.of(command, "--version");
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        if (!Files.exists(runnerPath)) {
            synchronized (this) {
                if (!Files.exists(runnerPath)) {
                    generateRunnerFile();
                }
            }
        }
        log.debug("scriptDir={} arguments={}", scriptDir, arguments);
        String jsonParams = ContextSerializer.serializeToJson(ctx, arguments);

        List<String> command = new ArrayList<>(resolveRuntime());
        command.add(runnerPath.toString());

        Path inputFile = scriptDir.resolve("_input.json");
        try {
            Files.writeString(inputFile, jsonParams, StandardCharsets.UTF_8);
            command.add(inputFile.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(scriptDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("工具执行超时 ({}秒): {}", TIMEOUT_SECONDS, scriptDir);
                throw new BusinessException(ErrorCode.TOOL_EXECUTE_TIMEOUT,
                        "工具执行超时（" + TIMEOUT_SECONDS + "秒）: " + scriptDir);
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();
            if (exitCode != 0) {
                log.error("工具执行失败, exitCode={}, dir={}, output={}", exitCode, scriptDir, result);
                throw new BusinessException(ErrorCode.TOOL_EXECUTE_ERROR,
                        "工具执行失败, exitCode=" + exitCode + ": " + result);
            }

            return parseResult(ctx, result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行工具异常: {}", scriptDir, e);
            throw new BusinessException(ErrorCode.TOOL_EXECUTE_ERROR,
                    "执行工具异常: " + scriptDir + " - " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(inputFile);
            } catch (Exception ignored) {
            }
        }
    }

    private String parseResult(AgentExecutionContext ctx, String raw) {
        try {
            JsonNode root = MAPPER.readTree(raw);
            if (root.has("result")) {
                if (root.has("sessionVariables")) {
                    JsonNode sv = root.get("sessionVariables");
                    if (sv.has("added")) {
                        JsonNode added = sv.get("added");
                        added.fields().forEachRemaining(e -> ctx.putSessionVariable(e.getKey(), e.getValue().asText()));
                    }
                    if (sv.has("removed")) {
                        for (JsonNode key : sv.get("removed")) {
                            ctx.removeSessionVariable(key.asText());
                        }
                    }
                }
                if (root.has("conversationVariables")) {
                    JsonNode cv = root.get("conversationVariables");
                    if (cv.has("added")) {
                        JsonNode added = cv.get("added");
                        added.fields().forEachRemaining(e -> ctx.putConversationVariable(e.getKey(), e.getValue().asText()));
                    }
                    if (cv.has("removed")) {
                        for (JsonNode key : cv.get("removed")) {
                            ctx.removeConversationVariable(key.asText());
                        }
                    }
                }
                return root.get("result").asText();
            }
        } catch (Exception e) {
            log.debug("非结构化 JSON 输出，按纯文本返回: {}", e.getMessage());
        }
        return raw;
    }

    private List<String> resolveRuntime() {
        if (python3Available) {
            if (isWindows()) {
                return List.of("cmd", "/c", "python3");
            }
            return List.of("python3");
        }
        if (pythonAvailable) {
            if (isWindows()) {
                return List.of("cmd", "/c", "python");
            }
            return List.of("python");
        }
        throw new BusinessException(ErrorCode.TOOL_RUNTIME_NOT_FOUND,
                "python 和 python3 运行时均不可用");
    }

    private void generateRunnerFile() {
        try {
            Files.writeString(runnerPath, RunnerTemplate.INSTANCE, StandardCharsets.UTF_8);
            log.info("已生成桥接文件: {}", runnerPath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR,
                    "生成桥接文件失败: " + runnerPath + " - " + e.getMessage());
        }
    }

    private static class RunnerTemplate {
        static final String INSTANCE = loadRunnerTemplate();
    }

    private static String loadRunnerTemplate() {
        try (InputStream in = PythonToolInvoker.class.getResourceAsStream("/agent/_runner.py")) {
            if (in == null) {
                throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR,
                        "未找到桥接文件模板: /agent/_runner.py");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR,
                    "加载桥接文件模板失败: " + e.getMessage());
        }
    }
}
