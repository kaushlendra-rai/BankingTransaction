package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import com.db.awmd.challenge.client.FundsTransferRequest;
import com.db.awmd.challenge.client.TransactionJob;
import com.db.awmd.challenge.client.TransactionJob.TransactionStatus;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransactionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@Slf4j
public class TransactionControllerTest {

	private MockMvc mockMvc;

	  @Autowired
	  private AccountsService accountsService;
	  
	  @Autowired
	  private TransactionService transactionService;

	  @Autowired
	  private WebApplicationContext webApplicationContext;

	  @Before
	  public void prepareMockMvc() {
	    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

	    // Reset the existing accounts and transactions before each test.
	    accountsService.getAccountsRepository().clearAccounts();
	    transactionService.clearTransactions();
	  }
	  
	  @Test
	  public void validateInvalidSourceAccountIdForTransactionRequest() throws Exception{
		  String account1 = "123";
		  String account2 = "abc";
		  
		  String amount2 = "2000";
		  String transactionAmount = "0";
		  
		  // Create only Target Account
		  createTestAccountsForTransaction(account2, amount2);
		  
		  String transactionRequest = "{ \"sourceAccountId\": \"" + account1 + "\", \"targetAccountId\": \"" + account2 + "\", \"amount\": " + transactionAmount + " }";
		  log.info("request {}", transactionRequest);
		  
		  this.mockMvc.perform(post("/v1/transaction/jobs").contentType(FundsTransferRequest.MEDIA_TYPE_JSON).accept(TransactionJob.MEDIA_TYPE_JSON)
			      .content(transactionRequest))
		  .andExpect(status().isBadRequest())
		  .andExpect(jsonPath("$.errorCode").value("1001"));
	  }
	  
	  @Test
	  public void validateInvalidTargetAccountIdForTransactionRequest() throws Exception{
		  String account1 = "123";
		  String account2 = "abc";
		  
		  String amount1 = "2000";
		  String transactionAmount = "0";
		  
		  // Create only Source Account
		  createTestAccountsForTransaction(account1, amount1);
		  
		  String transactionRequest = "{ \"sourceAccountId\": \"" + account1 + "\", \"targetAccountId\": \"" + account2 + "\", \"amount\": " + transactionAmount + " }";
		  log.info("request {}", transactionRequest);
		  
		  this.mockMvc.perform(post("/v1/transaction/jobs").contentType(FundsTransferRequest.MEDIA_TYPE_JSON).accept(TransactionJob.MEDIA_TYPE_JSON)
			      .content(transactionRequest))
		  .andExpect(status().isBadRequest())
		  .andExpect(jsonPath("$.errorCode").value("1002"));
	  }
	  
	  @Test
	  public void validateNegativeAmountForTransactionRequest() throws Exception{
		  String account1 = "123";
		  String account2 = "abc";
		  
		  String amount1 = "2000";
		  String amount2 = "2000";
		  String transactionAmount = "-10";
		  
		  createTestAccountsForTransaction(account1, amount1);
		  createTestAccountsForTransaction(account2, amount2);
		  
		  String transactionRequest = "{ \"sourceAccountId\": \"" + account1 + "\", \"targetAccountId\": \"" + account2 + "\", \"amount\": " + transactionAmount + " }";
		  log.info("request {}", transactionRequest);
		  
		  this.mockMvc.perform(post("/v1/transaction/jobs").contentType(FundsTransferRequest.MEDIA_TYPE_JSON).accept(TransactionJob.MEDIA_TYPE_JSON)
			      .content(transactionRequest))
		  .andExpect(status().isBadRequest())
		  .andExpect(jsonPath("$.errorCode").value("1003"));
	  }
	  
	  @Test
	  public void validateZeroAmountForTransactionRequest() throws Exception{
		  String account1 = "123";
		  String account2 = "abc";
		  
		  String amount1 = "2000";
		  String amount2 = "2000";
		  String transactionAmount = "0";
		  
		  createTestAccountsForTransaction(account1, amount1);
		  createTestAccountsForTransaction(account2, amount2);
		  
		  String transactionRequest = "{ \"sourceAccountId\": \"" + account1 + "\", \"targetAccountId\": \"" + account2 + "\", \"amount\": " + transactionAmount + " }";
		  log.info("request {}", transactionRequest);
		  
		  this.mockMvc.perform(post("/v1/transaction/jobs").contentType(FundsTransferRequest.MEDIA_TYPE_JSON).accept(TransactionJob.MEDIA_TYPE_JSON)
			      .content(transactionRequest))
		  .andExpect(status().isBadRequest())
		  .andExpect(jsonPath("$.errorCode").value("1003"));
	  }
	  
