# SpringCloud Alibaba 01 Sentinel 

## 官网

https://spring.io/projects/spring-cloud-alibaba

## Features

### Spring Cloud

- **Flow control and service degradation**：flow control, circuit breaking and system adaptive protection with [Alibaba Sentinel](https://github.com/alibaba/Sentinel/)
- **Service registration and discovery**：instances can be registered with [Alibaba Nacos](https://github.com/alibaba/nacos/) and clients can discover the instances using Spring-managed beans. Supports Ribbon, the client side load-balancer via Spring Cloud Netflix
- **Distributed Configuration**：using [Alibaba Nacos](https://github.com/alibaba/nacos/) as a data store
- **Event-driven**：building highly scalable event-driven microservices connected with Spring Cloud Stream [RocketMQ](https://rocketmq.apache.org/) Binder
- **Message Bus**: link nodes of a distributed system with Spring Cloud Bus RocketMQ
- **Distributed Transaction**：support for distributed transaction solution with high performance and ease of use with [Seata](https://github.com/seata/seata)
- **Dubbo RPC**：extend the communication protocols of Spring Cloud service-to-service calls by [Apache Dubbo RPC](https://dubbo.apache.org/en-us/)

## Sentinel 分布式系统的流量防卫兵

![Sentinel Logo](img/43697219-3cb4ef3a-9975-11e8-9a9c-73f4f537442d.png)

### Sentinel 是什么？

随着微服务的流行，服务和服务之间的稳定性变得越来越重要。Sentinel 以流量为切入点，从流量控制、熔断降级、系统负载保护等多个维度保护服务的稳定性。

Sentinel 具有以下特征:

- **丰富的应用场景**：Sentinel 承接了阿里巴巴近 10 年的双十一大促流量的核心场景，例如秒杀（即突发流量控制在系统容量可以承受的范围）、消息削峰填谷、集群流量控制、实时熔断下游不可用应用等。
- **完备的实时监控**：Sentinel 同时提供实时的监控功能。您可以在控制台中看到接入应用的单台机器秒级数据，甚至 500 台以下规模的集群的汇总运行情况。
- **广泛的开源生态**：Sentinel 提供开箱即用的与其它开源框架/库的整合模块，例如与 Spring Cloud、Dubbo、gRPC 的整合。您只需要引入相应的依赖并进行简单的配置即可快速地接入 Sentinel。
- **完善的 SPI 扩展点**：Sentinel 提供简单易用、完善的 SPI 扩展接口。您可以通过实现扩展接口来快速地定制逻辑。例如定制规则管理、适配动态数据源等。





Sentinel 的主要特性：

![Sentinel-features-overview](https://user-images.githubusercontent.com/9434884/50505538-2c484880-0aaf-11e9-9ffc-cbaaef20be2b.png)

Sentinel 的开源生态：

![Sentinel-opensource-eco](https://user-images.githubusercontent.com/9434884/84338449-a9497e00-abce-11ea-8c6a-473fe477b9a1.png)



Sentinel 分为两个部分:

- 核心库（Java 客户端）不依赖任何框架/库，能够运行于所有 Java 运行时环境，同时对 Dubbo / Spring Cloud 等框架也有较好的支持。
- 控制台（Dashboard）基于 Spring Boot 开发，打包后可以直接运行，不需要额外的 Tomcat 等应用容器。







### Sentinel 与 Hystrix 的对比

|                | Sentinel                                       | Hystrix                       |
| :------------- | :--------------------------------------------- | :---------------------------- |
| 隔离策略       | 信号量隔离                                     | 线程池隔离/信号量隔离         |
| 熔断降级策略   | 基于响应时间或失败比率                         | 基于失败比率                  |
| 实时指标实现   | 滑动窗口                                       | 滑动窗口（基于 RxJava）       |
| 规则配置       | 支持多种数据源                                 | 支持多种数据源                |
| 扩展性         | 多个 SPI 扩展点                                | 插件的形式                    |
| 基于注解的支持 | 支持                                           | 支持                          |
| 限流           | 基于 QPS，支持基于调用关系的限流               | 有限的支持                    |
| 流量整形       | 支持慢启动、匀速器模式                         | 不支持                        |
| 系统负载保护   | 支持                                           | 不支持                        |
| 控制台         | 开箱即用，可配置规则、查看秒级监控、机器发现等 | 不完善                        |
| 常见框架的适配 | Servlet、Spring Cloud、Dubbo、gRPC 等          | Servlet、Spring Cloud Netflix |

### Quick Start



依赖

```
		<dependency>
			<groupId>com.alibaba.cloud</groupId>
			<artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
		</dependency>
```



```
public class SentinelTest {

	public static void main(String[] args) {
		initFlowRules();
		int i = 100;
	    while (i != 0) {
	        Entry entry = null;
	        i--;
	        try {
		    entry = SphU.entry("HelloWorld");
	            /*您的业务逻辑 - 开始*/
	            System.out.println("hello world");
	            /*您的业务逻辑 - 结束*/
		} catch (BlockException e1) {
	            /*流控逻辑处理 - 开始*/
		    System.out.println("block!");
	            /*流控逻辑处理 - 结束*/
		} finally {
		   if (entry != null) {
		       entry.exit();
		   }
		}
	    }
	}
	
	private static void initFlowRules(){
	    List<FlowRule> rules = new ArrayList<>();
	    FlowRule rule = new FlowRule();
	    rule.setResource("HelloWorld");
	    rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
	    // Set limit QPS to 20.
	    rule.setCount(20);
	    rules.add(rule);
	    FlowRuleManager.loadRules(rules);
	}
}
```

### 整合web应用

##### 启动类

```
public class StnApplication {

	public static void main(String[] args) {
		  init();
		SpringApplication.run(StnApplication.class, args);
	}
	
	
    private static void init(){
        // 所有限流规则的合集
        List<FlowRule> rules = new ArrayList<>();

        FlowRule rule = new FlowRule();
        // 资源名称
        rule.setResource("PersonService.getBody");
        // 限流的类型
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 2 qps
        rule.setCount(2);

        rules.add(rule);

        FlowRuleManager.loadRules(rules);

    }
```

##### service

```
	@SentinelResource(value = "PersonService.getBody",blockHandler = "fail")
	public String getBody() {
		// TODO Auto-generated method stub
		return "meme";
	}
	
	    public String fail(BlockException e){
        System.out.println("阻塞");
        return "fail";
    }
```







## Sentinel Dashboard

Sentinel 提供一个轻量级的开源控制台，它提供机器发现以及健康情况管理、监控（单机和集群），规则管理和推送的功能。另外，鉴权在生产环境中也必不可少。这里，我们将会详细讲述如何通过[简单的步骤](https://github.com/alibaba/Sentinel/wiki/控制台#2-启动控制台)就可以使用这些功能。

接下来，我们将会逐一介绍如何整合 Sentinel 核心库和 Dashboard，让它发挥最大的作用。同时我们也在阿里云上提供企业级的控制台：[AHAS Sentinel 控制台](https://github.com/alibaba/Sentinel/wiki/AHAS-Sentinel-控制台)，您只需要几个简单的步骤，就能最直观地看到控制台如何实现这些功能。

Sentinel 控制台包含如下功能:

- [**查看机器列表以及健康情况**](https://github.com/alibaba/Sentinel/wiki/控制台#4-查看机器列表以及健康情况)：收集 Sentinel 客户端发送的心跳包，用于判断机器是否在线。
- [**监控 (单机和集群聚合)**](https://github.com/alibaba/Sentinel/wiki/控制台#5-监控)：通过 Sentinel 客户端暴露的监控 API，定期拉取并且聚合应用监控信息，最终可以实现秒级的实时监控。
- [**规则管理和推送**](https://github.com/alibaba/Sentinel/wiki/控制台#6-规则管理及推送)：统一管理推送规则。
- [**鉴权**](https://github.com/alibaba/Sentinel/wiki/控制台#鉴权)：生产环境中鉴权非常重要。这里每个开发者需要根据自己的实际情况进行定制。

> 注意：Sentinel 控制台目前仅支持单机部署。





#### 下载

https://github.com/alibaba/Sentinel/releases



#### 启动

> **注意**：启动 Sentinel 控制台需要 JDK 版本为 1.8 及以上版本。

使用如下命令启动控制台：

```
java -Dserver.port=8080 -Dcsp.sentinel.dashboard.server=localhost:8080 -Dproject.name=sentinel-dashboard -jar sentinel-dashboard.jar
```

其中 `-Dserver.port=8080` 用于指定 Sentinel 控制台端口为 `8080`。

从 Sentinel 1.6.0 起，Sentinel 控制台引入基本的**登录**功能，默认用户名和密码都是 `sentinel`。可以参考 [鉴权模块文档](https://github.com/alibaba/Sentinel/wiki/控制台#鉴权) 配置用户名和密码。

## sentinel 集成  Nacos

### springboot配置

```
spring.cloud.sentinel.datasource.ds.nacos.server-addr=localhost:8848
spring.cloud.sentinel.datasource.ds.nacos.dataId=stn3-sentinel
spring.cloud.sentinel.datasource.ds.nacos.groupId=DEFAULT_GROUP

spring.cloud.sentinel.datasource.ds.nacos.ruleType=flow
```



### Nacos添加规则

```
[
    {
        "resource": "/hello",
        "limitApp": "default",
        "grade": 1,
        "count": 2,
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    }
]
```

