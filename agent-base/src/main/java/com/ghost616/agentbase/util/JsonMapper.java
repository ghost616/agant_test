package com.ghost616.agentbase.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonMapper {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonMapper() {
    }
}
