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

### 7、sharding-jdbc强制路由

ShardingSphere使用ThreadLocal管理分片键值进行Hint强制路由。可以通过编程的方式向HintManager中添加分片值，该分片值仅在当前线程内生效。 Hint方式主要使用场景：

1.分片字段不存在SQL中、数据库表结构中，而存在于外部业务逻辑。

2.强制在主库进行某些数据操作。

具体操作：

order.java

```java
package com.mashibing.shardingsphere_demo.bean;

import java.io.Serializable;

public class Order implements Serializable {
    
    private static final long serialVersionUID = 661434701950670670L;
    
    private long orderId;
    
    private int userId;
    
    private long addressId;
    
    private String status;
    
    public long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(final long orderId) {
        this.orderId = orderId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(final int userId) {
        this.userId = userId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(final String status) {
        this.status = status;
    }
    
    public long getAddressId() {
        return addressId;
    }
    
    public void setAddressId(final long addressId) {
        this.addressId = addressId;
    }
    
    @Override
    public String toString() {
        return String.format("order_id: %s, user_id: %s, address_id: %s, status: %s", orderId, userId, addressId, status);
    }
}
```

orderItem.java

```java
package com.mashibing.shardingsphere_demo.bean;

import java.io.Serializable;

public class OrderItem implements Serializable {
    
    private static final long serialVersionUID = 263434701950670170L;
    
    private long orderItemId;
    
    private long orderId;
    
    private int userId;
    
    private String status;
    
    public long getOrderItemId() {
        return orderItemId;
    }
    
    public void setOrderItemId(final long orderItemId) {
        this.orderItemId = orderItemId;
    }
    
    public long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(final long orderId) {
        this.orderId = orderId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(final int userId) {
        this.userId = userId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(final String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return String.format("order_item_id:%s, order_id: %s, user_id: %s, status: %s", orderItemId, orderId, userId, status);
    }
}
```

Address.java

```java
package com.mashibing.shardingsphere_demo.bean;

public class Address {
    
    private static final long serialVersionUID = 661434701950670670L;
    
    private Long addressId;
    
    private String addressName;
    
    public Long getAddressId() {
        return addressId;
    }
    
    public void setAddressId(final Long addressId) {
        this.addressId = addressId;
    }
    
    public String getAddressName() {
        return addressName;
    }
    
    public void setAddressName(final String addressName) {
        this.addressName = addressName;
    }
}
```

MyHintalgorithm.java

```java
package com.mashibing.shardingsphere_demo.hint;

import org.apache.shardingsphere.api.sharding.hint.HintShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.hint.HintShardingValue;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class MyHintAlgorithm implements HintShardingAlgorithm<Long> {
    @Override
    public Collection<String> doSharding(final Collection<String> availableTargetNames, final HintShardingValue<Long> shardingValue) {
        Collection<String> result = new ArrayList<>();
        for (String each : availableTargetNames) {
            for (Long value : shardingValue.getValues()) {
                if (each.endsWith(String.valueOf(value % 2))) {
                    result.add(each);
                }
            }
        }
        return result;
    }
}

```

HintType.java

```java

package com.mashibing.shardingsphere_demo.hint;

public enum HintType {
    
    DATABASE_ONLY, DATABASE_TABLES, MASTER_ONLY
}

```

HintMain.java

