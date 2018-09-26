package com.mason;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
public class FluxStaticMethodExamples {

	static final String[] TEST_ARRAY = new String[]{"a", "b", "c"};
	static final String[] TEST_ARRAY2 = new String[]{"A", "B", "C"};

	static final String[] TEST_ARRAY3 = new String[]{"1", "C", "2"};

	static final String DELAYED_ERROR_MESSAGE = "Delayed error";

	/**
	 * 각각의 flux에서 가장 마지막으로 publish된 값들의 combinator 결과를 flux로 생성
	 */
	@Test
	public void combineLatest() {
		Flux a = Flux.fromArray(TEST_ARRAY);

		Flux b = Flux.create(o -> {
			o.next("a");
			sleep(1);
			o.next("b");
			sleep(1);
			o.next("c");
			sleep(1);
			o.complete();
		});

		Flux c = Flux.combineLatest(a, b, (o, p) -> o.toString() + p.toString());

		StepVerifier
				.create(c)
				.expectNext("ca")
				.expectNext("cb")
				.expectNext("cc")
				.verifyComplete();
	}

	/**
	 * List<Flux<T>> 형태를 Flux<T>로 생성
	 */
	@Test
	public void concat() {
		List l = Arrays.asList(Flux.fromArray(TEST_ARRAY), Flux.fromArray(TEST_ARRAY2));
		Flux a = Flux.concat(l);

		StepVerifier
				.create(a)
				.expectNext("a")
				.expectNext("b")
				.expectNext("c")
				.expectNext("A")
				.expectNext("B")
				.expectNext("C")
				.verifyComplete();
	}

	/**
	 * 여러 Publisher들로 하나의 Flux 생성, subscribe 도중 발생한 에러는 나머지가 concat된 후 까지 연기된다.
	 * @throws InterruptedException
	 */
	@Test
	public void concatDelayError() throws InterruptedException {

		Flux a = Flux.fromArray(TEST_ARRAY);

		Flux b = Flux.create(o -> {
			o.next("error");
			o.error(new Throwable(DELAYED_ERROR_MESSAGE));
		});

		Flux c = Flux.fromArray(TEST_ARRAY2);

		Flux d = Flux.concatDelayError(a, b, c);

		Flux e = Flux.concat(a, b, c);

		StepVerifier
				.create(d)
				// Flux a (TEST_ARRAY)
				.expectNext("a")
				.expectNext("b")
				.expectNext("c")
				// Flux b's next()
				.expectNext("error")
				// Flux a (TEST_ARRAY2)
				.expectNext("A")
				.expectNext("B")
				.expectNext("C")
				// Flux b's delayed error
				.expectErrorMessage(DELAYED_ERROR_MESSAGE)
				.verify();

		StepVerifier
				.create(e)
				// Flux a (TEST_ARRAY)
				.expectNext("a")
				.expectNext("b")
				.expectNext("c")
				// Flux b's next()
				.expectNext("error")
				.expectErrorMessage(DELAYED_ERROR_MESSAGE)
				.verify();
	}

	public void sleep(int sec) {
		try {
			TimeUnit.SECONDS.sleep(sec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
