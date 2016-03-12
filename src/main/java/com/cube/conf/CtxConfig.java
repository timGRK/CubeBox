package com.cube.conf;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
public class CtxConfig {
    @Bean(name = {"dataSource"})
    public DataSource dataSource() {
        return new ComboPooledDataSource();
    }
}
