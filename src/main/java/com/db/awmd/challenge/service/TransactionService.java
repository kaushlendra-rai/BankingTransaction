package com.db.awmd.challenge.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.client.AccountTransactionErrorCodes;
import com.db.awmd.challenge.client.FundsTransferRequest;
import com.db.awmd.challenge.client.TransactionJob;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransactionDO;
import com.db.awmd.challenge.exception.ResourceException;
import com.db.awmd.challenge.repository.TransactionRespository;
import com.db.awmd.challenge.util.TransactionUtil;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class TransactionService {
	
	@Autowired
	private AccountsService accountsService;
	
	@Autowired
	private TransactionRespository transactionRespository;
	
	@Autowired
	private FundsTransferManager fundsTransferManager;
	
	
	public TransactionJob transferFunds(FundsTransferRequest fundsTransferRequest) throws ResourceException{
		validateTransferRequest(fundsTransferRequest);
		
		TransactionJob transactionJob = persistTransaction(fundsTransferRequest);
		
		fundsTransferManager.startAsyncTransaction(transactionJob);
		
		return transactionJob;
	}
	
	public void updateTransactionJob(TransactionDO transactionDO) {
		
		if(transactionDO.getTransactionId() == null)
			throw new ResourceException("Invalid transaction object. transactionId cannot be null or empty", HttpStatus.NOT_FOUND, AccountTransactionErrorCodes.NULL_EMPTY_TRANSACTION_ID);
		
		TransactionDO validatedTransactionDO = transactionRespository.findTransactionById(transactionDO.getTransactionId());
		
		if(validatedTransactionDO == null)
			throw new ResourceException("Invalid transaction id " + transactionDO.getTransactionId(), HttpStatus.NOT_FOUND, AccountTransactionErrorCodes.INVALID_TRANSACTION_ID);
		
		transactionRespository.updateTransactionJob(transactionDO);
	}

	private TransactionJob persistTransaction(FundsTransferRequest fundsTransferRequest) {
		// Create Unique transactionId
		String transactionJobId = UUID.randomUUID().toString();
		
		// Actually persist the transaction before starting it.
		TransactionDO transactionDO = new TransactionDO();
		transactionDO.setAmount(fundsTransferRequest.getAmount());
		transactionDO.setSourceAccountId(fundsTransferRequest.getSourceAccountId());
		transactionDO.setTargetAccountId(fundsTransferRequest.getTargetAccountId());
		transactionDO.setTransactionId(transactionJobId);
		transactionDO.setTransactionStatus(TransactionJob.TransactionStatus.IN_PROGRESS);
		
		transactionRespository.createTransaction(transactionDO);
		
		// Pass on the client representation of Transaction.
		// Domain representation MUST never be passed in response.
		
		TransactionJob transactionJob = TransactionUtil.convertTransactionDOToTransactionJob(transactionDO);
		
		return transactionJob;
	}
	
	
	private void validateTransferRequest(FundsTransferRequest fundsTransferRequest) {
		if(fundsTransferRequest == null) {
			log.debug("fundsTransferRequest is null");
			throw new ResourceException("Invalid Fund transfer request. Request cannot be null.", HttpStatus.BAD_REQUEST, AccountTransactionErrorCodes.EMPTY_TRANSACTION_REQUEST);
		}
			
		Account sourceAccount = accountsService.getAccount(fundsTransferRequest.getSourceAccountId());
		if(sourceAccount == null)
			throw new ResourceException("Invalid source account id " + fundsTransferRequest.getSourceAccountId(), HttpStatus.BAD_REQUEST, AccountTransactionErrorCodes.INVALID_SOURCE_ACCOUNTID);
			
		Account targetAccount = accountsService.getAccount(fundsTransferRequest.getTargetAccountId());
		if(targetAccount == null)
			throw new ResourceException("Invalid target account id " + fundsTransferRequest.getTargetAccountId(), HttpStatus.BAD_REQUEST, AccountTransactionErrorCodes.INVALID_TARGET_ACCOUNTID);
		
		// Ensure that Source account and Target account are not same.
		if(sourceAccount.getAccountId().equals(targetAccount.getAccountId()))
				throw new ResourceException("Source Account and Target account cannot be same " + fundsTransferRequest.getTargetAccountId(), HttpStatus.BAD_REQUEST, AccountTransactionErrorCodes.SOURCE_ACCOUNT_SAME_AS_TARGET_ACCOUNT);
		
		if(fundsTransferRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			throw new ResourceException("Invalid Transfer Amount " + fundsTransferRequest.getAmount(), HttpStatus.BAD_REQUEST, AccountTransactionErrorCodes.INVALID_FUND_TRANSFER_AMOUNT);
		
		// Ensure that the Source account has the amount in account required for the transaction.
		if(sourceAccount.getBalance().compareTo(fundsTransferRequest.getAmount()) < 0)
			throw new ResourceException("Insufficient funds in source Account " + fundsTransferRequest.getSourceAccountId() + " , transaction amount: " + fundsTransferRequest.getAmount(), HttpStatus.BAD_REQUEST, AccountTransactionErrorCodes.INSUFFICIENT_FUNDS_IN_SOURCE_ACCOUNT);
		
	}

	public TransactionJob getTransactionJobStatus(String transactionJobId) {
		TransactionDO transactionDO = transactionRespository.findTransactionById(transactionJobId);
		
		if(transactionDO == null)
			throw new ResourceException("Invalid transaction id " + transactionJobId, HttpStatus.NOT_FOUND, AccountTransactionErrorCodes.INVALID_TRANSACTION_ID);
		
		return TransactionUtil.convertTransactionDOToTransactionJob(transactionDO);
	}
	
	public void clearTransactions() {
		transactionRespository.clearTransactions();
	}
}