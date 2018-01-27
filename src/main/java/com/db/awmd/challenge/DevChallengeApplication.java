package com.db.awmd.challenge;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@ComponentScan
public class DevChallengeApplication {

  public static void main(String[] args) {
    SpringApplication.run(DevChallengeApplication.class, args);
  }
  
  public static final String DEBIT_TRANSACTION_THREADPOOL = "debitTransactionThreadpool";
  
  public static final String CREDIT_TRANSACTION_THREADPOOL = "creditTransactionThreadpool";
  
  public static final String NOTIFICATION_THREADPOOL = "notificationThreadpool";
  
  
  /**
	 * This Threadpool would be used to actually initiate transactions between the two valid accounts.
	 * The transaction is an asynchronous job which is submitted to this pool.
	 * It would be used to debit the source account of the said amount.
	 * 
	 * NOTE: Under ideal conditions in production systems, the system would trigger events for transactions and the transactions would be processed by 
	 * event-consumers and not thread pool.
	 * The below threadpool is used as a place holder for asynchronous jobs.
	 * 
	 * @return ThreadPool executor instance.
	 */
	@Bean(name = DEBIT_TRANSACTION_THREADPOOL)
	public ThreadPoolTaskExecutor debitTransactionExecutor(){
		int corePoolSize = 5;
		int maxPoolSize = 20;
		int queueCapacity = 500;
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("debitTransactionExecutor-");
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		return executor;
  }
	
	/**
	 * This Threadpool would be used to credit the amount to the target account which has already been debit from the source account.
	 * The transaction is an asynchronous job which is submitted to this pool.
	 * 
	 * NOTE: Under ideal conditions in production systems, the system would trigger events for credit transactions and the transactions would be processed by 
	 * event-consumers and not thread pool.
	 * The below threadpool is used as a place holder for asynchronous jobs.
	 * 
	 * @return ThreadPool executor instance.
	 */
	@Bean(name = CREDIT_TRANSACTION_THREADPOOL)
	public ThreadPoolTaskExecutor creditTransactionExecutor(){
		int corePoolSize = 5;
		int maxPoolSize = 20;
		int queueCapacity = 500;
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("creditTransactionExecutor-");
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		return executor;
	}
	
	/**
	 * Sending notifications need not be part of any debit/credit transaction flow. It should be sent asynchronously after the actual business task has been acomplished. 
	 * This Threadpool would be used to send the notification asynchronously.
	 * 
	 * NOTE: Under ideal conditions in production systems, the system would trigger events for sending notifications and notification events would be processed by 
	 * event-consumers and not thread pool.
	 * The below threadpool is used as a place holder for asynchronous jobs.
	 * 
	 * @return ThreadPool executor instance.
	 */
	@Bean(name = NOTIFICATION_THREADPOOL)
	public ThreadPoolTaskExecutor notificationThreadpoolExecutor(){
		int corePoolSize = 5;
		int maxPoolSize = 20;
		int queueCapacity = 500;
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("notificationExecutor-");
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		return executor;
  }
}