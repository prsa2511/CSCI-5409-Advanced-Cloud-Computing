package com.example.container.model;

public class MyRequest {
    private String file;
    private String data;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public MyRequest(String file, String data) {
        this.file = file;
        this.data = data;
    }
}