	  @Test
	  public void sourceAccountSameAsTargetAccountForTransactionRequest() throws Exception{
		  String account1 = "123";
		  String amount1 = "2000";
		  
		  String transactionAmount = "0";
		  
		  createTestAccountsForTransaction(account1, amount1);
		  
		  String transactionRequest = "{ \"sourceAccountId\": \"" + account1 + "\", \"targetAccountId\": \"" + account1 + "\", \"amount\": " + transactionAmount + " }";
		  log.info("request {}", transactionRequest);
		  
		  this.mockMvc.perform(post("/v1/transaction/jobs").contentType(FundsTransferRequest.MEDIA_TYPE_JSON).accept(TransactionJob.MEDIA_TYPE_JSON)
			      .content(transactionRequest))
		  .andExpect(status().isBadRequest())
		  .andExpect(jsonPath("$.errorCode").value("1006"));
	  }
	  
	  @Test
	  public void insufficientFundsInSourceAccountForTransactionRequest() throws Exception{
		  String account1 = "123";
		  String account2 = "abc";
		  
		  String amount1 = "2000";
		  String amount2 = "2000";
		  String transactionAmount = "3000";
		  
		  createTestAccountsForTransaction(account1, amount1);
		  createTestAccountsForTransaction(account2, amount2);
		  
		  String transactionRequest = "{ \"sourceAccountId\": \"" + account1 + "\", \"targetAccountId\": \"" + account2 + "\", \"amount\": " + transactionAmount + " }";
		  log.info("request {}", transactionRequest);
		  
		  this.mockMvc.perform(post("/v1/transaction/jobs").contentType(FundsTransferRequest.MEDIA_TYPE_JSON).accept(TransactionJob.MEDIA_TYPE_JSON)
			      .content(transactionRequest))
		  .andExpect(status().isBadRequest())
		  .andExpect(jsonPath("$.errorCode").value("1005"));
	  }
	  
	  @Test
	  public void initiateValidTrancaction() throws Exception {
		  String account1 = "123";
		  String account2 = "abc";
		  
		  String amount1 = "1000";
		  String amount2 = "2000";
		  createTestAccountsForTransaction(account1, amount1);
		  createTestAccountsForTransaction(account2, amount2);
		  
		  String transactionAmount = "100";
		  String transactionRequest = "{ \"sourceAccountId\": \"" + account1 + "\", \"targetAccountId\": \"" + account2 + "\", \"amount\": " + transactionAmount + " }";
		  log.info("request {}", transactionRequest);
		  
		  MvcResult mvcResult =  this.mockMvc.perform(post("/v1/transaction/jobs").contentType(FundsTransferRequest.MEDIA_TYPE_JSON).accept(TransactionJob.MEDIA_TYPE_JSON)
			      .content(transactionRequest))
		  .andExpect(status().isOk())
		  .andReturn();
		  
		  String response = mvcResult.getResponse().getContentAsString();
		  log.info("Respose {}", response);
		  ObjectMapper mapper = new ObjectMapper();
		  TransactionJob transactionJob = mapper.readValue(response, new TypeReference<TransactionJob>() {});
		  log.info("JobId {}" , transactionJob.getTransactionJobId());
	    
		  while(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS)) {
			  transactionJob = getTransactionJobStatus(transactionJob.getTransactionJobId());
			  if(!(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS))) {
				  assertThat(TransactionStatus.SUCCESS).isEqualTo(transactionJob.getTransactionStatus());
				  break;
			  }
			  
