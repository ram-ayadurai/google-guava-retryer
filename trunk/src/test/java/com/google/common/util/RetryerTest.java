package com.google.common.util;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * 
 * @author wangzijian
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class RetryerTest {

	@Mock
	private HeavySevice heavySevice;
	
	private Callable<String> heavyTask;
	
	@Before
	public void setUp() {
		heavyTask = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return heavySevice.doSometing();
			}
		};
	}
	
	@Test
	public void testSuccess() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.build();
		
		when(heavySevice.doSometing()).thenReturn("SUCCESS");
		
		assertThat(retryer.callWithRetry(heavyTask), is("SUCCESS"));
		
		HeavySevice retryableHeavySevice = retryer.newProxy(heavySevice, HeavySevice.class);
		assertThat(retryableHeavySevice.doSometing(), is("SUCCESS"));
	}
	
	@Test
	public void testSucessAfterRetry() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, MILLISECONDS)
			.whenThrow(exception())
			.build();
	
		when(heavySevice.doSometing())
			.thenThrow(new RuntimeException("Mock"))
			.thenThrow(new RuntimeException("Mock"))
			.thenReturn("SUCCESS");
		
		String actual = retryer.callWithRetry(heavyTask);
		assertThat(actual, is("SUCCESS"));
	}
	
	@Test
	public void testReuse() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(10, MILLISECONDS)
			.whenThrow(exception())
			.build();

		for (int i = 0 ; i < 10 ; i ++) {
			when(heavySevice.doSometing())
				.thenThrow(new RuntimeException("Mock"))
				.thenThrow(new RuntimeException("Mock"))
				.thenReturn("SUCCESS");
		
			String actual = retryer.callWithRetry(heavyTask);
			assertThat(actual, is("SUCCESS"));
			
			reset(heavySevice);
		}
	}
	
	@Test
	public void testSucessAfterRetry2() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, MILLISECONDS)
			.whenThrow(exception())
			.build();
		
		when(heavySevice.doSometing())
			.thenThrow(new RuntimeException("Mock"))
			.thenThrow(new RuntimeException("Mock"))
			.thenReturn("SUCCESS");
		
		HeavySevice retryableHeavySevice = retryer.newProxy(heavySevice, HeavySevice.class);
		assertThat(retryableHeavySevice.doSometing(), is("SUCCESS"));
	}
	
	@Test
	public void testSucessAfterRetry3() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, MILLISECONDS)
			.whenReturn(new Predicate<Object>() {
				@Override
				public boolean apply(Object input) {
					return !input.toString().equals("SUCCESS");
				}
			})
			.build();
		
		when(heavySevice.doSometing())
			.thenReturn("FAILURE")
			.thenReturn("FAILURE")
			.thenReturn("SUCCESS");
		
		HeavySevice retryableHeavySevice = retryer.newProxy(heavySevice, HeavySevice.class);
		assertThat(retryableHeavySevice.doSometing(), is("SUCCESS"));
	}
	
	@Test(expected = RuntimeException.class)
	public void testThrowFailureAfterRetry() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, MILLISECONDS)
			.build();
		
		when(heavySevice.doSometing())
			.thenThrow(new RuntimeException("Mock"))
			.thenThrow(new RuntimeException("Mock"))
			.thenThrow(new RuntimeException("Mock"));
		
		retryer.callWithRetry(heavyTask);
		fail();
	}
	
	@Test
	public void testReturnFailureAfterRetry() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, MILLISECONDS)
			.whenReturn(new Predicate<Object>() {
				@Override
				public boolean apply(Object input) {
					if (input.toString().equals("Failure")) {
						return true;
					}
					return false;
				}
			})
			.build();
		
		when(heavySevice.doSometing())
			.thenReturn("Failure")
			.thenReturn("Failure")
			.thenReturn("Failure");
		
		String actual = retryer.callWithRetry(heavyTask);
		assertThat(actual, is("Failure"));
	}
	
	@Test(expected = RuntimeException.class)
	public void testFailureForConditionNotMatched() throws Exception {
		Predicate<Exception> alwaysFalse = Predicates.alwaysFalse();
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, MILLISECONDS)
			.whenThrow(alwaysFalse)
			.build();
		
		when(heavySevice.doSometing())
			.thenThrow(new RuntimeException("Mock"));
		
		retryer.callWithRetry(heavyTask);
		fail();
	}
	
	private Predicate<Exception> exception() {
		return Predicates.alwaysTrue();
	}
	
	private interface HeavySevice {
		String doSometing();
	}
}
