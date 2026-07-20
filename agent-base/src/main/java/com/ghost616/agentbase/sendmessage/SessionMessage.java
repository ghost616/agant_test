package com.ghost616.agentbase.sendmessage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SessionMessage implements MessageDefinition {

    private Long sessionId;
}
