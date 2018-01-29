package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.client.AccountTransactionErrorCodes;
import com.db.awmd.challenge.client.FundsTransferRequest;
import com.db.awmd.challenge.client.TransactionJob;
import com.db.awmd.challenge.client.TransactionJob.TransactionStatus;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.ResourceException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransactionService;
import com.db.awmd.challenge.util.TransactionUtil;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionServiceTest {

	@Autowired
	TransactionService transactionService;
	
	@Autowired
	private AccountsService accountsService;
	
	@Before
	public void prepareMockMvc() {
		// Reset the existing accounts and transactions before each test.
		accountsService.getAccountsRepository().clearAccounts();
		transactionService.clearTransactions();
	}
	
	@Test
	public void testValidTransaction() throws Exception{
		String accountId1 = "123";
		String accountId2 = "abc";
		int balance1 = 1000;
		int balance2 = 2000;
		
		createAccount(accountId1, balance1);
		createAccount(accountId2, balance2);
		
		FundsTransferRequest fundsTransferRequest = new FundsTransferRequest();
		fundsTransferRequest.setAmount(new BigDecimal(500));
		fundsTransferRequest.setSourceAccountId(accountId1);
		fundsTransferRequest.setTargetAccountId(accountId2);
		
		TransactionJob transactionJob = transactionService.transferFunds(fundsTransferRequest);
		
		while(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS)) {
			try {
				// Sleep for 50 milli seconds before polling the status again
				Thread.sleep(50);
			}catch(Exception e) {
			}
			
			transactionJob = transactionService.getTransactionJobStatus(transactionJob.getTransactionJobId());
		}
		assertThat(this.accountsService.getAccount(accountId1).getBalance()).isEqualTo(new BigDecimal(500));
		assertThat(this.accountsService.getAccount(accountId2).getBalance()).isEqualTo(new BigDecimal(2500));
	}
	
	@Test
	public void getTransactionJobStatus_failInvalidJobId() {
		try {
			transactionService.getTransactionJobStatus("invalidTransactionId");
			fail("Since the transaction job id does not exists, it must not reach this point.");
		}catch(ResourceException e) {
			assertThat(e.getErrorCode()).isEqualTo(AccountTransactionErrorCodes.INVALID_TRANSACTION_ID);
		}
	}
	
	@Test
	public void updateTransactionStatus_updateSuccessJobToFailed() throws Exception{
		String accountId1 = "123";
		String accountId2 = "abc";
		int balance1 = 1000;
		int balance2 = 2000;
		
		createAccount(accountId1, balance1);
		createAccount(accountId2, balance2);
		
		FundsTransferRequest fundsTransferRequest = new FundsTransferRequest();
		fundsTransferRequest.setAmount(new BigDecimal(500));
		fundsTransferRequest.setSourceAccountId(accountId1);
		fundsTransferRequest.setTargetAccountId(accountId2);
		
		TransactionJob transactionJob = transactionService.transferFunds(fundsTransferRequest);
		
		while(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS)) {
			try {
				// Sleep for 50 milli seconds before polling the status again
				Thread.sleep(50);
			}catch(Exception e) {
			}
			
			transactionJob = transactionService.getTransactionJobStatus(transactionJob.getTransactionJobId());
		}
		assertThat(transactionJob.getTransactionStatus()).isEqualTo(TransactionStatus.SUCCESS);
		assertThat(this.accountsService.getAccount(accountId1).getBalance()).isEqualTo(new BigDecimal(500));
		assertThat(this.accountsService.getAccount(accountId2).getBalance()).isEqualTo(new BigDecimal(2500));
		
		// Forcefully change the job status to FAILED and verify the same
		transactionJob.setTransactionStatus(TransactionStatus.FAILED);
		transactionService.updateTransactionJob(TransactionUtil.convertTransactionJobToTransactionDO(transactionJob));
		
		transactionJob = transactionService.getTransactionJobStatus(transactionJob.getTransactionJobId());
		assertThat(transactionJob.getTransactionStatus()).isEqualTo(TransactionStatus.FAILED);
	}
	
	private void createAccount(String accountId, int balance) throws Exception {
		Account account = new Account(accountId);
		account.setBalance(new BigDecimal(balance));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount(accountId)).isEqualTo(account);
	}
}