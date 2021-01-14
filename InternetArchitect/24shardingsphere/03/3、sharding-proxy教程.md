# 3、sharding-proxy教程

​		Sharding-Proxy是ShardingSphere的第二个产品。 它定位为透明化的数据库代理端，提供封装了数据库二进制协议的服务端版本，用于完成对异构语言的支持。 目前先提供MySQL/PostgreSQL版本，它可以使用任何兼容MySQL/PostgreSQL协议的访问客户端(如：MySQL Command Client, MySQL Workbench, Navicat等)操作数据，对DBA更加友好。

- 向应用程序完全透明，可直接当做MySQL/PostgreSQL使用。
- 适用于任何兼容MySQL/PostgreSQL协议的的客户端。

![](F:\lian\sharding-sphere\images\sharding-proxy.png)**对比**

| * Sharding-JDBC* | *Sharding-Proxy* | *Sharding-Sidecar* |                  |
| :--------------- | :--------------- | :----------------- | ---------------- |
| 数据库           | 任意             | `MySQL/PostgreSQL` | MySQL/PostgreSQL |
| 连接消耗数       | 高               | `低`               | 高               |
| 异构语言         | 仅Java           | `任意`             | 任意             |
| 性能             | 损耗低           | `损耗略高`         | 损耗低           |
| 无中心化         | 是               | `否`               | 是               |
| 静态入口         | 无               | `有`               | 无               |

### 1、sharding-proxy安装

#### 1、下载sharding-proxy的安装包

https://shardingsphere.apache.org/document/legacy/4.x/document/cn/downloads/

![image-20200830155513517](F:\lian\sharding-sphere\images\sharding-proxy下载.png)

#### 2、解压到linux的指定目录

### 2、sharding-proxy实现分表

#### 1、进入到conf目录，修改配置文件server.yaml,修改相关的系统配置

```yaml
authentication:
  users:
    root:
      password: root
    sharding:
      password: 123456 
      authorizedSchemas: sharding_db

props:
  max.connections.size.per.query: 1
  acceptor.size: 16  # The default value is available processors count * 2.
  executor.size: 16  # Infinite by default.
  proxy.frontend.flush.threshold: 128  # The default value is 128.
    # LOCAL: Proxy will run with LOCAL transaction.
    # XA: Proxy will run with XA transaction.
    # BASE: Proxy will run with B.A.S.E transaction.
  proxy.transaction.type: LOCAL
  proxy.opentracing.enabled: false
  proxy.hint.enabled: false
  query.with.cipher.column: true
  sql.show: true
  allow.range.query.with.inline.sharding: true

```

#### 2、复制mysql的驱动包到lib目录下

#### 3、配置分库分表的规则

config-sharding.yaml

```yaml
schemaName: sharding_db

dataSources:
  ds_0:
    url: jdbc:mysql://192.168.85.111:3306/sharding_haha?serverTimezone=UTC&useSSL=false
    username: root
    password: 123456
    connectionTimeoutMilliseconds: 30000
    idleTimeoutMilliseconds: 60000
    maxLifetimeMilliseconds: 1800000
    maxPoolSize: 50

shardingRule:
  tables:
    t_order:
      actualDataNodes: ds_0.t_order_${0..1}
      tableStrategy:
        inline:
          shardingColumn: order_id
          algorithmExpression: t_order_${order_id % 2}
      keyGenerator:
        type: SNOWFLAKE
        column: order_id
  bindingTables:
    - t_order
  defaultDatabaseStrategy:
    inline:
      shardingColumn: user_id
      algorithmExpression: ds_0
  defaultTableStrategy:
    none:

```

#### 4、执行相关的命令

```sql
--查看数据库，只有一个
show databases;
--切换库
use sharding_db；
--创建表，插入完成之后，在代理的连接中还是只有一个表，但是实际的物理节点有两个表
create table if not exists ds_0.t_order(order_id bigint not null,user_id int not null,status varchar(50),primary key(order_id));
--插入数据,会看到根据分片规则插入到具体的表中
insert into t_order(order_id,user_id,status) values(1,1,'proxy');
```

