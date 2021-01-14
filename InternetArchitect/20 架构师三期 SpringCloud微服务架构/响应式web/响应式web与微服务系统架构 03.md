# 响应式web与微服务系统架构 03

## 响应式的Web服务

### SpringMVC注解模式

传统的SpringMVC注解与WebFlux通用，区别在于底层实现，一个是基于ServerHTTPRequest的另一个是基于HTTPServletRequest

### Router与Handler

RouterFunctions可以产生Router和Handler对象，

RouterFunctions对标@Controller中的注解

Router相当于@RequestMapping

Handler相当于Controller中的方法

#### RouterFunctions中的Router

主要起到的功能是路由匹配URI，执行Handler中的逻辑

**指定Handler**

```
return RouterFunctions.route(RequestPredicates.GET("/06").
				and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), fluxHandler::getxx)
```



**直接返回**

```
.andRoute(RequestPredicates.path("/xxoo"),request -> ServerResponse.ok().body(BodyInserters.fromValue("xx")))
```



##### 多层匹配

指定的函数如果匹配不成功，则进入下一条规则，匹配顺序按照代码顺序执行

##### **Route规则**

可以使用Path+Method 或者使用Get、Post





### ServerRequest和ServerResponse

SpringMVC中使用的是HTTPServletRequest

webFlux + SpringMVC 使用的是ServerHTTPRequest

WebFlux+ 响应式 使用的是 ServerRequest

#### ServerRequest

##### 请求方式

请求方式与数据类型绑定在Router中

```java
RouterFunctions.route(RequestPredicates.GET("/01").
				and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), fluxHandler::getxx)
```

##### 获取请求参数

**使用request.queryParams()**

```
MultiValueMap<String, String> queryParams = request.queryParams();
```

**使用占位符**

request.pathVariable

```
.andRoute(RequestPredicates.GET("/03/{name}_{id}").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), fluxHandler::getxx2)
				
```



```
	public Mono<ServerResponse> getxx2(ServerRequest request){
		
		String name = request.pathVariable("name");
		String id = request.pathVariable("id");
		System.out.println("id:" + id);
		System.out.println("name:" + name);
		return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body(BodyInserters.fromValue("xx"));
	}
```



#### ServerResponse

##### 返回JSON

```
		Person person = new Person();
		person.setId(1);
		person.setName("xx");
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(person));
```

返回404和其他

```
return ServerResponse.notFound().build();
```

