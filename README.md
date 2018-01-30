# BankingTransaction
The application aims at simulating banking transactions with async jobs and very light locking

In the code, for async job, I have used ThreadPool which needs to be linked to Eventing and EventConsumers in real projects.
The events triggered can be also maintained as audit logs for transactions and can be referred in case of disputes.

NOTE:
In the current code 'FundsTransferManager' is kind of doing some critical processing for this project for simplicity.
Ideally, Async processing for Debit, Credit & Notifications should be handled in their separate classes.


Key design elements thought:
1) Instead of taking accountIds in URI for request, I took them in POST payload for data security as accountId is sensitive data.

2) Transaction initiated as an async job. Also, Debit and Credit actions are treated as two separate activities.
2 a) In order to maintain 'Atomicity' of transaction, for every Debit, there should be only one Credit.
2 b) Pre-requisite of transaction is a successful Debit. Once a debit is successfully done, we trigger an event for Credit task.
2 c) At a given point of time, there could be only one transaction (credit/debit) for a given account. This is to ensure consistency. If multiple requests for the same come, trigger an event for the transaction to be processed later.
2 d) While processing the actual Debit/Credit, ensure that the debit/credit transactions have not been processed earlier too (It could happen debit/credit events either due to some network issue while sending acknowledgement to Event-broker, etc). If processed earlier, ignore the current credit/debit to avoid side-effects of event replay. It is crucial for Idempotency of events for debit and credit tasks.
2 e) There should be a TRANSACTION_TIMEOUT for transaction which has been in too many re-tries and update job accordingly.
2 f) Notifications should be sent as an async task only. Any delays in sending notification should not account in deplays of transaction processing.
2 g) The TransactionStatus to be maintained in Distributed cache along with persistance store. Maintain distributed cache for Job status so that the job status for fresh transactions is served from the cache and thereby saving load on DB. Purge cache which is 1 hour old to maintain cache size.

3) Having a 'async' transactions help us scale. With async transactions, there is no blocking threads on teh server, thereby giving more room to serve new requests to the Application server.
4) The real DEBIT/CREDIT transactions could be processed on seperate cluster of nodes which are more event driven. They can be scaled as needed.
5) The real DEBIT/CREDIT transactions would be persisted with a new transactionId along with this transactionJobId (acts as a corelationId between the DEBIT and CREDIT of same transaction).
6) CORRECTION FOR FAILED CREDITS: For any DEBIT which has no corresponding CREDIT, a scheduled job MUST perform a reverse CREDIT to the source account so that teh funds go back to the original source account in case of failure to credit to Target Account.

7) I have used synchronized blocks for finding if any transaction is currently active for an account rther than holding lock for teh entire transaction. In production, we can use a pool (10-100) of distributed Locks hashed over the accountId. This would further reduce the contention on locks as hash on random accountId would get an even distribution in pool of locks.
8) We should use separate QUEUES for Debit/Credit/Notifications so that they all work in parallel and none of the task over-shadows the other.
9) The UI/client should use links provided in response to poll for Job status. It can either poll for status being 'DEBIT_SUCCESS' only or 'SUCCESS' (DEBIT & CREDIT) state of job as per the needs of business use case.
