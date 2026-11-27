package com.example.coffee.config;

import javax.sql.DataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@MapperScan("com.example.coffee.persistence.dao")
public class PersistenceConfiguration {
  @Bean
  DataSourceTransactionManager transactionManager(DataSource dataSource) {
    // MyBatis-Spring uses this same DataSource and joins TransactionTemplate's transaction.
    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
    // MANDATORY requires an existing transaction; strict validation also rejects a read-only one.
    transactionManager.setValidateExistingTransaction(true);
    return transactionManager;
  }
}
