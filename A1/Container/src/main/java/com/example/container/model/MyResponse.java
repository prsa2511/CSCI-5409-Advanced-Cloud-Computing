package com.example.container.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class MyResponse {
    private String file;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private int sum;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