```java
package com.mashibing.shardingsphere_demo.hint;

import com.mashibing.shardingsphere_demo.service.ExampleService;
import com.mashibing.shardingsphere_demo.service.OrderServiceImpl;
import org.apache.shardingsphere.api.hint.HintManager;
import org.apache.shardingsphere.shardingjdbc.api.yaml.YamlMasterSlaveDataSourceFactory;
import org.apache.shardingsphere.shardingjdbc.api.yaml.YamlShardingDataSourceFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class HintMain {

    private static final HintType TYPE = HintType.DATABASE_TABLES;
//        private static final HintType TYPE = HintType.DATABASE_ONLY;
//    private static final HintType TYPE = HintType.MASTER_ONLY;
    public static void main(String[] args) throws IOException, SQLException {
        DataSource dataSource = getDataSource();
        ExampleService exampleService = getExampleService(dataSource);
        exampleService.initEnvironment();
        exampleService.processSuccess();
        processWithHintValue(dataSource);
//        exampleService.cleanEnvironment();
    }

    private static DataSource getDataSource() throws IOException, SQLException {
        switch (TYPE) {
            case DATABASE_TABLES:
                return YamlShardingDataSourceFactory.createDataSource(getFile("F:\\selfproject\\shardingsphere_demo\\src\\main\\resources\\hint-databases-tables.yaml"));
            case DATABASE_ONLY:
                return YamlShardingDataSourceFactory.createDataSource(getFile("F:\\selfproject\\shardingsphere_demo\\src\\main\\resources\\hint-databases-only.yaml"));
            case MASTER_ONLY:
                return YamlMasterSlaveDataSourceFactory.createDataSource(getFile("F:\\selfproject\\shardingsphere_demo\\src\\main\\resources\\hint-master-only.yaml"));
            default:
                throw new UnsupportedOperationException("unsupported type");
        }
    }

    private static File getFile(final String configFile) {
        return new File(configFile);
    }

    private static ExampleService getExampleService(final DataSource dataSource) {
        return new OrderServiceImpl(dataSource);
    }

    private static void processWithHintValue(final DataSource dataSource) throws SQLException {
        try (HintManager hintManager = HintManager.getInstance();
             Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            setHintValue(hintManager);
            statement.execute("select * from t_order");
            statement.execute("SELECT i.* FROM t_order o, t_order_item i WHERE o.order_id = i.order_id");
            statement.execute("select * from t_order_item");
            statement.execute("INSERT INTO t_order (user_id, address_id, status) VALUES (1, 1, 'init')");
        }
    }

    private static void setHintValue(final HintManager hintManager) {
        switch (TYPE) {
            case DATABASE_TABLES:
                hintManager.addDatabaseShardingValue("t_order", 1L);
                hintManager.addTableShardingValue("t_order", 1L);
                return;
            case DATABASE_ONLY:
                hintManager.setDatabaseShardingValue(1L);
                return;
            case MASTER_ONLY:
                hintManager.setMasterRouteOnly();
                return;
            default:
                throw new UnsupportedOperationException("unsupported type");
        }
    }
}

```

CommonMapper.java

```java
package com.mashibing.shardingsphere_demo.mapper;

import java.sql.SQLException;
import java.util.List;

public interface CommonMapper<T, P> {
    
    /**
     * Create table if not exist.
     * 
     * @throws SQLException SQL exception
     */
    void createTableIfNotExists() throws SQLException;
    
    /**
     * Drop table.
     * 
     * @throws SQLException SQL exception
     */
    void dropTable() throws SQLException;
    
    /**
     * Truncate table.
     * 
     * @throws SQLException SQL exception
     */
    void truncateTable() throws SQLException;
    
    /**
     * insert data.
     * 
     * @param entity entity
     * @return generated primary key
     * @throws SQLException SQL exception
     */
    P insert(T entity) throws SQLException;
    
    /**
     * Delete data.
     * 
     * @param primaryKey primaryKey
     * @throws SQLException SQL exception
     */
    void delete(P primaryKey) throws SQLException;
    
    /**
     * Select all data.
     * 
     * @return all data
     * @throws SQLException SQL exception
     */
    List<T> selectAll() throws SQLException;
}

```

OrderMapper.java

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.Order;

public interface OrderMapper extends CommonMapper<Order,Long>{
}

