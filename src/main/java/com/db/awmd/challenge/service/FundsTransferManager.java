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
	
	@Async(DevChallengeApplication.DEBIT_TRANSACTION_THREADPOOL)
	public void startAsyncTransaction(TransactionJob transactionJob) {
		log.debug("startAsynchTransaction() transactionJobId = " + transactionJob.getTransactionJobId());
		
		debitFundsFromSourceAccount(transactionJob);
		
		// After successful debit of amount, start asynchronous job for credit amount in target account.
		startAsyncCreditTransaction(transactionJob);
	}

	private void debitFundsFromSourceAccount(TransactionJob transactionJob) {
		log.info("Initiate Debit for transaction {} of amount {}", transactionJob.getTransactionJobId(), transactionJob.getAmount());
		// If a transaction is already in progress for source account, put the job back in queue.
		// Double-check locking
		if(activeTransactionSet.contains(transactionJob.getSourceAccountId()))
			startAsyncTransaction(transactionJob);
		else {
			synchronized (debitLockObject) {
				if(activeTransactionSet.contains(transactionJob.getSourceAccountId())) {
					startAsyncTransaction(transactionJob);
					return;
				}else {
					activeTransactionSet.add(transactionJob.getSourceAccountId());
				}
			}
			
			TransactionDO transactionDO = TransactionUtil.convertTransactionJobToTransactionDO(transactionJob);
			try {
				accountsService.debitSourceAccountForTransaction(transactionDO);
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
	
	@Async(DevChallengeApplication.CREDIT_TRANSACTION_THREADPOOL)
	private void startAsyncCreditTransaction(TransactionJob transactionJob) {
		log.info("Initiate Credit for transaction {} of amount {}", transactionJob.getTransactionJobId(), transactionJob.getAmount());
		if(activeTransactionSet.contains(transactionJob.getTargetAccountId())) {
			startAsyncCreditTransaction(transactionJob);
		}else {
			synchronized (creditLockObject) {
				if(activeTransactionSet.contains(transactionJob.getTargetAccountId())) {
					startAsyncCreditTransaction(transactionJob);
					return;
				}else {
					activeTransactionSet.add(transactionJob.getTargetAccountId());
				}
			}
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
	 * @param accountId The accountId to which notifications is to be sent.
	 * @param transferDescription The message to be sent in the notification.
	 */
	@Async(DevChallengeApplication.NOTIFICATION_THREADPOOL)
	public void sendNotification(String accountId,  String transferDescription) {
		Account account = accountsService.getAccount(accountId);
		notificationService.notifyAboutTransfer(account, transferDescription);
	}
}