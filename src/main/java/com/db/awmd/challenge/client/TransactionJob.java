package com.db.awmd.challenge.client;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.db.awmd.challenge.domain.AccountConstants;

import lombok.Data;

@Data
public class TransactionJob {

	public static final String MEDIA_TYPE = "application/com.db.transaction.job";
	public static final String MEDIA_TYPE_JSON = MEDIA_TYPE+AccountConstants.JSON;
	
	@NotNull
	@NotEmpty
	private String transactionJobId;
	
	private int version = 1;
	private TransactionStatus transactionStatus;
	
	private String sourceAccountId;
	private String targetAccountId;
	private BigDecimal amount;
	
	// It holds the HATEOAS links for this response.
	private List<Link> links;
	
	public TransactionJob() {
	}
	
	public TransactionJob(String transactionJobId) {
		this.transactionJobId = transactionJobId;
	}
	
	public enum TransactionStatus {
		SUCCESS("Success"), FAILED("Failed"), INSUFFICIENT_FUNDS("Insufficient Funds"), IN_PROGRESS("In Progress"),
		TRANSACTION_TIMEOUT("Transaction timeout");

		private String stringVal;

		TransactionStatus(String value) {
			this.stringVal = value;
		}

		@Override
		public String toString() {
			return stringVal;
		}
	}
}