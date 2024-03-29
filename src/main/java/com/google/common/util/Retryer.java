package com.google.common.util;

import java.util.concurrent.Callable;

/**
 * A retry tool can retry the execution. 
 * 
 * @author wangzijian
 * 
 */
public interface Retryer {

	/**
	 * Retry the execution wrapped in {@link Callable} when the specified condition matched.
	 * 
	 * @param <T>
	 * @param retryableTask
	 * @return
	 * @throws Exception
	 */
	<T> T callWithRetry(Callable<T> retryableTask) throws Exception;

	/**
	 * Returns an instance of interfaceType that delegates all method calls to
	 * the target object, enforcing the retry policy on each call.
	 * 
	 * @param <T>
	 * @param target
	 * @param interfaceType
	 * @return
	 */
	<T> T newProxy(T target, Class<T> interfaceType);
}
