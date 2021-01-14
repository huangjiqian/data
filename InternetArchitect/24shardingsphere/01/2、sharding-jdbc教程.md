# 2 sharding-jdbc教程

​		Sharding-JDBC是ShardingSphere的第一个产品，也是ShardingSphere的前身。 它定位为轻量级Java框架，在Java的JDBC层提供的额外服务。它使用客户端直连数据库，以jar包形式提供服务，无需额外部署和依赖，可理解为增强版的JDBC驱动，完全兼容JDBC和各种ORM框架。

- 适用于任何基于JDBC的ORM框架，如：JPA, Hibernate, Mybatis, Spring JDBC Template或直接使用JDBC。
- 支持任何第三方的数据库连接池，如：DBCP, C3P0, BoneCP, Druid, HikariCP等。
- 支持任意实现JDBC规范的数据库。目前支持MySQL，Oracle，SQLServer，PostgreSQL以及任何遵循SQL92标准的数据库。

![](images\sharding-jdbc.png)

​		上面是官网对于sharding-jdbc的解释和介绍，其实说的直白一点，就是包含了分库分表功能的JDBC，因此我们可以直接把sharding-jdbc当做普通的jdbc来进行使用。

### 1、环境构建

​		1、创建一个springboot项目

​		2、导入如下依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.2.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.mashibing</groupId>
    <artifactId>shardingsphere_demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>shardingsphere_demo</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.1.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>1.1.23</version>
        </dependency>
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
            <version>4.1.1</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <skipTests>true</skipTests>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>

```

### 2、sharding-jdbc实现水平分表

#### 		1、创建sharding_sphere数据库

#### 		2、在数据库中创建两张表，orders_1和orders_2

#### 		3、分片规则：如果订单编号是偶数添加到orders_1,如果是奇数添加到orders_2

#### 		4、创建实体类

```java
package com.mashibing.shardingsphere_demo.bean;

public class Orders {
    private Integer id;
    private Integer orderType;
    private Integer customerId;
    private Double amount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Orders{" +
                "id=" + id +
                ", orderType=" + orderType +
                ", customerId=" + customerId +
                ", amount=" + amount +
                '}';
    }
}

```

#### 5、创建mapper类

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.Orders;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface OrdersMapper {

    @Insert("insert into orders(id,order_type,customer_id,amount) values(#{id},#{orderType},#{customerId},#{amount})")
    public void insert(Orders orders);

    @Select("select * from orders where id = #{id}")
    @Results({
            @Result(property = "id",column = "id"),
            @Result(property = "orderType",column = "order_type"),
            @Result(property = "customerId",column = "customer_id"),
            @Result(property = "amount",column = "amount")
    })
    public Orders selectOne(Integer id);
}
```

#### 6、创建配置文件

```properties
#整合mybatis
mybatis.type-aliases-package=com.mashibing.mapper

#配置数据源的名称
spring.shardingsphere.datasource.names=ds1


#配置数据源的具体内容，
spring.shardingsphere.datasource.ds1.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds1.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds1.url=jdbc:mysql://192.168.85.111:3306/sharding_sphere?serverTimezone=UTC
spring.shardingsphere.datasource.ds1.username=root
spring.shardingsphere.datasource.ds1.password=123456

#指定orders表的分布情况，配置表在哪个数据库中，表名称是什么
spring.shardingsphere.sharding.tables.orders.actual-data-nodes=ds1.orders_$->{1..2}
#指定orders表里主键id生成策略
spring.shardingsphere.sharding.tables.orders.key-generator.column=id
spring.shardingsphere.sharding.tables.orders.key-generator.type=SNOWFLAKE

#指定分片策略。根据id的奇偶性来判断插入到哪个表
spring.shardingsphere.sharding.tables.orders.table-strategy.inline.sharding-column=id
spring.shardingsphere.sharding.tables.orders.table-strategy.inline.algorithm-expression=orders_${id%2+1}

#打开sql输出日志
spring.shardingsphere.props.sql.show=true


```

#### 7、创建测试类

```java
package com.mashibing.shardingsphere_demo;

import com.mashibing.shardingsphere_demo.bean.Orders;
import com.mashibing.shardingsphere_demo.mapper.OrdersMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShardingsphereDemoApplicationTests {

    @Autowired
    private OrdersMapper ordersMapper;
    @Test
    public void addOrders(){
        for (int i = 1; i <=10 ; i++) {
            Orders orders = new Orders();
            orders.setId(i);
            orders.setCustomerId(i);
            orders.setOrderType(i);
            orders.setAmount(1000.0*i);
            ordersMapper.insert(orders);
        }
    }
    @Test
    public void queryOrders(){
        Orders orders = ordersMapper.selectOne(1);
        System.out.println(orders);
    }

}
```

