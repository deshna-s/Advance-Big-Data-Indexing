package edu.neu.InsurancePlan;

import edu.neu.InsurancePlan.DAO.ElasticSearchConfiguration;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


//@SpringBootApplication
@SpringBootApplication

public class InsurancePlanApplication {


	public static void main(String[] args) {
		SpringApplication.run(InsurancePlanApplication.class, args);
	}

}
