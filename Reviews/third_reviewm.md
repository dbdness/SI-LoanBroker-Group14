# Loan Broker review 3 med Tine

* We have picked up the pace, and are underway.
* We have successfully consumed our own Rule Base, and created a RabbitMQ cluster on a droplet.
* We are currently implementing the third bank.
* Error-handling: double request from same SSN?
* Third bank is SOAP – therefore it can’t put a response on a MessageQueue. Extra component/adapter to put the SOAP bank answer on a queue?