```

OrderMapperImpl.java

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.Order;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class OrderMapperImpl implements OrderMapper {

    private DataSource dataSource;

    public OrderMapperImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS t_order (order_id BIGINT NOT NULL AUTO_INCREMENT, user_id INT NOT NULL, address_id BIGINT NOT NULL, status VARCHAR(50), PRIMARY KEY (order_id))";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public void dropTable() throws SQLException {
        String sql = "DROP TABLE t_order";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public void truncateTable() throws SQLException {
        String sql = "TRUNCATE TABLE t_order";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public Long insert(final Order order) throws SQLException {
        String sql = "INSERT INTO t_order (user_id, address_id, status) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, order.getUserId());
            preparedStatement.setLong(2, order.getAddressId());
            preparedStatement.setString(3, order.getStatus());
            preparedStatement.executeUpdate();
            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    order.setOrderId(resultSet.getLong(1));
                }
            }
        }
        return order.getOrderId();
    }

    @Override
    public void delete(final Long orderId) throws SQLException {
        String sql = "DELETE FROM t_order WHERE order_id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, orderId);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<Order> selectAll() throws SQLException {
        String sql = "SELECT * FROM t_order";
        return getOrders(sql);
    }

    protected List<Order> getOrders(final String sql) throws SQLException {
        List<Order> result = new LinkedList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Order order = new Order();
                order.setOrderId(resultSet.getLong(1));
                order.setUserId(resultSet.getInt(2));
                order.setAddressId(resultSet.getLong(3));
                order.setStatus(resultSet.getString(4));
                result.add(order);
            }
        }
        return result;
    }
}

```

OrderItemMapper.java

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.OrderItem;

public interface OrderItemMapper extends CommonMapper<OrderItem,Long> {

}

```

OrderItemMapperImpl.java

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.OrderItem;

import javax.sql.DataSource;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class OrderItemMapperImpl implements OrderItemMapper{

    private DataSource dataSource;

    public OrderItemMapperImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS t_order_item "
                + "(order_item_id BIGINT NOT NULL AUTO_INCREMENT, order_id BIGINT NOT NULL, user_id INT NOT NULL, status VARCHAR(50), PRIMARY KEY (order_item_id))";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public void dropTable() throws SQLException {
        String sql = "DROP TABLE t_order_item";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public void truncateTable() throws SQLException {
        String sql = "TRUNCATE TABLE t_order_item";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public Long insert(final OrderItem orderItem) throws SQLException {
        String sql = "INSERT INTO t_order_item (order_id, user_id, status) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, orderItem.getOrderId());
            preparedStatement.setInt(2, orderItem.getUserId());
            preparedStatement.setString(3, orderItem.getStatus());
            preparedStatement.executeUpdate();
            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    orderItem.setOrderItemId(resultSet.getLong(1));
                }
            }
        }
        return orderItem.getOrderItemId();
    }

    @Override
    public void delete(final Long orderItemId) throws SQLException {
        String sql = "DELETE FROM t_order_item WHERE order_item_id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, orderItemId);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<OrderItem> selectAll() throws SQLException {
        String sql = "SELECT i.* FROM t_order o, t_order_item i WHERE o.order_id = i.order_id";
        return getOrderItems(sql);
    }

    protected List<OrderItem> getOrderItems(final String sql) throws SQLException {
        List<OrderItem> result = new LinkedList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderItemId(resultSet.getLong(1));
                orderItem.setOrderId(resultSet.getLong(2));
                orderItem.setUserId(resultSet.getInt(3));
                orderItem.setStatus(resultSet.getString(4));
                result.add(orderItem);
            }
        }
        return result;
    }
}

```

AddressMapper.java

```java
package com.mashibing.shardingsphere_demo.mapper;


import com.mashibing.shardingsphere_demo.bean.Address;

public interface AddressMapper extends CommonMapper<Address,Long> {

}
```

AddressMapperImpl.java

