# SpringCloud Gateway 2

![img](img/3)



## 路由

### 使用webflux

```
	@Bean
	public RouterFunction<ServerResponse> function(){
		
		RouterFunction<ServerResponse> route = RouterFunctions.route(
				
				RequestPredicates.path("/002"),
				req -> ServerResponse.ok().body(BodyInserters.fromValue("xxx"))
				
				);
		
		return route;
		
		
	}
```

### 缓存

### 权重与灰度发布

![img](img/5)

#### 随机算法

```yaml
      routes:
      - id: w1
        predicates:
        - Path=/w/**
        - Weight=service,95
        uri: lb://MDB
        
        filters:
        - StripPrefix=1
        
        
      - id: w2
        predicates:
        - Path=/w/**
        - Weight=service,5
        uri: lb://MDB2
        
        filters:
        - StripPrefix=1        
```





## 过滤器

SpringCloud Gateway用于拦截用户请求和链式处理，可以实现面向切面编程，在切面中可以实现与应用无关的需求，比如安全、访问超时等

### 有序

order值越小 优先级越高

```
	@Override
	public int getOrder() {
		// TODO Auto-generated method stub
		return 110;
	}
```



## 限流

### 内置令牌桶 + Redis

pom

```
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>
```







```
      routes:
      - id: w1
        predicates:
        - Path=/w/**
        uri: lb://MDB2
        
        filters:
        - StripPrefix=1
        - name: RequestRateLimiter
          args:
            key-resolver: '#{@userKeyResolver}'
            redis-rate-limiter.replenishRate: 1
            redis-rate-limiter.burstCapacity: 3
```



```
public class RateLimitConfig {
    KeyResolver userKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getQueryParams().getFirst("user"));
    }
}

```



### 整合GoogleGuava

DefaultRateLimiter

```
package com.mashibing.admin;

import java.util.HashMap;
import java.util.Objects;

import javax.validation.constraints.DecimalMin;

import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.google.common.util.concurrent.RateLimiter;

import reactor.core.publisher.Mono;

@Component
@Primary
public class DefaultRateLimiter extends AbstractRateLimiter<DefaultRateLimiter.Config> {

    public DefaultRateLimiter() {
    	
        super(Config.class, "default-rate-limit", new ConfigurationService());
    }

	/**
     * 每秒一个请求，每秒发一个令牌
     */
    private final RateLimiter limiter = RateLimiter.create(1);



    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        Config config = getConfig().get(routeId);
        limiter.setRate(Objects.isNull(config.getPermitsPerSecond()) ? 1 : config.getPermitsPerSecond());

        boolean isAllow = limiter.tryAcquire();

        return Mono.just(new Response(isAllow, new HashMap<>()));
    }

    @Validated
    public static class Config {

        @DecimalMin("0.1")
        private Double permitsPerSecond;


        public Double getPermitsPerSecond() {
            return permitsPerSecond;
        }

        public Config setPermitsPerSecond(Double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
            return this;
        }
    }

	
	
	
	
}

```

配置

```
      routes:
      - id: w1
        predicates:
        - Path=/w/**
        uri: lb://MDB2
        
        filters:
        - StripPrefix=1
        - name: RequestRateLimiter
          args:
            rate-limiter: "#{@defaultRateLimiter}"
            key-resolver: "#{@userKeyResolver}"
            default-rate-limit.permitsPerSecond: 0.5
        
```







## 权限

写在filter中

## hystrix

项目里讲

## 生命周期

![img](img/4)

Spring Cloud Gateway同zuul类似，有“pre”和“post”两种方式的filter。客户端的请求先经过“pre”类型的filter，然后将请求转发到具体的业务服务，比如上图中的user-service，收到业务服务的响应之后，再经过“post”类型的filter处理，最后返回响应到客户端。


