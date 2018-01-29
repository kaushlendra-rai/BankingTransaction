package com.db.awmd.challenge.service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import com.db.awmd.challenge.DevChallengeApplication;
import com.db.awmd.challenge.client.TransactionJob;
import com.db.awmd.challenge.client.TransactionJob.TransactionStatus;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransactionDO;
import com.db.awmd.challenge.exception.ResourceException;
import com.db.awmd.challenge.repository.TransactionRespository;
import com.db.awmd.challenge.util.TransactionUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * We maintain separate Threadpools for debit and credit transactions to accounts so that debit/credit does not consumes all
 * threads and both transactions runs in parallel.
 * Also, Notifications are sent as asynchronous task as it should not block completion of Transaction for any delays in sending notifications.
 * @author sinkar
 *
 */
@EnableAsync
@Component
@Slf4j
public class FundsTransferManager {
	
	@Autowired
	private AccountsService accountsService;
	
	@Autowired
	private TransactionRespository transactionRespository;
	
	@Autowired
	private NotificationService notificationService;
	
	private final Object debitLockObject = new Object();
	private final Object creditLockObject = new Object();
	
	// In a production deployment, it would be implemented using a distributed Cache
	private Set<String> activeTransactionSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	
	/**
	 * It is an async method for initiating a transaction. In a production setup, event would be triggered for this activity. 
	 * Based on request load, more instances of debit worker nodes could be added.
	 * Making the transaction async ensures that the web application is able to cater to larger number of transaction requests.
	 *  
	 * @param transactionJob The transaction to be initiated for funds transfer.
	 */
	@Async(DevChallengeApplication.DEBIT_TRANSACTION_THREADPOOL)
	public void startAsyncTransaction(TransactionJob transactionJob, int retryCount) {
		log.debug("startAsynchTransaction() transactionJobId = " + transactionJob.getTransactionJobId());
		
		// A retry counter added in case a transaction was already in process for the source account.
		// Typically, the event would be re-thrown to the queue which could be used to handle such cases and have a threshold on retry count and mark the transaction job as 'TIMED_OUT' if it exceeds certain limit.
		// In production code, we will not have this Thread.sleep. 
		// The special queue handling the concurrent retry DEBIT requests will process it appropriately. Also, it would be a rare case. 
		if(retryCount > 0) {
			try {
				Thread.sleep(10 * retryCount);
			}catch(Exception e) {
				// Do nothing
			}
		}
		debitFundsFromSourceAccount(transactionJob, retryCount);
		
		// After successful debit of amount, start asynchronous job for credit amount in target account.
		if(retryCount == 0)
			startAsyncCreditTransaction(transactionJob, 0);
	}

	/**
	 * This method initiates the actual debit on source account.
	 * 
	 * @param transactionJob the transaction job request object for which debit is to be initiated for source account.
	 */
	private void debitFundsFromSourceAccount(TransactionJob transactionJob, int retryCount) {
		// If a transaction is already in progress for source account, put the job back in queue.
		if(activeTransactionSet.contains(transactionJob.getSourceAccountId()))
			startAsyncTransaction(transactionJob, retryCount+1);
		else {
			synchronized (debitLockObject) {
				if(activeTransactionSet.contains(transactionJob.getSourceAccountId())) {
					startAsyncTransaction(transactionJob, retryCount+1);
					return;
				}else {
					activeTransactionSet.add(transactionJob.getSourceAccountId());
				}
			}
			
			log.info("Initiate Debit for transaction {} of amount {}", transactionJob.getTransactionJobId(), transactionJob.getAmount());
			TransactionDO transactionDO = TransactionUtil.convertTransactionJobToTransactionDO(transactionJob);
			try {
				accountsService.debitSourceAccountForTransaction(transactionDO);
				
				// Mark the transaction as DEBIT_SUCCESS after actual debit.
				transactionDO.setTransactionStatus(TransactionStatus.DEBIT_SUCCESS);
				transactionRespository.updateTransactionJob(transactionDO);
			}catch(ResourceException re) {
				// Mark the transaction FAILED.
				transactionDO.setTransactionStatus(TransactionStatus.INSUFFICIENT_FUNDS);
				transactionRespository.updateTransactionJob(transactionDO);
				
				throw re;
			}
			
			// After successful debit, remove the source account from active transaction set.
			// This will allow another debit from the account if a transaction is initiated again.
			sendNotification(transactionDO.getSourceAccountId(), "Account number: " + transactionDO.getSourceAccountId() + " debited by amount : INR " + transactionDO.getAmount());
			activeTransactionSet.remove(transactionJob.getSourceAccountId());
		}
	}
	
