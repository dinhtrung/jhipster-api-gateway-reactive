package com.ft.web.rest.errors;
import java.util.Optional;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

@AllArgsConstructor
public class DownloadFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    private Optional<String> statusText = Optional.of("UNKNOWN");

    public DownloadFailedException(SdkResponse response) {

        SdkHttpResponse httpResponse = response.sdkHttpResponse();
        if (httpResponse != null) {
            this.statusCode = httpResponse.statusCode();
            this.statusText = httpResponse.statusText();
        } else {
            this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
            this.statusText = Optional.of("UNKNOWN");
        }
    }

}