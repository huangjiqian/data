package com.mashibing.admin.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.mashibing.admin.pojo.Person;

import reactor.core.publisher.Flux;

@Service
public class PersonService {
	static ConcurrentHashMap<Integer, Person> map = new ConcurrentHashMap<>();
	
	static {
		
		for (int i = 0; i < 100; i++) {
			
			Person person = new Person();
			
			person.setId(i);
			person.setName("yangchaoyue" + i);
		
			map.put(i, person);
		}
	}
	
	
	
	
	public Person getPerson() {
		// TODO Auto-generated method stub
		System.out.println("线程 getPerson" + Thread.currentThread().getName());
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map.get(1);
	}
	
	public Person getPersonMax() {
		// TODO Auto-generated method stub
		
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map.get(1);
	}
	
	public Flux<Person> getPersons(){
		// 直接给
		// 响应式数据源
		return Flux.fromIterable(map.values());
		
	}
	
	

}
