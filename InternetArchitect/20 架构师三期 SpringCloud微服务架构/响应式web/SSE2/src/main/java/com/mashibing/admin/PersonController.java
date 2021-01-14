package com.mashibing.admin;

import java.util.Random;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

import com.mashibing.admin.pojo.Person;
import com.mashibing.admin.service.PersonService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// 注解式
// 函数式
@RestController
@RequestMapping("/person")
public class PersonController {

	@Autowired
	PersonService personSrv;
	
	
	@GetMapping("")
	Mono<Object> get(String name){
		
		
		System.out.println("线程 get" + Thread.currentThread().getName());
	System.out.println("---1");
		
		// 异步
	Mono<Object> mono = Mono.create(sink -> {
		
		// 组装数据序列
		System.out.println("线程 create" + Thread.currentThread().getName());
			sink.success(personSrv.getPerson());
		})
			
			
		.doOnSubscribe(sub -> {
			
		// 1 订阅	
			System.out.println("xxx");
		})
		
		.doOnNext(data -> {
			
			// 得到数据
			
			System.out.println("data:" + data);
		})
		
		.doOnSuccess(onSuccess -> {
			
			// 整体完成
			System.out.println("onSuccess");
		});
			
	
	System.out.println("---2");
	 
	// SpringMvc  值 在这个环节准备好
	// 得到一个包装 数据序列 -> 包含特征  -> 容器 拿到这个序列 -> 执行序列里的方法
	
	// Ajax   a()  -> b(c()) ->
	
	// 1,  写回调接口 ， 让b调
	// 2,  直接传方法过去
	
	
	// 看起来 像是异步，实质上，阻塞的过程 在容器内部
	return mono;
	
	}
	
	
	
	
	@GetMapping("xxoo")
	// ServerHttpRequest webFlux 中特有
	// 拓展思维，SpringCloud Gateway
	Mono<Object> get2(ServerHttpRequest request,String name
			,
			WebSession session
			){
		
		//System.out.println("name:" + name);
		
		if(StringUtils.isEmpty(session.getAttribute("code"))) {
			
			System.out.println("我要set了~");
			session.getAttributes().put("code", 250);
		}
		
		System.out.println("code = " + session.getAttribute("code"));
		
		return Mono.just("么么哒");
	}
	
	
	@GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> sse(){
		
		
		
		// 1. 封装对象
		Flux<String> flux = Flux.fromStream(IntStream.range(1, 10).mapToObj(i -> {
			
			try {
				Thread.sleep(new Random().nextInt(3000));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "xxoo" + i;
		}))
				
		.doOnSubscribe(sub -> {
			
			System.out.println("sub 了");
			
		})	
		
		.doOnComplete(() -> {
			
			System.out.println("doOnComplete");
			
		})
		
		.doOnNext(data -> {
			
			System.out.println("有data了~" + data);
		})
				
				;
		
		// 2. 对象 连带里面的方法 给了容器
		return flux;
		
	}
	
	
	
	
}
