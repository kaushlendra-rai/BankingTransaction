package com.db.awmd.challenge.repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.db.awmd.challenge.client.AccountTransactionErrorCodes;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransactionDO;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.ResourceException;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();
  
  //  In a production deployment, it would be implemented using a distributed Cache backed by persistance
  // The checks for transaction ids would be used for idempotent behavior of re-play of events for debit/credit in event based system.
  // Access to below data is a layer of cache for performance over the actual repository.
  // The cache would have a life span typically, lets say 1 hour to handle any replay.
  private Set<String> activeDebitTransactionSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
  private Set<String> activeCreditTransactionSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	
  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
        "Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Override
  public Account getAccount(String accountId) {
    return accounts.get(accountId);
  }

  @Override
  public void clearAccounts() {
    accounts.clear();
  }

	@Override
	public void debitAccountForTransaction(TransactionDO transactionDO) {
		if(!activeDebitTransactionSet.contains(transactionDO.getTransactionId()) && accounts.get(transactionDO.getSourceAccountId()).getBalance().compareTo(transactionDO.getAmount()) >= 0) {
			activeDebitTransactionSet.add(transactionDO.getTransactionId());
			BigDecimal newBalance = accounts.get(transactionDO.getSourceAccountId()).getBalance().subtract(transactionDO.getAmount());
			accounts.get(transactionDO.getSourceAccountId()).setBalance(newBalance);
		}else
			throw new ResourceException("Insufficient funds in account: " + transactionDO.getSourceAccountId() + " for transaction " + transactionDO.getTransactionId(), HttpStatus.BAD_REQUEST, AccountTransactionErrorCodes.INSUFFICIENT_FUNDS_IN_SOURCE_ACCOUNT);
	}
	
	@Override
	public void creditAccountForTransaction(TransactionDO transactionDO) {
		if(!activeCreditTransactionSet.contains(transactionDO.getTransactionId())) {
			activeCreditTransactionSet.add(transactionDO.getTransactionId());
			BigDecimal newBalance = accounts.get(transactionDO.getTargetAccountId()).getBalance().add(transactionDO.getAmount());
			accounts.get(transactionDO.getTargetAccountId()).setBalance(newBalance);
		}
	}
}