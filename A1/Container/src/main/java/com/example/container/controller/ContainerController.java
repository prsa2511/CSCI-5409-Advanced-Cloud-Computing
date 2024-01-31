package com.example.container.controller;

import com.example.container.ContainerApplication;
import com.example.container.model.MyRequest;
import com.example.container.model.MyResponse;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class ContainerController {

    @PostMapping("/calculate")
    public MyResponse processRequest(@RequestBody MyRequest myRequest) {
        MyResponse response = new MyResponse();
        String file = myRequest.getfile();
        String product = myRequest.getProduct();
        String filepath = "/shared/" + file;
        //String filepath = "D:\\Documents\\Dalhousie University\\Fall 23-24\\CSCI 5409 -  Advanced Cloud Computing\\A1-Docker\\" + file;
        System.out.println("In container 1: file is " + file + " and product is " + product);

        response.setFile(file);
        //Validate file and product
        //Check if file is null
        if (StringUtils.isEmpty(file)) {
            response.setFile(null);
            response.setError("Invalid JSON input.");
            return response;
        }
        //Check if file provided exists on mounted volume
        Path path = Paths.get(filepath);
        boolean fileExists = Files.exists(path);

        if (!fileExists) {
            response.setError("File not found.");
            return response;
        }
        System.out.println("This if after checking the file. If it reaches here then file exists");

        response = callContainer2(file,product);
        //response.setSum(res);
        return response;
    }

    public  MyResponse callContainer2(String file, String product) {
       // WebClient webClient = new WebClient();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"file\":\"" + file + "\",\"product\":\"" + product + "\"}";
        String url = "http://container2:9001/final_calculation";
        //String url = "http://localhost:8080/final_calculation";
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // Make the POST request and retrieve the response
        ResponseEntity<MyResponse> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                MyResponse.class
        );

        // Extract the response body from the ResponseEntity
        return responseEntity.getBody();
    }

}