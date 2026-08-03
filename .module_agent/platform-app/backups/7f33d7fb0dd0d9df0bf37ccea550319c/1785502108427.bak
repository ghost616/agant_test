package com.ghost616.platform.dto;

import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class AgentContextDTO {

    private Long sessionId;
    private Long agentId;
    private String systemPrompt;
    private Long modelId;
    private Long parentSessionId;
    private Integer recentMessageCount;
    private List<HistoryEntryDTO> history;
    private List<ToolConfigDTO> tools;
    private List<SkillConfigDTO> skills;
    private String projectDir;
    private Map<String, String> sessionVariables;
    private Map<String, String> conversationVariables;

    @Data
    public static class HistoryEntryDTO {
        private String role;
        private String content;
        private String reasoning;
        private String toolCallId;
        private int sequenceNum;
        private LocalDateTime createTime;
        private List<ToolCall> toolCalls;
        private UsageInfo usage;
    }
}
