A retry tool based on google guava, you can use it to retry your task easily.

Only 2 classes you need to know: Retry, RetryBuilder.

Example:
```
Retryer retryer = new RetryerBuilder()
   .times(3)
   .interval(10, SECONDS)
   .whenThrow(timeoutException())
   .whenReturn(errorResponse())
   .build();
		
Response response = retryer.callWithRetry(new Callable<Response>() {
   public Response call() throws Exception {
      return removeService.upload(request);
   }
});

```

Retry condition:
```
private static Predicate<Exception> timeoutException() {
   return new Predicate<Exception>() {
      public boolean apply(Exception exception) {
         if (exception instanceof TimeoutException) {
            return exception.getMessage().startsWith("Retryable");
         }
         return false;
      }
   };
}
```