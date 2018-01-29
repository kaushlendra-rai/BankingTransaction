package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
	    
		  // Poll until transaction completes
		  while(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS) || transactionJob.getTransactionStatus().equals(TransactionStatus.DEBIT_SUCCESS)) {
			  transactionJob = getTransactionJobStatus(transactionJob.getTransactionJobId());
			  if(!(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS)  || transactionJob.getTransactionStatus().equals(TransactionStatus.DEBIT_SUCCESS))) {
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
		  
		  // Validate the 'Location' header for newly created Job resource 
		  String locationHeader = mvcResult.getResponse().getHeader(HttpHeaders.LOCATION);
		  assertThat(locationHeader).isNotNull();
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
		  
		  ExecutorService executor = Executors.newFixedThreadPool(5);//creating a pool of 5 threads  
		  Set<String> transactionJobIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		  
		  int parallelTransactionCount = 10;
		  for(int i=0; i < parallelTransactionCount; i++) {
			  Runnable task = () -> { 
				  try{
					  transactionJobIds.add(initiateTransaction(accountId1, accountId2, transactionAmount)); 
				  }catch(Exception e) {}};
				  
			  executor.execute(task);
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
		  
		  log.info("Source account balance {}", account1.getBalance());
		  log.info("Target account balance {}", account2.getBalance());
		  
		  assertThat(new BigDecimal("4000")).isEqualTo(account1.getBalance());
		  assertThat(new BigDecimal("5000")).isEqualTo(account2.getBalance());
	  }
	  
	  @Test
	  public void initiateTransactionsInParallelBetweenMultipleAccounts() throws Exception{
		  String accountId1 = "123";
		  String accountId2 = "abc";
		  String accountId3 = "456";
		  String accountId4 = "def";
		  String accountId5= "789";
		  String accountId6 = "ghi";
		  
		  String amount1 = "1000";
		  String amount2 = "2000";
		  String amount3 = "3000";
		  String amount4 = "4000";
		  String amount5 = "5000";
		  String amount6 = "6000";
		  
		  String transactionAmount1 = "50";
		  String transactionAmount2 = "100";
		  String transactionAmount3 = "150";
		  String transactionAmount4 = "200";
		  String transactionAmount5 = "250";
		  String transactionAmount6 = "300";
		  
		  createTestAccountsForTransaction(accountId1, amount1);
		  createTestAccountsForTransaction(accountId2, amount2);
		  createTestAccountsForTransaction(accountId3, amount3);
		  createTestAccountsForTransaction(accountId4, amount4);
		  createTestAccountsForTransaction(accountId5, amount5);
		  createTestAccountsForTransaction(accountId6, amount6);
		  
		  ExecutorService executor = Executors.newFixedThreadPool(5);//creating a pool of 5 threads  
		  Set<String> transactionJobIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		  
		  int parallelTransactionCount = 5;
		  for(int i=0; i < parallelTransactionCount; i++) {
			  Runnable task = () -> { 
				  try{
					  transactionJobIds.add(initiateTransaction(accountId1, accountId2, transactionAmount1));
					  transactionJobIds.add(initiateTransaction(accountId2, accountId3, transactionAmount2)); 
					  transactionJobIds.add(initiateTransaction(accountId3, accountId4, transactionAmount3)); 
					  transactionJobIds.add(initiateTransaction(accountId4, accountId5, transactionAmount4)); 
					  transactionJobIds.add(initiateTransaction(accountId5, accountId6, transactionAmount5)); 
					  transactionJobIds.add(initiateTransaction(accountId6, accountId1, transactionAmount6)); 
				  }catch(Exception e) {}};
				  
			  executor.execute(task);
		  }
		  
		  // Since Job are running in parallel, we need to wait till all jobs have been initiated in their threads.
		  // Only after that we should try to get transaction and account status.
		  while(transactionJobIds.size() < parallelTransactionCount * 6) {
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
		  Account account3 = this.accountsService.getAccount(accountId3);
		  Account account4 = this.accountsService.getAccount(accountId4);
		  Account account5 = this.accountsService.getAccount(accountId5);
		  Account account6 = this.accountsService.getAccount(accountId6);
		  
		  log.info("Source account balance {}", account1.getBalance());
		  log.info("Target account balance {}", account2.getBalance());
		  log.info("Source account balance {}", account3.getBalance());
		  log.info("Target account balance {}", account4.getBalance());
		  log.info("Source account balance {}", account5.getBalance());
		  log.info("Target account balance {}", account6.getBalance());
		  
		  assertThat(new BigDecimal("2250")).isEqualTo(account1.getBalance());
		  assertThat(new BigDecimal("1750")).isEqualTo(account2.getBalance());
		  assertThat(new BigDecimal("2750")).isEqualTo(account3.getBalance());
		  assertThat(new BigDecimal("3750")).isEqualTo(account4.getBalance());
		  assertThat(new BigDecimal("4750")).isEqualTo(account5.getBalance());
		  assertThat(new BigDecimal("5750")).isEqualTo(account6.getBalance());
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
		  // Poll until transaction completes
		  while(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS)  || transactionJob.getTransactionStatus().equals(TransactionStatus.DEBIT_SUCCESS)) {
			  transactionJob = getTransactionJobStatus(transactionJob.getTransactionJobId());
			  if(!(transactionJob.getTransactionStatus().equals(TransactionStatus.IN_PROGRESS)  || transactionJob.getTransactionStatus().equals(TransactionStatus.DEBIT_SUCCESS))) {
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