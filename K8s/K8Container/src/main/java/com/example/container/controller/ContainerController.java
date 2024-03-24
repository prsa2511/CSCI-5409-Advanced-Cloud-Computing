package com.example.container.controller;

import com.example.container.model.CalculationRequest;
import com.example.container.model.MyRequest;
import com.example.container.model.MyResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


@RestController
public class ContainerController {
	//Testing for Recording
    private static final String PV_DIRECTORY = "/pratik_PV_dir/";
    //private static final String PV_DIRECTORY = "C:\\Program Files (x86)\\Google\\Cloud SDK\\Container1\\";

    @PostMapping("/store-file")
    public MyResponse processRequest(@RequestBody MyRequest request) {
        MyResponse response = new MyResponse();
        // Check if filename is provided
        if (request.getFile() == null || request.getFile().isEmpty()) {
            response.setFile(null);
            response.setError("Invalid JSON input.");
            return response;
        }

        String filePath = PV_DIRECTORY + request.getFile();

        // Delete the file if it exists
        File fileToDelete = new File(filePath);
        if (fileToDelete.exists()) {
            if (!fileToDelete.delete()) {
                response.setFile(request.getFile());
                response.setError("Failed to delete existing file.");
                return response;
            }
        }

        try {
            // Create file with the specified filename and content
            //String filePath = PV_DIRECTORY + request.getFile();
            System.out.println(filePath);
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(request.getData());
            fileWriter.close();

            response.setFile(request.getFile());
            response.setMessage("Success.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            response.setFile(request.getFile());
            response.setError("Error while storing the file to the storage.");
        }

        return response;
    }

    @PostMapping("/calculate")
    public MyResponse calculate(@RequestBody CalculationRequest request) {
        MyResponse response = new MyResponse();
        String filePath = PV_DIRECTORY + request.getFile(); // Change the path accordingly
        System.out.println(filePath);
        // Check if filename is provided
        if (request.getFile() == null || request.getFile().isEmpty()) {
            response.setFile(null);
            response.setError("Invalid JSON input.");
            return response;
        }

        // Check if the file exists
        System.out.println("Test");
        File file = new File(filePath);
        System.out.println("This?");
        if (!file.exists()) {
            System.out.println("Here?");
            response.setFile(request.getFile());
            response.setError("File not found.");
            return response;
        }
        System.out.println("Before call to 2");
        // Call method callContainer2 and return its response
        return callContainer2(request.getFile(), request.getProduct());
    }

    @GetMapping("/calculate")
    public MyResponse getcalculate(@RequestBody CalculationRequest request) {
        MyResponse response = new MyResponse();
        String filePath = PV_DIRECTORY + request.getFile(); // Change the path accordingly
        System.out.println(filePath);
        // Check if filename is provided
        if (request.getFile() == null || request.getFile().isEmpty()) {
            response.setFile(null);
            response.setError("Invalid JSON input.");
            return response;
        }

        // Check if the file exists
        System.out.println("Test");
        File file = new File(filePath);
        System.out.println("This?");
        if (!file.exists()) {
            System.out.println("Here?");
            response.setFile(request.getFile());
            response.setError("File not found.");
            return response;
        }
        System.out.println("Before call to 2");
        // Call method callContainer2 and return its response
        return callContainer2(request.getFile(), request.getProduct());
    }

    public MyResponse callContainer2(String file, String product) {
        String container2Host = System.getenv("CONTAINER2_SERVICE_HOST");
        String container2Port = System.getenv("CONTAINER2_SERVICE_PORT");
        String m2 = System.getenv("M2");
        System.out.println(m2 + " host: " + container2Host + " port: " + container2Port);
        // WebClient webClient = new WebClient();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"file\":\"" + file + "\",\"product\":\"" + product + "\"}";
        String url = "http://container2.default.svc.cluster.local:9001/calculation_final";
        //String url = "http://localhost:9001/calculation_final";
        //String url = String.format("http://%s:%s/calculation_final", container2Host, container2Port);
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