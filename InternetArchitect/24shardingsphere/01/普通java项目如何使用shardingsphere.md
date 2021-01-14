# 普通java项目如何使用shardingsphere

1、创建普通maven项目

2、导入对应的依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>shardingsphere_java</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-core</artifactId>
            <version>4.1.1</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/com.alibaba/druid -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>1.1.23</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/mysql/mysql-connector-java -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.21</version>
        </dependency>

    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

3、编写对应的测试类

```java
package com.mashibing;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.InlineShardingStrategyConfiguration;
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class Test {

    public static void main(String[] args) {
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        // 配置第一个数据源
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://192.168.85.111:3306/sharding_sphere_1");
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("123456");
        dataSourceMap.put("ds1",druidDataSource);
        // 配置第二个数据源
        DruidDataSource druidDataSource2 = new DruidDataSource();
        druidDataSource2.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource2.setUrl("jdbc:mysql://192.168.85.112:3306/sharding_sphere_2");
        druidDataSource2.setUsername("root");
        druidDataSource2.setPassword("123456");
        dataSourceMap.put("ds2",druidDataSource2);
        // 配置orders表规则
        TableRuleConfiguration orderTableRuleConfig = new TableRuleConfiguration("orders","ds${1..2}.orders_${1..2}");

        // 配置分库+分表策略
        orderTableRuleConfig.setDatabaseShardingStrategyConfig(new InlineShardingStrategyConfiguration("customer_id","ds${customer_id%2+1}"));
        orderTableRuleConfig.setTableShardingStrategyConfig(new InlineShardingStrategyConfiguration("id","orders_$->{id%2+1}"));

        // 配置分片规则
        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        shardingRuleConfiguration.getTableRuleConfigs().add(orderTableRuleConfig);

        // 获取数据源对象
        try {
            DataSource dataSource = ShardingDataSourceFactory.createDataSource(dataSourceMap,shardingRuleConfiguration,new Properties());
            Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("insert into orders(id,order_type,customer_id,amount) values(?,?,?,?)");
            for (int i = 400; i <410 ; i++) {
                preparedStatement.setInt(1,i);
                preparedStatement.setInt(2,i);
                preparedStatement.setInt(3,new Random().nextInt(10));
                preparedStatement.setDouble(4,i*10.0);
                preparedStatement.execute();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }
}
```

