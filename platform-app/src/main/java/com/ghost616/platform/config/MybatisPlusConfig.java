package com.ghost616.platform.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@MapperScan("com.ghost616.platform.repository")
public class MybatisPlusConfig {

    @Bean
    public JdbcTemplate primaryJdbcTemplate(DynamicRoutingDataSource dynamicRoutingDataSource) {
        return new JdbcTemplate(dynamicRoutingDataSource.getDataSource("primary"));
    }

    @Bean
    public JdbcTemplate messageJdbcTemplate(DynamicRoutingDataSource dynamicRoutingDataSource) {
        return new JdbcTemplate(dynamicRoutingDataSource.getDataSource("message"));
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        return interceptor;
    }
}
