package com.mashibing.admin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;

public class ReactorTest {
	
	public static void main(String[] args) {


		Flux.create(sink -> {
			
			for (int i = 0; i < 1000; i++) {
				sink.next("xxoo:" + i);
			}
			
			sink.complete();
			
			
		}).subscribe(System.out::println);
	}
	    
	    
}
