package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.TransactionDO;


public interface TransactionRespository {

	/**
	 * It creates a transaction entry maintaining the Transaction object.
	 * @param transactionDO The basic TransactionDO object holding information of transaction 
	 */
	void createTransaction(TransactionDO transactionDO);

	/**
	 * Fetches the transaction object based on the transaction id
	 * @param transactionJobId The unique identifier for the transaction.
	 * @return an instance of TransactionDO if one exists.
	 */
	TransactionDO findTransactionById(String transactionJobId);
	
	/**
	 * Removes all the transaction that might have been carried out so far.
	 */
	void clearTransactions();

	/**
	 * Updates the transaction object after an update in the repository. 
	 * @param transactionDO The transaction object to be updated.
	 */
	void updateTransactionJob(TransactionDO transactionDO);
}
