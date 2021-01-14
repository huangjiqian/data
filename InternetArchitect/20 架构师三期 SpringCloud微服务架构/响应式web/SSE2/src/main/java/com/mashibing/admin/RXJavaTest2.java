package com.mashibing.admin;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class RXJavaTest2 {
	// 异步
	
	
	public static void main(String[] args) throws InterruptedException {
	
		
		// 被观察者
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
		// 哪个线程是观察者
		.observeOn(
				Schedulers.computation()
				)
		.subscribeOn( Schedulers.computation())
		.subscribe(new Observer<String>() {

			@Override
			public void onSubscribe(Disposable d) {
				System.out.println("onSubscribe...");
			}

			@Override
			public void onNext(String t) {
				System.out.println("onNext");
			}

			@Override
			public void onError(Throwable e) {
				System.out.println("onError");
			}

			@Override
			public void onComplete() {
				System.out.println("onComplete");
			}

		})
		;
		
		
		Thread.sleep(10000);
		
	}
}
