package com.db.awmd.challenge.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.db.awmd.challenge.client.FundsTransferRequest;
import com.db.awmd.challenge.client.Link;
import com.db.awmd.challenge.client.TransactionJob;
import com.db.awmd.challenge.exception.ResourceException;
import com.db.awmd.challenge.service.TransactionService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/transaction")
@Slf4j
public class TransactionController {
	
	@Autowired
	private TransactionService transactionService;
	

	/**
	 * 
	 * @param fundsTransferRequest
	 * @return
	 */
	@RequestMapping(value = "/jobs", produces = {TransactionJob.MEDIA_TYPE_JSON,
			MediaType.APPLICATION_JSON_VALUE },consumes={FundsTransferRequest.MEDIA_TYPE_JSON}, method = RequestMethod.POST)
	public ResponseEntity<Object> startTransaction(@RequestBody FundsTransferRequest fundsTransferRequest) {
		log.info("Received transaction request {}", fundsTransferRequest);
	    if(fundsTransferRequest == null)
	    	System.out.println("fundsTransferRequest is NULL");
	    else
	    	System.out.println(fundsTransferRequest.getSourceAccountId());
	    TransactionJob transactionJob = null;
	    try {
	    	transactionJob = transactionService.transferFunds(fundsTransferRequest);
	    	log.info("Transaction initiated : {}", transactionJob.getTransactionJobId());
	    	addHATEOASLinksForJob(transactionJob, "transactionJobStatus");
	    }catch(ResourceException e) {
	    	return new ResponseEntity<>(e, HttpStatus.BAD_REQUEST);
	    }catch(Throwable t) {
	    	log.error("Uncaught exception encountered while transferring funds for request: " + fundsTransferRequest, t);
	    	return new ResponseEntity<>("An internal server error occured.", HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    
	    return new ResponseEntity<>(transactionJob, HttpStatus.OK);
	}

	
	/**
	 * 
	 * @param transactionJobId
	 * @return
	 */
	@RequestMapping(value = "/jobs/{transactionJobId}", produces = { TransactionJob.MEDIA_TYPE_JSON,
			MediaType.APPLICATION_JSON_VALUE }, method = RequestMethod.GET)
	public ResponseEntity<Object> getTransferJobStatus(@PathVariable String transactionJobId) {
		log.info("Retrieving transaction job status for transactionJobId {}", transactionJobId);
	    
	    TransactionJob transactionJob = null;
	    try {
	    	transactionJob = transactionService.getTransactionJobStatus(transactionJobId);
	    	addHATEOASLinksForJob(transactionJob, Link.REL_SELF);
	    }catch(ResourceException e) {
	    	return new ResponseEntity<>(e, HttpStatus.BAD_REQUEST);
	    }catch(Throwable t) {
	    	log.error("Uncaught exception encountered while fetching transaction job status for transactionJobId: " + transactionJobId, t);
	    	return new ResponseEntity<>("An internal server error occured.", HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    
	    return new ResponseEntity<>(transactionJob, HttpStatus.OK);
	}
	  
	/**
	 * 
	 * @param transactionJob
	 * @param rel
	 */
	private void addHATEOASLinksForJob(TransactionJob transactionJob, String rel) {
		List<Link> links = new ArrayList<Link>();
		
		Link jobStatusLink = new Link("Link to get transaction job status", HttpMethod.GET.name(), rel, "/transaction/jobs/"+ transactionJob.getTransactionJobId(),
				TransactionJob.MEDIA_TYPE_JSON);
		links.add(jobStatusLink);
		
		transactionJob.setLinks(links);
	}
}