### 3、sharding-proxy实现分库

#### 1、修改config-sharding.yaml配置文件

```yaml
schemaName: sharding_db

dataSources:
  ds_0:
    url: jdbc:mysql://192.168.85.111:3306/sharding_haha?serverTimezone=UTC&useSSL=false
    username: root
    password: 123456
    connectionTimeoutMilliseconds: 30000
    idleTimeoutMilliseconds: 60000
    maxLifetimeMilliseconds: 1800000
    maxPoolSize: 50
  ds_1:
    url: jdbc:mysql://192.168.85.111:3306/sharding_haha2?serverTimezone=UTC&useSSL=false
    username: root
    password: 123456
    connectionTimeoutMilliseconds: 30000
    idleTimeoutMilliseconds: 60000
    maxLifetimeMilliseconds: 1800000
    maxPoolSize: 50

shardingRule:
  tables:
    t_order:
      actualDataNodes: ds_$->{0..1}.t_order_${0..1}
      tableStrategy:
        inline:
          shardingColumn: order_id
          algorithmExpression: t_order_${order_id % 2}
      keyGenerator:
        type: SNOWFLAKE
        column: order_id
  bindingTables:
    - t_order
  defaultDatabaseStrategy:
    inline:
      shardingColumn: user_id
      algorithmExpression: ds_$->{user_id % 2}
  defaultTableStrategy:
    none:

```

#### 2、重新启动服务并测试

```sql
create table if not exists t_order(order_id bigint not null,user_id int not null,status varchar(50),primary key(order_id));
insert into t_order(order_id,user_id,status) values(1,1,'proxy');
```

### 4、sharding-proxy实现读写分离

#### 1、修改配置文件

```yaml
schemaName:master_slave_db
  dataSource:
    master_ds:
      url: jdbc:mysql://192.168.85.111:3306/master?serverTimezone=UTC&useSSL=false
      username: root
      password: 123456
      connectionTimeoutMilliseconds: 30000
      idleTimeoutMilliseconds: 60000
      maxLifetimeMilliseconds: 1800000
      maxPoolSize: 50
    slave_ds_0:
      url: jdbc:mysql://192.168.85.111:3306/slave1?serverTimezone=UTC&useSSL=false
      username: root
      password: 123456
      connectionTimeoutMilliseconds: 30000
      idleTimeoutMilliseconds: 60000
      maxLifetimeMilliseconds: 1800000
      maxPoolSize: 50
    slave_ds_1:
      url: jdbc:mysql://192.168.85.111:3306/slave2?serverTimezone=UTC&useSSL=false
      username: root
      password: 123456
      connectionTimeoutMilliseconds: 30000
      idleTimeoutMilliseconds: 60000
      maxLifetimeMilliseconds: 1800000
      maxPoolSize: 50
masterSlaveRule:
  name:ms_ds
  masterDataSourceName:master_ds
  slaveDataSourceNames:
    - slave_ds_0
    - slave_ds_1

```

#### 2、测试

```sql
--创建表
create table if not exists master.t_order(order_id bigint not null,user_id int not null,status varchar(50),primary key(order_id));
create table if not exists slave1.t_order(order_id bigint not null,user_id int not null,status varchar(50),primary key(order_id));
create table if not exists slave2.t_order(order_id bigint not null,user_id int not null,status varchar(50),primary key(order_id));
--插入数据
insert into t_order(order_id,user_id,status) values(1,1,'proxy');
--此时可以看到效果，只在master中有数据，slave中没有数据，查询不到对应的结果

--向slave中插入不同的数据
insert into slave1.t_order(order_id,user_id,status) values(2,2,'proxy2');
insert into slave2.t_order(order_id,user_id,status) values(3,3,'proxy3');
--再次执行查询可以看到对应的结果
```

### 

