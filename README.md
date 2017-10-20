# Loan Broker - Group 14

**Tines e-mail adresse: 
<br>tm@cphbusiness.dk**</br>


## Overall Implementation  
A requestor sends a request for a loan through the system. This request is firstly going through a component called: "Get Credit Score"(A Content Enricher). The "Get Credit Score" makes a request for the Credit Bureau, where all the information about a requestors credit is stored. After the retrieval of the score the component move on in the flow and get the banks through the "Get Banks"(Content Enricher) component. This component retrieves a set of rules based on the score given from the Credit Bureau. Some banks might not want to waste resources on requestors with low scores. When the rules have been applied, the cycle continues and the recipient list go out and grabs the banks that fit the implied rules. The messages are broadcastet to those banks, which were in the recipientlist, after they've been transformed into a format that the bank can read. This is the Translator component that determines the correct format for each bank. The banks look at the credit score from the requestor and based on the score, they determine an interest rate on the requested loan. The banks forwards their Best Quote back to the requestor. Before the requestor receives the Best Quote from each bank, they will go through a Normalizer and Aggregator component. The components will take the banks formats and transform it back to the requestor format and then aggregate all the quotes back into a single message.  

![alt text](https://github.com/dbdness/SI-LoanBroker-Group14/blob/master/Loan%20Broker%20overview.png)
  
**In Brief**  
1. Receive the consumer's loan quote request (ssn, loan amount, loan duration)
2. Obtain credit score from credit agency (ssncredit score)  
3. Determine the most appropriate banks to contact from the Rule Base web service  
4. Send a request to each selected bank (ssn, credit score, loan amount, loan duration)  
5. Collect response from each selected bank  
6. Determine the best response  
7. Pass the result back to the consumer

| Component                 | Description
| -------------             |:-------------:
| Content Enricher          | To access an external data source in order to augment a message with missing information  
| Recipient List            | To inspect an incomming message, determine the list of desired recipients, and forward the message to all channels within the list
| Translator                | Translate a data format into another.  
| Normalizer                | To ensure that the messages matches a common format 
| Aggregator                | Collects and stores individual messages, then publishes a single message combined from the individual messages  
  
* The Credit Bureau is a Web Service where you can get the needed information from the requestors SSN (Social Security Number)
* The Rule base is a recipient list where the broker decides upon which banks to contact based on the credit score for each customer request.
* The banks are the handlers of the customers request. They will give an interest rate of the loan requested. The banks will have different formats and this is where we have the translator and normilzer to handle this issue.  
  
The credit score scale ranges from 0 to 800 (where 800 is the highest score). This score determines how good a credit each customer has.

## Technical aspects

The implementation consist of a mix between SOAP/WSDL web services, RabbitMQ messaging services and a GUI for the end user.
* The Rule Base component will be implemented as a SOAP web service.
* Two more banks will be implemented manually. One as a SOAP web service, the other one as a RabbitMQ messaging service. 
* Every service is asynchronously connected through messaging. 
* The GUI will be a simple web application that allows the user to input his/her social security number for evaluation.


*Implementation techniques*
* The main implementation language used for the Restful APIs  will be JAX-RS (Java).  
* Their will be provided a frontend i Angular.js to show how the system can be used  
* The first bank at exchange `cphbusiness.bankXML` is XML based.
* The second bank at exchange `cphbusiness.bankJSON` is JSON based. 
* Our own bank at exchange _ _ will be _ _ based. 

The XML format for the loan request from the first bank is:
```xml
<LoanRequest>
 <ssn>12345678</ssn>
 <creditScore>685</creditScore>
 <loanAmount>1000.0</loanAmount>
 <loanDuration>1973-01-01 01:00:00.0 CET</loanDuration>
</LoanRequest>
```

The response format is:
```xml
<LoanResponse>
 <interestRate>4.5600000000000005</interestRate>
 <ssn>12345678</ssn>
</LoanResponse>
```



The JSON format for the loan request from the second bank is:
```json
{"ssn":1605789787,"creditScore":598,"loanAmount":10.0,"loanDuration":360}
```

The  response format is:
```json
{"interestRate":5.5,"ssn":1605789787}
```

---

The credit score evaluator WSDL is located at: <br>
http://138.68.85.24:8080/CreditScoreService/CreditScoreService?wsdl 
</br>

Our Rule Base WSDL is located at: <br>
http://94.130.57.246:9000/rules/RequestBankRulesService?wsdl
</br>

Our Bank WSDL can be requested at: <br>
http://94.130.57.246:9000/bankwsdl/BankAppService?wsdl
<br>

The Github repo that describes the WSDL can be found here: <br>
https://github.com/AlexanderFalk/bankwsdl
<br>

The WSDL has an adapter attached, since it is not able to communicate with a MessageQueue (RabbitMQ in this case). 
Therefore the adapter which consumes from the **Get_Banks_Queue** and publish to **Group14_Bank_Response_Queue** , can be found here : <br>
https://github.com/AlexanderFalk/bankadapterXML
<br>

The JSON Bank, which is the second self-made bank, can be found at this location: <br>
http://94.130.57.246:9000/bankjson/bank 
<br>

But you have to send a POST request to this link: <br>
http://94.130.57.246:9000/bankjson/bank/interestrate 
<br>

To see the documentation on how to use the bank, you can find it below: <br>
https://github.com/AlexanderFalk/bankjson 
<br>

---

## Process flow
[Can be found here](https://github.com/dbdness/SI-LoanBroker-Group14/blob/master/Process%20flow.pdf)

## Bottlenecks
The program is not bulletproof. If the server http://94.130.57.246 , running at Hetzner.com, slams down, the whole program will stop functioning. We could make a failover server that function as a "Hot" server. This means that the exact moment the server stops running, the Hot-server will takeover and function as main server. Just until the main server is up and running again. A failover process is always a good idea, but since is a course project, we save the money for another cold day.  
We also haven't thought of making Dead-letter queues in case anything goes wrong during the process flow. This is something that has to be taken into consideration for further development. 
