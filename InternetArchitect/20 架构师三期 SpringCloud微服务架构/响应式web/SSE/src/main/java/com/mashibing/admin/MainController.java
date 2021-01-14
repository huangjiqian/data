package com.mashibing.admin;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;


@RestController
public class MainController {

	
	@RequestMapping(value = "/sse",produces = "text/event-stream;charset=utf-8")
	public Object xxoo(HttpServletRequest r) {
		System.out.println("来啦 老弟！" + Thread.currentThread().getName());
		
		Date date = new Date();
		return "data:" + date.getTime() +  " \n\n";
	}
	
	
	@GetMapping("/1")
	public String get() {
		
		System.out.println("----1");
		// Service
		String result = getResult();
		System.out.println("----2");
		return result;
		
	}

	
	
	@GetMapping("/2")
	public Mono<String> get2() {
		
		System.out.println("----1");
		// Service
		Mono<String> result = Mono.create(sink -> getResult());
		System.out.println("----2");
		return result;
		
	}

	private String getResult() {


		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "xxoo";
	}
}