### 3、sharding-jdbc实现水平分库

#### 		1、在不同的数据节点node01,node02上创建不同名称的数据库：sharding_sphere_1,sharding_sphere_2

#### 		2、在两个数据库上创建相同的表orders_1,orders_2

#### 		3、分片规则，按照customer_id的奇偶性来进行分库，然后按照id的奇偶性进行分表

#### 		4、修改配置文件

```properties
# 配置不同的数据源
spring.shardingsphere.datasource.names=ds1,ds2

#配置ds1数据源的基本信息
spring.shardingsphere.datasource.ds1.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds1.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds1.url=jdbc:mysql://192.168.85.111:3306/sharding_sphere_1?serverTimezone=UTC
spring.shardingsphere.datasource.ds1.username=root
spring.shardingsphere.datasource.ds1.password=123456

#配置ds2数据源的基本信息
spring.shardingsphere.datasource.ds2.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds2.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds2.url=jdbc:mysql://192.168.85.112:3306/sharding_sphere_2?serverTimezone=UTC
spring.shardingsphere.datasource.ds2.username=root
spring.shardingsphere.datasource.ds2.password=123456

#指定数据库的分布情况
spring.shardingsphere.sharding.tables.orders.actual-data-nodes=ds$->{1..2}.orders_$->{1..2}

#指定orders表的主键生成策略
spring.shardingsphere.sharding.tables.orders.key-generator.column=id
spring.shardingsphere.sharding.tables.orders.key-generator.type=SNOWFLAKE

#指定表分片策略，根据id的奇偶性来添加到不同的表中
spring.shardingsphere.sharding.tables.orders.table-strategy.inline.sharding-column=id
spring.shardingsphere.sharding.tables.orders.table-strategy.inline.algorithm-expression=orders_$->{id%2+1}

#指定库分片策略，根据customer_id的奇偶性来添加到不同的库中
spring.shardingsphere.sharding.tables.orders.database-strategy.inline.sharding-column=customer_id
spring.shardingsphere.sharding.tables.orders.database-strategy.inline.algorithm-expression=ds$->{customer_id%2+1}

#打开sql输出日志
spring.shardingsphere.props.sql.show=true
```

#### 5、修改mapper类

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.Orders;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface OrdersMapper {

    @Insert("insert into orders(id,order_type,customer_id,amount) values(#{id},#{orderType},#{customerId},#{amount})")
    public void insert(Orders orders);

    @Select("select * from orders where id = #{id}")
    @Results({
            @Result(property = "id",column = "id"),
            @Result(property = "orderType",column = "order_type"),
            @Result(property = "customerId",column = "customer_id"),
            @Result(property = "amount",column = "amount")
    })
    public Orders selectOne(Integer id);

    @Select("select * from orders where id = #{id} and customer_id=#{customerId}")
    @Results({
            @Result(property = "id",column = "id"),
            @Result(property = "orderType",column = "order_type"),
            @Result(property = "customerId",column = "customer_id"),
            @Result(property = "amount",column = "amount")
    })
    public Orders selectOneDB(Orders orders);

}

```

#### 6、编写测试类

```java
package com.mashibing.shardingsphere_demo;

import com.mashibing.shardingsphere_demo.bean.Orders;
import com.mashibing.shardingsphere_demo.mapper.OrdersMapper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
class ShardingsphereDemoApplicationTests {

    @Autowired
    private OrdersMapper ordersMapper;
    @Test
    public void addOrdersDB(){
        for (int i = 1; i <=10 ; i++) {
            Orders orders = new Orders();
            orders.setId(i);
            orders.setCustomerId(new Random().nextInt(10));
            orders.setOrderType(i);
            orders.setAmount(1000.0*i);
            ordersMapper.insert(orders);
        }
    }
    @Test
    public void queryOrdersDB(){
        Orders orders = new Orders();
        orders.setCustomerId(7);
        orders.setId(7);
        Orders o = ordersMapper.selectOneDB(orders);
        System.out.println(o);
    }
}
```

### 4、sharding-jdbc实现垂直分库

#### 		1、在不同的数据节点node01,node02创建相同的库sharding_sphere

#### 		2、在node01上创建orders表，在node02上创建customer表

#### 		3、分片规则：将不同的表插入到不同的库中

#### 		4、编写customer类

```java
package com.mashibing.shardingsphere_demo.bean;