	/**
	 * The Debit and credits need not be linked and performed in same atomic action.
	 * Once a successful Debit has been made, a corresponding credit needs to be made for the same. The only requirement is that credit request must not get lost in the system.
	 * For that we initiate an event to process the credit task on a separate Queue.
	 * However, in current code, we initiate an async job for credit task.
	 * 
	 * @param transactionJob The transaction job for which credit is to be initiated for target account.
	 */
	@Async(DevChallengeApplication.CREDIT_TRANSACTION_THREADPOOL)
	private void startAsyncCreditTransaction(TransactionJob transactionJob, int retryCount) {
		// A retry added in case a transaction was already in process for the target account.
		// Typically, the event would be re-thrown to the queue which could be used to handle such cases and have a threshold on retry count and mark the transaction job as 'TIMED_OUT' if it exceeds certain limit.
		// In production code, we will not have this Thread.sleep. 
		// The special queue handling the concurrent retry CREDIT requests will process it appropriately. Also, it would be a rare case. 
		if(retryCount > 0) {
			try {
				Thread.sleep(10 * retryCount);
			}catch(Exception e) {
				// Do nothing
			}
		}
		
		if(activeTransactionSet.contains(transactionJob.getTargetAccountId())) {
			startAsyncCreditTransaction(transactionJob, retryCount + 1);
		}else {
			synchronized (creditLockObject) {
				if(activeTransactionSet.contains(transactionJob.getTargetAccountId())) {
					startAsyncCreditTransaction(transactionJob, retryCount + 1);
					return;
				}else {
					activeTransactionSet.add(transactionJob.getTargetAccountId());
				}
			}
			log.info("Initiate Credit for transaction {} of amount {}", transactionJob.getTransactionJobId(), transactionJob.getAmount());
			TransactionDO transactionDO = TransactionUtil.convertTransactionJobToTransactionDO(transactionJob);
			accountsService.creditTargetAccountForTransaction(transactionDO);
			
			// Update the transaction status to SUCCESS after successful credit to Target account
			transactionDO.setTransactionStatus(TransactionStatus.SUCCESS);
			transactionRespository.updateTransactionJob(transactionDO);
			
			// After successful credit, remove the source account from active transaction set.
			// This will allow another credit from the account if a transaction is initiated again.
			sendNotification(transactionDO.getSourceAccountId(), "Account number: " + transactionDO.getTargetAccountId() + " credited with amount : INR " + transactionDO.getAmount());
			activeTransactionSet.remove(transactionJob.getTargetAccountId());
		}
	}
	
	/**
	 * Since sending notification immediately is not part of business critical functionality, off-loading this task to thread pool as an asynchronous job and allow transaction to complete.
	 * Delays in sending notification should not actually hold the transaction from completion.
	 * In production setup, the event must be triggered to send notification.
	 * 
	 * @param accountId The accountId to which notifications is to be sent.
	 * @param transferDescription The message to be sent in the notification.
	 */
	@Async(DevChallengeApplication.NOTIFICATION_THREADPOOL)
	public void sendNotification(String accountId,  String transferDescription) {
		Account account = accountsService.getAccount(accountId);
		notificationService.notifyAboutTransfer(account, transferDescription);
	}
}