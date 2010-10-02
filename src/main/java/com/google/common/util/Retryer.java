package com.google.common.util;
import java.util.concurrent.Callable;

/**
 * 
 * @author wangzijian
 * 
 */
public interface Retryer {

	<T> T callWithRetry(Callable<T> retryableTask) throws Exception;
}
