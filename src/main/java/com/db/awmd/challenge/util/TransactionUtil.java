package com.db.awmd.challenge.util;

import com.db.awmd.challenge.client.TransactionJob;
import com.db.awmd.challenge.domain.TransactionDO;

public class TransactionUtil {
	
	public static TransactionJob convertTransactionDOToTransactionJob(TransactionDO transactionDO) {
		TransactionJob transactionJob = new TransactionJob(transactionDO.getTransactionId());
		transactionJob.setTransactionStatus(transactionDO.getTransactionStatus());
		transactionJob.setSourceAccountId(transactionDO.getSourceAccountId());
		transactionJob.setTargetAccountId(transactionDO.getTargetAccountId());
		transactionJob.setAmount(transactionDO.getAmount());
		
		return transactionJob;
	}

	public static TransactionDO convertTransactionJobToTransactionDO(TransactionJob transactionJob) {
		TransactionDO transactionDO = new TransactionDO();
		transactionDO.setSourceAccountId(transactionJob.getSourceAccountId());
		transactionDO.setTargetAccountId(transactionJob.getTargetAccountId());
		transactionDO.setTransactionId(transactionJob.getTransactionJobId());
		transactionDO.setAmount(transactionJob.getAmount());
		transactionDO.setTransactionStatus(transactionJob.getTransactionStatus());
		
		return transactionDO;
	}
}
