package com.google.common.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
/**
 * 
 * @author wangzijian
 *
 */
public class RetryerBuilder {

	private static final Predicate<Exception> CONDITION_ALWAYS_TRUE = Predicates.alwaysTrue();
	private static final Duration DEFAULT_INTERVAL = new Duration(10, TimeUnit.SECONDS);
	private static final int DEFAULT_RETRY_TIMES = 3;
	
	private Predicate<Exception> condition = CONDITION_ALWAYS_TRUE;
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

	public RetryerBuilder when(Predicate<Exception> condition) {
		checkNotNull(condition, "condition");
		this.condition = condition;
		return this;
	}

	private static final class RetryerImpl implements Retryer {

		private final int times;
		private final Duration interval;
		private final Predicate<Exception> condition;
		
		public RetryerImpl(RetryerBuilder retryerBuilder) {
			this.times = retryerBuilder.times;
			this.interval = retryerBuilder.interval;
			this.condition = retryerBuilder.condition;
		}

		@Override
		public <T> T callWithRetry(Callable<T> retryableTask) throws Exception {
			return doWithRetryTimesReduction(retryableTask, times);
		}
		
		private <T> T doWithRetryTimesReduction(Callable<T> retryableTask, int retryTimes) throws Exception {
			try {
				return retryableTask.call();
			} catch (Exception exception) {
				if (retryTimes != 0) {
					if (condition.apply(exception)) {
						interval.sleep();
						return doWithRetryTimesReduction(retryableTask, retryTimes - 1);
					}
				}
				throw exception;
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
	}
}