public class Customer {

    private Integer id;
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

```

#### 		5、编写customerMapper类

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.Customer;
import org.apache.ibatis.annotations.Insert;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerMapper {
    @Insert("insert into customer(id,name) values(#{id},#{name})")
    public void insertCustomer(Customer customer);
}
```

#### 		6、修改配置文件

```properties
#配置数据源
spring.shardingsphere.datasource.names=ds1,ds2
#配置第一个数据源
spring.shardingsphere.datasource.ds1.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds1.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds1.url=jdbc:mysql://192.168.85.111:3306/sharding_sphere?serverTimezone=UTC
spring.shardingsphere.datasource.ds1.username=root
spring.shardingsphere.datasource.ds1.password=123456

#配置第二个数据源
spring.shardingsphere.datasource.ds2.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds2.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds2.url=jdbc:mysql://192.168.85.112:3306/sharding_sphere?serverTimezone=UTC
spring.shardingsphere.datasource.ds2.username=root
spring.shardingsphere.datasource.ds2.password=123456

#配置orders表所在的数据节点
#spring.shardingsphere.sharding.tables.order.actual-data-nodes=ds1.orders

#配置customer表所在的数据节点
spring.shardingsphere.sharding.tables.customer.actual-data-nodes=ds2.customer
#customer表的主键生成策略
spring.shardingsphere.sharding.tables.customer.key-generator.column=id
spring.shardingsphere.sharding.tables.customer.key-generator.type=SNOWFLAKE
#指定分片的策略
spring.shardingsphere.sharding.tables.customer.table-strategy.inline.sharding-column=id
spring.shardingsphere.sharding.tables.customer.table-strategy.inline.algorithm-expression=customer

#显示sql
spring.shardingsphere.props.sql.show=true

```

#### 		7、编写测试类

```java
package com.mashibing.shardingsphere_demo;

import com.mashibing.shardingsphere_demo.bean.Customer;
import com.mashibing.shardingsphere_demo.bean.Orders;
import com.mashibing.shardingsphere_demo.mapper.CustomerMapper;
import com.mashibing.shardingsphere_demo.mapper.OrdersMapper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
class ShardingsphereDemoApplicationTests {

    @Autowired
    private CustomerMapper customerMapper;
  
    @Test
    public void insertCustomer(){
        for (int i = 1; i <= 10 ; i++) {
            Customer customer = new Customer();
            customer.setId(i);
            customer.setName("zs"+i);
            customerMapper.insertCustomer(customer);
        }
    }
}

```

### 5、sharding-jdbc公共表

​		之前我们在学习mycat的时候接触过字典表的概念，其实在shardingsphere中也有类似的概念，只不过名字叫做公共表，也就是需要在各个库中都存在的表，方便做某些关联查询。

#### 		1、在不同节点的库上创建相同的表

#### 		2、分片规则：公共表表示所有的库都具备相同的表

#### 		3、创建实体类

```java
package com.mashibing.shardingsphere_demo.bean;

public class DictOrderType {

    private Integer id;
    private String orderType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    @Override
    public String toString() {
        return "DictOrderType{" +
                "id=" + id +
                ", orderType='" + orderType + '\'' +
                '}';
    }
}

```

#### 		4、创建DictOrderTypeMapper文件

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.DictOrderType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.springframework.stereotype.Repository;

@Repository
public interface DictOrderTypeMapper {

    @Insert("insert into dict_order_type(id,order_type) values(#{id},#{orderType})")
    public void insertDictOrderType(DictOrderType dictOrderType);

