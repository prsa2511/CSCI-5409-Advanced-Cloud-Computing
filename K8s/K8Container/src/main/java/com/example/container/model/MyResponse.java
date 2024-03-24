package com.example.container.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyResponse {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String file;

    private String error;

    private String message;

    private Integer sum;

    public MyResponse() {

    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getMessage() {
        return message;
    }

    public MyResponse(String file, String message) {
        this.file = file;
        this.message = message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
