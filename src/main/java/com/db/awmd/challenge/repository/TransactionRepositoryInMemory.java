package com.db.awmd.challenge.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.db.awmd.challenge.domain.TransactionDO;

@Repository
public class TransactionRepositoryInMemory implements TransactionRespository {

	private final Map<String, TransactionDO> transactions = new ConcurrentHashMap<>();
	
	@Override
	public void createTransaction(TransactionDO transactionDO) {
		transactions.putIfAbsent(transactionDO.getTransactionId(), transactionDO);
	}

	@Override
	public TransactionDO findTransactionById(String transactionJobId) {
		return transactions.get(transactionJobId);
	}

	@Override
	public void clearTransactions() {
		transactions.clear();
	}

	@Override
	public void updateTransactionJob(TransactionDO transactionDO) {
		transactions.put(transactionDO.getTransactionId(), transactionDO);
	}
}