			  try {
				  // Sleep for 50 milli seconds before polling the status again
				  Thread.sleep(50);
			  }catch(Exception e) {
			  }
		  }
		  
		  Account account123 = this.accountsService.getAccount(account1);
		  Account accountAbc = this.accountsService.getAccount(account2);
		  
		  assertThat(new BigDecimal("900")).isEqualTo(account123.getBalance());
		  assertThat(new BigDecimal("2100")).isEqualTo(accountAbc.getBalance());
	  }
	  
	  @Test
	  public void initiateTwoTrancactionsInSequence() throws Exception {
		  String accountId1 = "123";
		  String accountId2 = "abc";
		  
		  String amount1 = "1000";
		  String amount2 = "2000";
		  createTestAccountsForTransaction(accountId1, amount1);
		  createTestAccountsForTransaction(accountId2, amount2);
		  
		  String transactionAmount = "100";

		  initiateTransaction(accountId1, accountId2, transactionAmount);
		  
		  Account account1 = this.accountsService.getAccount(accountId1);
		  Account account2 = this.accountsService.getAccount(accountId2);
		  
		  assertThat(new BigDecimal("900")).isEqualTo(account1.getBalance());
		  assertThat(new BigDecimal("2100")).isEqualTo(account2.getBalance());
		  
		  transactionAmount = "200";
		  
		  initiateTransaction(accountId1, accountId2, transactionAmount);
		  
		  account1 = this.accountsService.getAccount(accountId1);
		  account2 = this.accountsService.getAccount(accountId2);
		  assertThat(new BigDecimal("700")).isEqualTo(account1.getBalance());
		  assertThat(new BigDecimal("2300")).isEqualTo(account2.getBalance());
	  }
	  
	  @Test
	  public void initiateTransactionsInParallelFromSameSourceToSameTarget() throws Exception{
		  String accountId1 = "123";
		  String accountId2 = "abc";
		  
		  String amount1 = "5000";
		  String amount2 = "4000";
		  String transactionAmount = "100";
		  
		  createTestAccountsForTransaction(accountId1, amount1);
		  createTestAccountsForTransaction(accountId2, amount2);
		  
		  List<String> transactionJobIds = new ArrayList<>();
		  
		  int parallelTransactionCount = 20;
		  for(int i=0; i < parallelTransactionCount; i++) {
			  Runnable task2 = () -> { 
				  try{
					  transactionJobIds.add(initiateTransaction(accountId1, accountId2, transactionAmount)); 
				  }catch(Exception e) {}};
			  new Thread(task2).start();
		  }
		  
		  // Since Job are running in parallel, we need to wait till all jobs have been initiated in their threads.
		  // Only after that we should try to get transaction and account status.
		  while(transactionJobIds.size() < parallelTransactionCount) {
			  try {
				  Thread.sleep(200);
			  }catch(Exception e) {
				  log.error("Exception while sleeping", e);
			  }
		  }
		  
		  // .Since all jobs are running in parallel threads, we need to poll job status in main thread to verify transactions are complete prior to checking account balances. 
		  for(String transactionJobId : transactionJobIds) {
			  TransactionJob transactionJob = getTransactionJobStatus(transactionJobId);
			  
			  validateTransactionJobStatusAsSuccess(transactionJob);
		  }
		  
		  // Ensure the consistency of account after transactions
		  Account account1 = this.accountsService.getAccount(accountId1);
		  Account account2 = this.accountsService.getAccount(accountId2);
		  assertThat(new BigDecimal("3000")).isEqualTo(account1.getBalance());
		  assertThat(new BigDecimal("6000")).isEqualTo(account2.getBalance());
	  }
	  
	  private String initiateTransaction(String account1, String account2, String transactionAmount) throws Exception{
		  String transactionRequest = "{ \"sourceAccountId\": \"" + account1 + "\", \"targetAccountId\": \"" + account2 + "\", \"amount\": " + transactionAmount + " }";
		  
		  MvcResult mvcResult =  this.mockMvc.perform(post("/v1/transaction/jobs").contentType(FundsTransferRequest.MEDIA_TYPE_JSON).accept(TransactionJob.MEDIA_TYPE_JSON)
			      .content(transactionRequest))
		  .andExpect(status().isOk())
		  .andReturn();
		  
		  return validateTransactionJobStatusAsSuccess(mvcResult);
	  }
	  
	  private String validateTransactionJobStatusAsSuccess(MvcResult mvcResult) throws Exception{
		  String response = mvcResult.getResponse().getContentAsString();
		  ObjectMapper mapper = new ObjectMapper();
		  TransactionJob transactionJob = mapper.readValue(response, new TypeReference<TransactionJob>() {});
	    
		  return validateTransactionJobStatusAsSuccess(transactionJob);
	  }
	  
	  private String validateTransactionJobStatusAsSuccess(TransactionJob transactionJob) throws Exception{
		 
		  while(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS)) {
			  transactionJob = getTransactionJobStatus(transactionJob.getTransactionJobId());
			  if(!(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS))) {
				  assertThat(TransactionStatus.SUCCESS).isEqualTo(transactionJob.getTransactionStatus());
				  break;
			  }
			  
			  try {
				  // Sleep for 50 milli seconds before polling the status again
				  Thread.sleep(50);
			  }catch(Exception e) {
			  }
		  }
		  
		  return transactionJob.getTransactionJobId();
	  }
	  
	  private void createTestAccountsForTransaction(String accountId, String amount) throws Exception {
		  this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
		      .content("{\"accountId\":\"" + accountId + "\",\"balance\":" + amount + "}")).andExpect(status().isCreated());

		  Account account123 = accountsService.getAccount(accountId);
		  assertThat(account123.getAccountId()).isEqualTo(accountId);
		  assertThat(account123.getBalance()).isEqualByComparingTo(amount);
	  }
	  
	  private TransactionJob getTransactionJobStatus(String transactionId) throws Exception{
		  MvcResult mvcResult = this.mockMvc.perform(get("/v1/transaction/jobs/" + transactionId).accept(TransactionJob.MEDIA_TYPE_JSON))
				  .andExpect(status().isOk())
				  .andReturn();
		  
		  String response = mvcResult.getResponse().getContentAsString();
		  ObjectMapper mapper = new ObjectMapper();
		  TransactionJob transactionJob = mapper.readValue(response, new TypeReference<TransactionJob>() {});

		  return transactionJob;
	  }
}