```java
package com.mashibing.shardingsphere_demo.mapper;

import com.mashibing.shardingsphere_demo.bean.Address;

import javax.sql.DataSource;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class AddressMapperImpl implements AddressMapper{

    private DataSource dataSource;

    public AddressMapperImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS t_address "
                + "(address_id BIGINT NOT NULL, address_name VARCHAR(100) NOT NULL, PRIMARY KEY (address_id))";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public void dropTable() throws SQLException {
        String sql = "DROP TABLE t_address";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public void truncateTable() throws SQLException {
        String sql = "TRUNCATE TABLE t_address";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public Long insert(final Address entity) throws SQLException {
        String sql = "INSERT INTO t_address (address_id, address_name) VALUES (?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, entity.getAddressId());
            preparedStatement.setString(2, entity.getAddressName());
            preparedStatement.executeUpdate();
        }
        return entity.getAddressId();
    }

    @Override
    public void delete(final Long primaryKey) throws SQLException {
        String sql = "DELETE FROM t_address WHERE address_id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, primaryKey);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<Address> selectAll() throws SQLException {
        String sql = "SELECT * FROM t_address";
        return getAddress(sql);
    }

    private List<Address> getAddress(final String sql) throws SQLException {
        List<Address> result = new LinkedList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Address address = new Address();
                address.setAddressId(resultSet.getLong(1));
                address.setAddressName(resultSet.getString(2));
                result.add(address);
            }
        }
        return result;
    }
}

```

ExampleService.java

```java
package com.mashibing.shardingsphere_demo.service;

import java.sql.SQLException;

public interface ExampleService {

    /**
     * Initialize environment.
     *
     * @throws SQLException SQL exception
     */
    void initEnvironment() throws SQLException;

    /**
     * Clean environment.
     *
     * @throws SQLException SQL exception
     */
    void cleanEnvironment() throws SQLException;

    /**
     * Process success.
     *
     * @throws SQLException SQL exception
     */
    void processSuccess() throws SQLException;

    /**
     * Process failure.
     *
     * @throws SQLException SQL exception
     */
    void processFailure() throws SQLException;

    /**
     * Print data.
     *
     * @throws SQLException SQL exception
     */
    void printData() throws SQLException;
}

```

OrderServiceImpl.java

