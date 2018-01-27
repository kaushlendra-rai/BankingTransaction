package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransactionDO;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

public interface AccountsRepository {

  void createAccount(Account account) throws DuplicateAccountIdException;

  Account getAccount(String accountId);
  
  void debitAccountForTransaction(TransactionDO transactionDO);
  
  void creditAccountForTransaction(TransactionDO transactionDO);

  void clearAccounts();
}