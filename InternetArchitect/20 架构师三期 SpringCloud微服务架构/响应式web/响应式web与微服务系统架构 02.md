# 响应式web与微服务系统架构 02

## Project Reactor

官网

https://projectreactor.io/

Reactor 是Spring5中构建各个响应式组件的基础框架，内部提供了Flux和Mono两个代表异步数据序列的核心组件。

### Flux

### 静态方法生成

```java
// 静态方法生成Flux
	
	
	String[] s = new String[] {"xx","oo"};
	// just 已知元素数量和内容 使用
	// 
	Flux<String> flux1 = Flux.just(s);
//	flux1.subscribe(System.out::println);

	
	Flux<String> flux2 = Flux.just("xx","xxx");
//	flux2.subscribe(System.out::println);
	
	
	
    //fromArray方法
    List<String> list = Arrays.asList("hello", "world");
    Flux<String> flux3 = Flux.fromIterable(list);
  //  flux3.subscribe(System.out::println);
	
    
    //fromStream方法
    Stream<String> stream = Stream.of("hi", "hello");
    Flux<String> flux4 = Flux.fromStream(stream);
 //   flux4.subscribe(System.out::println);
    
    
    //range方法
    Flux<Integer> range = Flux.range(0, 5);
    
 //   range.subscribe(System.out::println);
    
  //interval方法, take方法限制个数为5个
    Flux<Long> longFlux = Flux.interval(Duration.ofSeconds(1)).take(5);
    longFlux.subscribe(System.out::println);
    
    //链式
    Flux.range(1, 5).subscribe(System.out::println);
}

```





```java
    //链式
   Flux.range(1, 5).subscribe(System.out::println);
   
   
   // 合并
   Flux<String> mergeWith = flux3.mergeWith(flux4);
   mergeWith.subscribe(System.out::println);
   System.out.println("---");
   
   // 结合为元祖
   Flux<String> source1 = Flux.just("111", "world","333");
   Flux<String> source2 = Flux.just("2111", "xxx");

   Flux<Tuple2<String, String>> zip = source1.zipWith(source2);
   zip.subscribe(tuple -> {
       System.out.println(tuple.getT1() + " -> " + tuple.getT2());
   });
```



```java
	// 跳过两个
    Flux<String> flux = Flux.just("1111", "222", "333");

    Flux<String> skip = flux.skip(2);
    skip.subscribe(System.out::println);
    
    // 拿前几个
    Flux<String> flux2 = Flux.just("1111", "222", "333");
    Flux<String> skip2 = flux2.take(2);
    skip2.subscribe(System.out::println);
   

	// 过滤
    Flux<String> flux = Flux.just("xx", "oo", "x1x");

    Flux<String> filter = flux.filter(s -> s.startsWith("x"));
    filter.subscribe(System.out::println);

	// 去重
    Flux<String> flux = Flux.just("xx", "oo", "x1x","x2x");

    Flux<String> filter = flux.filter(s -> s.startsWith("x")).distinct();
    filter.subscribe(System.out::println);
    // 转 Mono
    Flux<String> flux = Flux.just("xx", "oo", "x1x","x2x");
    Mono<List<String>> mono = flux.collectList();
    
    mono.subscribe(System.out::println);


    // 逻辑运算 all 与 any
    Flux<String> flux = Flux.just("xx", "oox", "x1x","x2x");

    Mono<Boolean> mono = flux.all(s -> s.contains("x"));
    mono.subscribe(System.out::println);
```

Mono 连接

```
		Flux<String> concatWith = Mono.just("100").concatWith(Mono.just("100"));
		
		concatWith.subscribe(System.out::println);
```

异常处理

```
		Mono.just("100")
				.concatWith(Mono.error(new Exception("xx")))
				
				.onErrorReturn("xxx")
				.subscribe(System.out::println)
```



### 动态创建

```java
		// 同步动态创建，next 只能被调用一次
		Flux.generate(sink -> {

			sink.next("xx");
			sink.complete();

		}).subscribe(System.out::print);
	}
```



```
		Flux.create(sink -> {
			
			for (int i = 0; i < 10; i++) {
				sink.next("xxoo:" + i);
			}
			
			sink.complete();
			
			
		}).subscribe(System.out::println);
	}
```



## WebFlux

![img](images/1158242-20180713114621450-1330576417.png)





## RXJava2

http://reactivex.io/#

**Reactive Extensions** 

### 同步

哪个线程产生就在哪个线程消费



依赖

```
	<!-- https://mvnrepository.com/artifact/io.reactivex.rxjava2/rxjava -->
<dependency>
    <groupId>io.reactivex.rxjava2</groupId>
    <artifactId>rxjava</artifactId>
</dependency>
```



```java
	public static void main(String[] args) {
		
		Observable<String> girl = Observable.create(new ObservableOnSubscribe<String>() {

			@Override
			public void subscribe(ObservableEmitter<String> emitter) throws Exception {
				emitter.onNext("1");
				emitter.onNext("2");
				emitter.onNext("3");
				emitter.onNext("4");
				emitter.onNext("5");
				emitter.onComplete();
			}
		});
	
	// 观察者
		Observer<String> man = new Observer<String>() {
			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub
				System.out.println("onSubscribe" + d);
			}

			@Override
			public void onNext(String t) {
				// TODO Auto-generated method stub
				System.out.println("onNext " + t);
			}

			@Override
			public void onError(Throwable e) {
				// TODO Auto-generated method stub
				System.out.println("onError " + e.getMessage());
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				System.out.println("onComplete");
			}
		};
		
		girl.subscribe(man);
	}
```

### 异步

| 方法                      | 说明                               |
| ------------------------- | ---------------------------------- |
| Schedulers.computation()  | 适用于计算密集型任务               |
| Schedulers.io()           | 适用于 IO 密集型任务               |
| Schedulers.trampoline()   | 在某个调用 schedule 的线程执行     |
| Schedulers.newThread()    | 每个 Worker 对应一个新线程         |
| Schedulers.single()       | 所有 Worker 使用同一个线程执行任务 |
| Schedulers.from(Executor) | 使用 Executor 作为任务执行的线程   |



```java
	public static void main(String[] args) throws InterruptedException {
		Observable.create(new ObservableOnSubscribe<String>() {

			@Override
			public void subscribe(ObservableEmitter<String> emitter) throws Exception {
				emitter.onNext("1");
				emitter.onNext("2");
				emitter.onNext("3");
				emitter.onNext("4");
				emitter.onNext("5");
				emitter.onComplete();				
			}
		})
		.observeOn(
				Schedulers.computation()
				)
		.subscribeOn( Schedulers.computation())
		.subscribe(new Observer<String>() {

			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub
				System.out.println("onSubscribe");
			}

			@Override
			public void onNext(String t) {
				// TODO Auto-generated method stub
				System.out.println("onNext");
			}

			@Override
			public void onError(Throwable e) {
				// TODO Auto-generated method stub
				System.out.println("onError");
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				System.out.println("onComplete");
			}

		})
		;
		
		
		Thread.sleep(10000);
		
	}
```