```java
package com.mashibing.shardingsphere_demo.service;

import com.mashibing.shardingsphere_demo.bean.Address;
import com.mashibing.shardingsphere_demo.bean.Order;
import com.mashibing.shardingsphere_demo.bean.OrderItem;
import com.mashibing.shardingsphere_demo.mapper.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderServiceImpl implements ExampleService{
    private OrderMapperImpl orderMapperImpl;

    private OrderItemMapperImpl orderItemMapperImpl;

    private AddressMapperImpl addressMapperImpl;

    public OrderServiceImpl(final DataSource dataSource) {
        this.orderMapperImpl = new OrderMapperImpl(dataSource);
        this.orderItemMapperImpl = new OrderItemMapperImpl(dataSource);
        this.addressMapperImpl = new AddressMapperImpl(dataSource);
    }

    public OrderServiceImpl( OrderMapperImpl orderMapperImpl,  OrderItemMapperImpl orderItemMapperImpl,  AddressMapperImpl addressMapperImpl) {
        this.orderMapperImpl = orderMapperImpl;
        this.orderItemMapperImpl = orderItemMapperImpl;
        this.addressMapperImpl = addressMapperImpl;
    }

    @Override
    public void initEnvironment() throws SQLException {
        orderMapperImpl.createTableIfNotExists();
        orderItemMapperImpl.createTableIfNotExists();
        orderMapperImpl.truncateTable();
        orderItemMapperImpl.truncateTable();
        initAddressTable();
    }

    private void initAddressTable() throws SQLException {
        addressMapperImpl.createTableIfNotExists();
        addressMapperImpl.truncateTable();
        initAddressData();
    }

    private void initAddressData() throws SQLException {
        for (int i = 0; i < 10; i++) {
            insertAddress(i);
        }
    }

    private void insertAddress(final int i) throws SQLException {
        Address address = new Address();
        address.setAddressId((long) i);
        address.setAddressName("address_" + i);
        addressMapperImpl.insert(address);
    }

    @Override
    public void cleanEnvironment() throws SQLException {
        orderMapperImpl.dropTable();
        orderItemMapperImpl.dropTable();
        addressMapperImpl.dropTable();
    }

    @Override
    public void processSuccess() throws SQLException {
        System.out.println("-------------- Process Success Begin ---------------");
        List<Long> orderIds = insertData();
        printData();
//        deleteData(orderIds);
        printData();
        System.out.println("-------------- Process Success Finish --------------");
    }

    @Override
    public void processFailure() throws SQLException {
        System.out.println("-------------- Process Failure Begin ---------------");
        insertData();
        System.out.println("-------------- Process Failure Finish --------------");
        throw new RuntimeException("Exception occur for transaction test.");
    }

    private List<Long> insertData() throws SQLException {
        System.out.println("---------------------------- Insert Data ----------------------------");
        List<Long> result = new ArrayList<>(10);
        for (int i = 1; i <= 10; i++) {
            Order order = insertOrder(i);
            insertOrderItem(i, order);
            result.add(order.getOrderId());
        }
        return result;
    }

    private Order insertOrder(final int i) throws SQLException {
        Order order = new Order();
        order.setUserId(i);
        order.setAddressId(i);
        order.setStatus("INSERT_TEST");
        orderMapperImpl.insert(order);
        return order;
    }

    private void insertOrderItem(final int i, final Order order) throws SQLException {
        OrderItem item = new OrderItem();
        item.setOrderId(order.getOrderId());
        item.setUserId(i);
        item.setStatus("INSERT_TEST");
        orderItemMapperImpl.insert(item);
    }

    private void deleteData(final List<Long> orderIds) throws SQLException {
        System.out.println("---------------------------- Delete Data ----------------------------");
        for (Long each : orderIds) {
            orderMapperImpl.delete(each);
            orderItemMapperImpl.delete(each);
        }
    }

    @Override
    public void printData() throws SQLException {
        System.out.println("---------------------------- Print Order Data -----------------------");
        for (Object each : orderMapperImpl.selectAll()) {
            System.out.println(each);
        }
        System.out.println("---------------------------- Print OrderItem Data -------------------");
        for (Object each : orderItemMapperImpl.selectAll()) {
            System.out.println(each);
        }
    }
}

```

hint-databases-only.yaml

```yaml
dataSources:
  ds_0: com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.85.113:3306/sharding_sphere_0
    username: root
    password: 123456
  ds_1: com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.85.113:3306/sharding_sphere_1
    username: root
    password: 123456

shardingRule:
  tables:
    t_order: 
      actualDataNodes: ds_${0..1}.t_order
    t_order_item:
      actualDataNodes: ds_${0..1}.t_order_item
  bindingTables:
    - t_order,t_order_item
  broadcastTables:
    - t_address
  
  defaultDatabaseStrategy:
    hint:
      algorithmClassName: com.mashibing.shardingsphere_demo.hint.MyHintAlgorithm
  defaultTableStrategy:
    none:

props:
  sql.show: true

```

hint-databases-tables.yaml

```yaml
dataSources:
  ds_0: !!com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.jdbc.Driver
    url: jdbc:mysql://192.168.85.113:3306/sharding_sphere_0
    username: root
    password: 123456
  ds_1: !!com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.jdbc.Driver
    url: jdbc:mysql://192.168.85.113:3306/sharding_sphere_1
    username: root
    password: 123456

shardingRule:
  tables:
    t_order: 
      actualDataNodes: ds_${0..1}.t_order_${0..1}
      databaseStrategy:
        hint:
          algorithmClassName: com.mashibing.shardingsphere_demo.hint.MyHintAlgorithm
      tableStrategy:
        hint:
          algorithmClassName: com.mashibing.shardingsphere_demo.hint.MyHintAlgorithm
      keyGenerator:
        type: SNOWFLAKE
        column: order_id
        props:
          worker.id: 123
    t_order_item:
      actualDataNodes: ds_${0..1}.t_order_item_${0..1}
  bindingTables:
    - t_order,t_order_item
  broadcastTables:
    - t_address

  defaultDatabaseStrategy:
    inline:
      shardingColumn: user_id
      algorithmExpression: ds_${user_id % 2}
  defaultTableStrategy:
    inline:
      shardingColumn: order_id
      algorithmExpression: t_order_item_${order_id % 2}

props:
  sql.show: true

```

