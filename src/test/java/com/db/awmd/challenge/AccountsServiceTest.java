package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.client.AccountTransactionErrorCodes;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransactionDO;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.ResourceException;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }
  
  @Test
  public void debitAccountSuccess() throws Exception{
	  String accountId = "123";
	  Account account = new Account(accountId);
	  account.setBalance(new BigDecimal(1000));
	  this.accountsService.createAccount(account);
	  
	  assertThat(this.accountsService.getAccount(accountId)).isEqualTo(account);
	  
	  TransactionDO transactionDO = new TransactionDO();
	  transactionDO.setAmount(new BigDecimal(100));
	  transactionDO.setSourceAccountId(accountId);
	  transactionDO.setTargetAccountId("dummy");
	  transactionDO.setTransactionId("12345");
	  
	  accountsService.debitSourceAccountForTransaction(transactionDO);
	  assertThat(this.accountsService.getAccount(accountId).getBalance()).isEqualTo(new BigDecimal(900));
  }
  
  /**
   * This will be a case when two or more simultaneous transactions started on same source account and passed initial validations of minimum account balance
   * However, once actual transactions's debit started, it discovered that balance was low after a few debits.
   * This checks ensures safety at lower levels.   
   * @throws Exception
   */
  @Test
  public void debitAccount_failOnInsufficientFunds() throws Exception{
	  String accountId = "123";
	  Account account = new Account(accountId);
	  account.setBalance(new BigDecimal(1000));
	  this.accountsService.createAccount(account);
	  
	  assertThat(this.accountsService.getAccount(accountId)).isEqualTo(account);
	  
	  TransactionDO transactionDO = new TransactionDO();
	  transactionDO.setAmount(new BigDecimal(2000));
	  transactionDO.setSourceAccountId(accountId);
	  transactionDO.setTargetAccountId("dummy");
	  transactionDO.setTransactionId("12345");
	  
	  try {
		  accountsService.debitSourceAccountForTransaction(transactionDO);
		  fail("Transaction should have failed and not reached this point due to insufficient funds in source account");
	  }catch(ResourceException e) {
		  assertThat(e.getErrorCode()).isEqualTo(AccountTransactionErrorCodes.INSUFFICIENT_FUNDS_IN_SOURCE_ACCOUNT);
	  }
	  
  }
  
  @Test
  public void creditAccountSuccess() throws Exception{
	  String accountId = "123";
	  Account account = new Account(accountId);
	  account.setBalance(new BigDecimal(1000));
	  this.accountsService.createAccount(account);
	  
	  assertThat(this.accountsService.getAccount(accountId)).isEqualTo(account);
	  
	  TransactionDO transactionDO = new TransactionDO();
	  transactionDO.setAmount(new BigDecimal(2000));
	  transactionDO.setSourceAccountId("dummy");
	  transactionDO.setTargetAccountId(accountId);
	  transactionDO.setTransactionId("12345");
	  
	  accountsService.creditTargetAccountForTransaction(transactionDO);
	  
	  assertThat(this.accountsService.getAccount(accountId).getBalance()).isEqualTo(new BigDecimal(3000));
  }
}