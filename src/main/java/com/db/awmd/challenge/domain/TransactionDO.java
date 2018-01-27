package com.db.awmd.challenge.domain;

import java.math.BigDecimal;

import com.db.awmd.challenge.client.TransactionJob.TransactionStatus;

import lombok.Data;

@Data
public class TransactionDO {
	private String sourceAccountId;
	private String targetAccountId;
	private BigDecimal amount;
	
	private String transactionId;
	private TransactionStatus transactionStatus;
	
	// Ignoring startTime, endTime, createdBy, etc. fields for now
}