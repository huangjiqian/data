package com.mashibing.admin;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class RXJavaTest {
	
	// 同步
	
	public static void main(String[] args) {
		
		// Observable 被观察者
		
		Observable<String> girl = Observable.create(new ObservableOnSubscribe<String>() {

			
			// emitter 发射器，发射体
			@Override
			public void subscribe(ObservableEmitter<String> emitter) throws Exception {
			
				// onNext可以 无限次调用
				System.out.println(Thread.currentThread().getName());
				emitter.onNext("1");
				Thread.sleep(1000);
				System.out.println(Thread.currentThread().getName());
				emitter.onNext("2");
				Thread.sleep(1000);
				System.out.println(Thread.currentThread().getName());
				emitter.onNext("3");
				Thread.sleep(1000);
				emitter.onNext("4");
				Thread.sleep(1000);
				emitter.onNext("5");
				Thread.sleep(1000);
				emitter.onComplete();
			}
		});
	
	// Observer 观察者
		Observer<String> man = new Observer<String>() {
			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub
				System.out.println("onSubscribe" + d);
			}

			@Override
			public void onNext(String t) {
				// TODO Auto-generated method stub
				System.out.println(Thread.currentThread().getName());
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
}
