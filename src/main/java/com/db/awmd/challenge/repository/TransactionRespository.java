package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.TransactionDO;

public interface TransactionRespository {

	void createTransaction(TransactionDO transactionDO);

	TransactionDO findTransactionById(String transactionJobId);
	
	void clearTransactions();

	void updateTransactionJob(TransactionDO transactionDO);
}
