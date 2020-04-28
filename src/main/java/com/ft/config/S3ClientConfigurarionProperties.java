package com.ft.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import software.amazon.awssdk.regions.Region;

@ConfigurationProperties(prefix = "aws.s3")
@Data
public class S3ClientConfigurarionProperties {

	private URI endpoint = URI.create("http://localhost:9091");

	private String accessKey = "Q3AM3UQ867SPQQA43P2F";

	private String secretKey = "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG";

	private String bucketName = "joylists.com";
	
	private Region region = Region.AWS_GLOBAL;
	
	// AWS S3 requires that file parts must have at least 5MB, except
    // for the last part. This may change for other S3-compatible services, so let't
    // define a configuration property for that
    private int multipartMinPartSize = 5*1024*1024;
	
}
