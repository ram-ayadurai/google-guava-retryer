package com.google.common.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;

/**
 * A Retry builder can build Retryer with retry policy and condition.
 * <pre>
 * Retryer retryer = new RetryerBuilder()
 * 	.times(3)
 * 	.interval(10, TimeUnit.SECONDS)
 * 	.whenThrow(expectedException())
 * 	.build();
 * </pre>
 * 
 * Retry condition:
 * <pre>
 * private static Predicate<Exception> expectedException() {
 * 	return new Predicate<Exception>() {
 * 		public boolean apply(Exception exception) {
 * 			if (exception instanceof TimeoutException) {
 * 				return exception.getMessage().startsWith("Retryable");
 * 			}
 *		}
 * 	}
 * }
 * </pre>
 * 
 * @author wangzijian
 */
public class RetryerBuilder {

	private static final Duration DEFAULT_INTERVAL = new Duration(0, TimeUnit.SECONDS);
	private static final int DEFAULT_RETRY_TIMES = 3;
	
	private Predicate<Exception> throwCondition = Predicates.alwaysTrue();
	private Predicate<Object> returnCondition = Predicates.alwaysFalse();
	private Duration interval = DEFAULT_INTERVAL;
	private int times = DEFAULT_RETRY_TIMES;

	public Retryer build() {
		return new RetryerImpl(this);
	}

	public RetryerBuilder times(int times) {
		checkArgument(times >=0, "times '" + times + "'");
		this.times = times;
		return this;
	}

	public RetryerBuilder interval(int duration, TimeUnit timeUnit) {
		checkArgument(duration >= 0, "duration '" + duration + "'");
		checkNotNull(timeUnit, "timeUnit");
		this.interval = new Duration(duration, timeUnit);
		return this;
	}

	public RetryerBuilder whenThrow(Predicate<Exception> throwCondition) {
		checkNotNull(throwCondition, "throwCondition");
		this.throwCondition = throwCondition;
		return this;
	}
	
	public RetryerBuilder whenReturn(Predicate<Object> returnCondition) {
		checkNotNull(throwCondition, "returnCondition");
		this.returnCondition = returnCondition;
		return this;
	}

	private static final class RetryerImpl implements Retryer {

		private final Duration interval;
		private final Predicate<Exception> throwCondition;
		private final Predicate<Object> returnCondition;
		private final int times;
		
		private RetryerImpl(RetryerBuilder retryerBuilder) {
			this.times = retryerBuilder.times;
			this.interval = retryerBuilder.interval;
			this.throwCondition = retryerBuilder.throwCondition;
			this.returnCondition = retryerBuilder.returnCondition;
		}

		@Override
		public final <T> T callWithRetry(Callable<T> task) throws Exception {
			return new RetryableTask<T>(task).call();
		}

		@Override
		public final <T> T newProxy(final T target, Class<T> interfaceType) {
			checkNotNull(target, "target");
			checkNotNull(interfaceType, "interfaceType");
			return newProxy(interfaceType, new RetryableInvocationHandler(target));
		}
		
		private static <T> T newProxy(Class<T> interfaceType, InvocationHandler handler) {
			Object object = Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[] { interfaceType }, handler);
			return interfaceType.cast(object);
		}
		
		private final class RetryableTask<T> implements Callable<T> {
			private final Callable<T> callable;
			private int left;
			
			public RetryableTask(Callable<T> callable) {
				this.callable = callable;
				this.left = times;
			}

			@Override
			public T call() throws Exception {
				try {
					T returnObject = callable.call();
					if (!exhausted() && returnCondition.apply(returnObject)) {
						return retry();
					}
					return returnObject;
				} catch (Exception exception) {
					if (!exhausted() && throwCondition.apply(exception)) {
						return retry();
					}
					throw exception;
				}
			}
			
			private T retry() throws Exception {
				interval.sleep();
				left--;
				return call();
			}

			private boolean exhausted() {
				return left == 0;
			}
		}
		
		private final class RetryableInvocationHandler implements InvocationHandler {
			private final Object target;

			public RetryableInvocationHandler(Object target) {
				this.target = target;
			}

			@Override
			public Object invoke(final Object obj, final Method method, final Object[] args) throws Throwable {
				Callable<Object> task = new Callable<Object>() {
					public Object call() throws Exception {
						try {
							return method.invoke(target, args);
						} catch (InvocationTargetException e) {
							throw Throwables.throwCause(e, false);
						}
					}
				};
				return callWithRetry(task);
			}
		}
	}
	
	private final static class Duration {
		private final long duration;
		private final TimeUnit timeUnit;
		
		private Duration(long duration, TimeUnit timeUnit) {
			this.duration = duration;
			this.timeUnit = timeUnit;
		}
		
		private void sleep() throws InterruptedException {
			if (duration > 0) {
				timeUnit.sleep(duration);
			}
		}

		@Override
		public String toString() {
			return new StringBuilder()
				.append(duration)
				.append(" ")
				.append(timeUnit)
				.toString();
		}
		
	}
}
