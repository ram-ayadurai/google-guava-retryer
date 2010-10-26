package com.google.common.util;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
	
	@Mock
	private Response response;
	
	private Callable<Response> heavyTask;
	
	@Before
	public void setUp() {
		heavyTask = new Callable<Response>() {
			@Override
			public Response call() throws Exception {
				return heavySevice.doSometing();
			}
		};
	}
	
	@Test
	public void testSuccess() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.build();
		
		when(heavySevice.doSometing()).thenReturn(response);
		
		assertThat(retryer.callWithRetry(heavyTask), sameInstance(response));
		
		HeavySevice retryableHeavySevice = retryer.newProxy(heavySevice, HeavySevice.class);
		assertThat(retryableHeavySevice.doSometing(), sameInstance(response));
	}
	
	@Test
	public void testSucessAfterRetry() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, TimeUnit.MILLISECONDS)
			.build();
	
		when(heavySevice.doSometing())
			.thenThrow(new RuntimeException("Mock"))
			.thenThrow(new RuntimeException("Mock"))
			.thenReturn(response);
		
		Response actual = retryer.callWithRetry(heavyTask);
		assertThat(actual, sameInstance(response));
	}
	
	@Test
	public void testSucessAfterRetry2() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, TimeUnit.MILLISECONDS)
			.build();
		
		when(heavySevice.doSometing())
			.thenThrow(new RuntimeException("Mock"))
			.thenThrow(new RuntimeException("Mock"))
			.thenReturn(response);
		
		HeavySevice retryableHeavySevice = retryer.newProxy(heavySevice, HeavySevice.class);
		assertThat(retryableHeavySevice.doSometing(), sameInstance(response));
	}
	
	@Test(expected = RuntimeException.class)
	public void testFailureAfterRetry() throws Exception {
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, TimeUnit.MILLISECONDS)
			.build();
		
		when(heavySevice.doSometing())
			.thenThrow(new RuntimeException("Mock"))
			.thenThrow(new RuntimeException("Mock"))
			.thenThrow(new RuntimeException("Mock"));
		
		retryer.callWithRetry(heavyTask);
		fail();
	}
	
	@Test(expected = RuntimeException.class)
	public void testFailureForConditionNotMatched() throws Exception {
		Predicate<Exception> alwaysFalse = Predicates.alwaysFalse();
		Retryer retryer = new RetryerBuilder()
			.times(3)
			.interval(100, TimeUnit.MILLISECONDS)
			.when(alwaysFalse)
			.build();
		
		when(heavySevice.doSometing())
			.thenThrow(new RuntimeException("Mock"));
		
		retryer.callWithRetry(heavyTask);
		fail();
	}
	
	private interface Response {
	}
	
	private interface HeavySevice {
		Response doSometing();
	}
}
