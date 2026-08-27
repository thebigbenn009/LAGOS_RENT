package com.lagosrent.listing;

import org.springframework.boot.SpringApplication;

public class TestListingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(ListingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
