# InsurancePlan

## Architecture Diagram
<https://github.com/gokuljayavel/InsurancePlan/blob/main/ArchitectureDiagram.pdf>



Repository related to development for REST Api prototype model demo work for Indurance plan

Contents
In this project, we will develop a REST Api to parse a JSON schema model divided into three demos

Prototype demo 1
Develop a Spring Boot based REST Api to parse a given sample JSON schema.
Save the JSON schema in a redis key value store.
Demonstrate the use of operations like GET, POST and DELETE for the first prototype demo.
Prototype demo 2
Regress on your model and perform additional operations like PUT and PATCH.
Secure the REST Api with a security protocol like JWT or OAuth2.
Prototype demo 3
Adding Elasticsearch capabilities
Using RedisSMQ for REST API queueing
Pre-requisites
Java
Maven
Redis Server
Elasticsearch and Kibana(Local or cloud based)
Build and Run
Run as Spring Boot Application in any IDE.

Querying Elasticsearch
Run both the application i.e FinalProject and Consumer Message Queue(CMQ). CMQ application will create the indexes.
Run POST query from Postman
Run custom search queries as per your use case(Few are present in DemoQueries)
(Optional) For testing purpose - Inorder to test the indexes separately, Run the PUT query in Testing-ElasticSearchQueries on Kibana. This will create an index in elasticsearch