hint-master-only.yaml

```yaml
dataSources:
  ds_master: com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.85.113:3306/sharding_sphere_0
    username: root
    password: 123456
  ds_slave_0: com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.85.113:3306/sharding_sphere_1
    username: root
    password: 123456

masterSlaveRule:
  name: ds_ms
  masterDataSourceName: ds_master
  slaveDataSourceNames: [ds_slave_0]

props:
  sql.show: true

```

### 8、sharding-jdbc编排治理

​		编排治理模块提供配置中心/注册中心(以及规划中的元数据中心)、配置动态化、数据库熔断禁用、调用链路等治理能力。

#### 1、配置中心的实现动机：

​		1、配置集中化：越来越多的运行时实例，使得散落的配置难于管理，配置不同步导致的问题十分严重。将配置集中于配置中心，可以更加有效进行管理。

​		2、配置动态化：配置修改后的分发，是配置中心可以提供的另一个重要能力。它可支持数据源、表与分片及读写分离策略的动态切换。

#### 2、注册中心的实现动机：

​		1、相对于配置中心管理配置数据，注册中心存放运行时的动态/临时状态数据，比如可用的proxy的实例，需要禁用或熔断的datasource实例。

​		2、通过注册中心，可以提供熔断数据库访问程序对数据库的访问和禁用从库的访问的编排治理能力。治理仍然有大量未完成的功能（比如流控等）。

#### 3、支持的配置中心/注册中心

#### SPI

[Service Provider Interface (SPI)](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html)是一种为了被第三方实现或扩展的API。它可以用于实现框架扩展或组件替换。

ShardingSphere在数据库治理模块使用SPI方式载入数据到配置中心/注册中心，进行实例熔断和数据库禁用。 目前，ShardingSphere内部支持Zookeeper和etcd这种常用的配置中心/注册中心。 此外，您可以使用其他第三方配置中心/注册中心，并通过SPI的方式注入到ShardingSphere，从而使用该配置中心/注册中心，实现数据库治理功能。

#### Zookeeper

ShardingSphere官方使用[Apache Curator](http://curator.apache.org/)作为Zookeeper的实现方案（支持配置中心和注册中心）。 请使用Zookeeper 3.4.6及其以上版本，详情请参见[官方网站](https://zookeeper.apache.org/)。

#### Etcd

ShardingSphere官方使用[io.etcd/jetcd](https://github.com/etcd-io/jetcd)作为Etcd的实现方案（支持配置中心和注册中心）。 请使用Etcd v3以上版本，详情请参见[官方网站](https://etcd.io/)。

#### Apollo

ShardingSphere官方使用[Apollo Client](https://github.com/ctripcorp/apollo)作为Apollo的实现方案（支持配置中心）。 请使用Apollo Client 1.5.0及其以上版本，详情请参见[官方网站](https://github.com/ctripcorp/apollo)。

#### Nacos

ShardingSphere官方使用[Nacos Client](https://nacos.io/zh-cn/docs/sdk.html)作为Nacos的实现方案（支持配置中心）。 请使用Nacos Client 1.0.0及其以上版本，详情请参见[官方网站](https://nacos.io/zh-cn/docs/sdk.html)。

#### 其他

使用SPI方式自行实现相关逻辑编码。

### 