    @Delete("delete from dict_order_type where id = #{id}")
    public void DeleteDictOrderType(Integer id);
}

```

#### 		5、修改配置文件

```properties
#配置数据源
spring.shardingsphere.datasource.names=ds1,ds2
#配置第一个数据源
spring.shardingsphere.datasource.ds1.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds1.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds1.url=jdbc:mysql://192.168.85.111:3306/sharding_sphere?serverTimezone=UTC
spring.shardingsphere.datasource.ds1.username=root
spring.shardingsphere.datasource.ds1.password=123456

#配置第二个数据源
spring.shardingsphere.datasource.ds2.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds2.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds2.url=jdbc:mysql://192.168.85.112:3306/sharding_sphere?serverTimezone=UTC
spring.shardingsphere.datasource.ds2.username=root
spring.shardingsphere.datasource.ds2.password=123456

#配置公共表
spring.shardingsphere.sharding.broadcast-tables=dict_order_type
spring.shardingsphere.sharding.tables.dict_order_type.key-generator.column=id
spring.shardingsphere.sharding.tables.dict_order_type.key-generator.type=SNOWFLAKE

```

#### 		6、编写测试类

```java
package com.mashibing.shardingsphere_demo;

import com.mashibing.shardingsphere_demo.bean.Customer;
import com.mashibing.shardingsphere_demo.bean.DictOrderType;
import com.mashibing.shardingsphere_demo.bean.Orders;
import com.mashibing.shardingsphere_demo.mapper.CustomerMapper;
import com.mashibing.shardingsphere_demo.mapper.DictOrderTypeMapper;
import com.mashibing.shardingsphere_demo.mapper.OrdersMapper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
class ShardingsphereDemoApplicationTests {

    @Autowired
    private DictOrderTypeMapper dictOrderTypeMapper;

    @Test
    public void insertDictOrderType(){
        for (int i = 1; i <= 10 ; i++) {
            DictOrderType dictOrderType = new DictOrderType();
            dictOrderType.setOrderType("orderType"+i);
            dictOrderTypeMapper.insertDictOrderType(dictOrderType);
        }
    }

    @Test
    public void deleteDictOrderType(){
        dictOrderTypeMapper.DeleteDictOrderType(1);
    }
}

```

### 6、sharding-jdbc实现读写分离

​		读写分离的概念大家应该已经很熟练了，此处不在赘述，下面我们通过sharding-jdbc来实现读写分离，其实大家应该已经发现了，所有的操作都是配置问题，下面我们来讲一下具体的配置，关于读写分离的原理，以及如何配置mysql的主从复制，我们就不在多聊了，直接看sharding-jdbc的配置。

​		1、我们规定ds1为写库，ds2为读库

​		2、创建person类

```java
package com.mashibing.shardingsphere_demo.bean;

public class Person {

    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
```

​		3、创建personMapper类

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.Person;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonMapper {

    @Insert("insert into person(id,name) values(#{id},#{name})")
    public void insertPerson(Person person);

    @Select("select * from person where id = #{id}")
    public Person queryPerson(Long id);
}

```

​		4、修改配置文件

```properties
#配置数据源
spring.shardingsphere.datasource.names=ds1,ds2
#配置第一个数据源
spring.shardingsphere.datasource.ds1.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds1.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds1.url=jdbc:mysql://192.168.85.111:3306/shardingsphere?serverTimezone=UTC
spring.shardingsphere.datasource.ds1.username=root
spring.shardingsphere.datasource.ds1.password=123456

#配置第二个数据源
spring.shardingsphere.datasource.ds2.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.ds2.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds2.url=jdbc:mysql://192.168.85.112:3306/shardingsphere?serverTimezone=UTC
spring.shardingsphere.datasource.ds2.username=root
spring.shardingsphere.datasource.ds2.password=123456

#主库从库逻辑定义
spring.shardingsphere.masterslave.name=ms
spring.shardingsphere.masterslave.master-data-source-name=ds1
spring.shardingsphere.masterslave.slave-data-source-names=ds2

#显示执行的sql
spring.shardingsphere.props.sql.show=true

```

​		5、编写测试类

```java
package com.mashibing.shardingsphere_demo;

import com.mashibing.shardingsphere_demo.bean.Customer;
import com.mashibing.shardingsphere_demo.bean.DictOrderType;
import com.mashibing.shardingsphere_demo.bean.Orders;
import com.mashibing.shardingsphere_demo.bean.Person;
import com.mashibing.shardingsphere_demo.mapper.CustomerMapper;
import com.mashibing.shardingsphere_demo.mapper.DictOrderTypeMapper;
import com.mashibing.shardingsphere_demo.mapper.OrdersMapper;
import com.mashibing.shardingsphere_demo.mapper.PersonMapper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
class ShardingsphereDemoApplicationTests {

    @Autowired
    private PersonMapper personMapper;

    @Test
    public void insertPerson(){
        Person person = new Person();
        person.setId(1l);
        person.setName("zhangsan");
        personMapper.insertPerson(person);
    }

    @Test
    public void queryPerson(){
        Person person = personMapper.queryPerson(1l);
        System.out.println(person);
    }
}

```

