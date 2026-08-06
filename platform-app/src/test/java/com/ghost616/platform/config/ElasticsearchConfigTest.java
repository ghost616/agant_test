package com.ghost616.platform.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ElasticsearchConfigTest {

    private ElasticsearchConfig createConfig(String host, int port, String username, String password) throws Exception {
        ElasticsearchConfig config = new ElasticsearchConfig();
        setField(config, "host", host);
        setField(config, "port", port);
        setField(config, "username", username);
        setField(config, "password", password);
        return config;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("默认配置 localhost:9200，创建 Transport 与 Client Bean")
    void 默认配置创建Bean() throws Exception {
        ElasticsearchConfig config = createConfig("localhost", 9200, "", "");

        ElasticsearchTransport transport = config.elasticsearchTransport();
        ElasticsearchClient client = config.elasticsearchClient(transport);

        assertNotNull(transport);
        assertInstanceOf(RestClientTransport.class, transport);
        assertNotNull(client);

        RestClientTransport restClientTransport = (RestClientTransport) transport;
        RestClient restClient = restClientTransport.restClient();
        assertNotNull(restClient);
        assertEquals("localhost", restClient.getNodes().get(0).getHost().getHostName());
        assertEquals(9200, restClient.getNodes().get(0).getHost().getPort());

        restClient.close();
    }

    @Test
    @DisplayName("自定义 host/port 时使用配置值")
    void 自定义hostPort() throws Exception {
        ElasticsearchConfig config = createConfig("es.example.com", 9201, "", "");

        ElasticsearchTransport transport = config.elasticsearchTransport();
        RestClientTransport restClientTransport = (RestClientTransport) transport;
        RestClient restClient = restClientTransport.restClient();

        assertEquals("es.example.com", restClient.getNodes().get(0).getHost().getHostName());
        assertEquals(9201, restClient.getNodes().get(0).getHost().getPort());

        restClient.close();
    }

    @Test
    @DisplayName("elasticsearchTransport Bean 声明 destroyMethod=close")
    void destroyMethod为close() throws Exception {
        Bean bean = ElasticsearchConfig.class.getMethod("elasticsearchTransport").getAnnotation(Bean.class);
        assertNotNull(bean);
        assertEquals("close", bean.destroyMethod());
    }

    @Test
    @DisplayName("配置用户名密码时注入 BasicCredentialsProvider 认证")
    void 配置认证时注入凭据() throws Exception {
        ElasticsearchConfig config = createConfig("localhost", 9200, "user1", "pass1");

        ElasticsearchTransport transport = config.elasticsearchTransport();
        RestClientTransport restClientTransport = (RestClientTransport) transport;
        CredentialsProvider provider = extractCredentialsProvider(restClientTransport.restClient());

        assertNotNull(provider, "应配置 CredentialsProvider");
        assertInstanceOf(BasicCredentialsProvider.class, provider);
        assertNotNull(provider.getCredentials(AuthScope.ANY), "应包含默认作用域的凭据");
        assertEquals("user1", provider.getCredentials(AuthScope.ANY).getUserPrincipal().getName());

        restClientTransport.restClient().close();
    }

    @Test
    @DisplayName("未配置用户名密码时不设置认证")
    void 未配置认证() throws Exception {
        ElasticsearchConfig config = createConfig("localhost", 9200, "", "");

        ElasticsearchTransport transport = config.elasticsearchTransport();
        RestClientTransport restClientTransport = (RestClientTransport) transport;
        CredentialsProvider provider = extractCredentialsProvider(restClientTransport.restClient());

        assertNotNull(provider, "即使未配置也会存在默认凭据提供器");
        assertNull(provider.getCredentials(AuthScope.ANY), "未配置时不应包含任何凭据");

        restClientTransport.restClient().close();
    }

    @Test
    @DisplayName("@Value 字段带有正确默认值")
    void value字段默认值() throws Exception {
        Value host = ElasticsearchConfig.class.getDeclaredField("host").getAnnotation(Value.class);
        Value port = ElasticsearchConfig.class.getDeclaredField("port").getAnnotation(Value.class);
        Value username = ElasticsearchConfig.class.getDeclaredField("username").getAnnotation(Value.class);
        Value password = ElasticsearchConfig.class.getDeclaredField("password").getAnnotation(Value.class);

        assertEquals("${elasticsearch.host:localhost}", host.value());
        assertEquals("${elasticsearch.port:9200}", port.value());
        assertEquals("${elasticsearch.username:}", username.value());
        assertEquals("${elasticsearch.password:}", password.value());
    }

    private CredentialsProvider extractCredentialsProvider(RestClient restClient) throws Exception {
        Object httpClient = restClient.getHttpClient();
        Field field = httpClient.getClass().getDeclaredField("credentialsProvider");
        field.setAccessible(true);
        return (CredentialsProvider) field.get(httpClient);
    }

    @SuppressWarnings("unused")
    private List<?> unusedHelper() {
        return List.of();
    }
}
