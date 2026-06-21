package com.ghost616.platform.systemtest;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ghost616.platform.service.agent.AgentExecutionContext;
import com.ghost616.platform.service.agent.invoker.ToolInvoker;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统信息工具，返回当前运行环境的 OS、JVM、内存及磁盘信息。
 *
 * @author ghost616
 */
@Slf4j
public class SystemInfoTool implements ToolInvoker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 获取当前系统信息并以 JSON 字符串返回。
     *
     * @param ctx       智能体执行上下文（只读）
     * @param arguments JSON 格式的参数字符串（本工具无需参数）
     * @return 包含系统信息的 JSON 字符串
     */
    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();

            ObjectNode os = OBJECT_MAPPER.createObjectNode();
            os.put("name", System.getProperty("os.name"));
            os.put("version", System.getProperty("os.version"));
            os.put("arch", System.getProperty("os.arch"));
            root.set("os", os);

            ObjectNode jvm = OBJECT_MAPPER.createObjectNode();
            jvm.put("version", System.getProperty("java.version"));
            jvm.put("vendor", System.getProperty("java.vendor"));
            root.set("jvm", jvm);

            Runtime runtime = Runtime.getRuntime();
            root.put("availableProcessors", runtime.availableProcessors());

            ObjectNode memory = OBJECT_MAPPER.createObjectNode();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            memory.put("totalMemory", totalMemory);
            memory.put("freeMemory", freeMemory);
            memory.put("usedMemory", totalMemory - freeMemory);
            root.set("memory", memory);

            ArrayNode disks = OBJECT_MAPPER.createArrayNode();
            for (File diskRoot : File.listRoots()) {
                ObjectNode disk = OBJECT_MAPPER.createObjectNode();
                disk.put("path", diskRoot.getAbsolutePath());
                disk.put("totalSpace", diskRoot.getTotalSpace());
                disk.put("usableSpace", diskRoot.getUsableSpace());
                disks.add(disk);
            }
            root.set("disks", disks);

            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            return "{\"error\":\"获取系统信息失败: " + e.getMessage() + "\"}";
        }
    }
}
