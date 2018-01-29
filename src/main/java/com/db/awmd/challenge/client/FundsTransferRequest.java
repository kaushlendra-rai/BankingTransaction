package com.db.awmd.challenge.client;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.db.awmd.challenge.domain.AccountConstants;

import lombok.Data;

/**
 * Sensitive information like 'source accountId' and 'target accountId' should never be part of URI, hence moved them to 'FundsTransferRequest' 
 * which would be sent as payload in HTTP POST request instead of keeping them as a part of URI path.
 * @author sinkar
 *
 */
@Data
public class FundsTransferRequest{

	public static final String MEDIA_TYPE = "application/com.db.funds.transfer.request";
	public static final String MEDIA_TYPE_JSON = MEDIA_TYPE+AccountConstants.JSON;
	
	private int version = 1;
	
	@NotNull
	@NotEmpty
	private String sourceAccountId;
	
	@NotNull
	@NotEmpty
	private String targetAccountId;
	
	@NotNull
	@Min(value = 0, message = "Amount to be transferred should not be negative.")
	private BigDecimal amount;
}