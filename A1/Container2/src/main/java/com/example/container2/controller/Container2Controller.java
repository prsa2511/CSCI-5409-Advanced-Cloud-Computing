package com.example.container2.controller;

import com.example.container2.model.MyRequest;
import com.example.container2.model.MyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class Container2Controller {

    @PostMapping("/final_calculation")
    public MyResponse finalCalculation(@RequestBody MyRequest myRequest){
            MyResponse myResponse = new MyResponse();
            String file = myRequest.getfile();
            String product = myRequest.getProduct();
        System.out.println("In container 2, file and product are " + file + "& " + product);
            myResponse.setFile(file);
        String filepath = "/shared/" + file;
        //String filepath = "D:\\Documents\\Dalhousie University\\Fall 23-24\\CSCI 5409 -  Advanced Cloud Computing\\A1-Docker\\" + file;

        //Check if file corresponding to file is in CSV format
        try {
            if (!isValidCsv(filepath)) {
                myResponse.setError("Input file not in CSV format.");
                return myResponse;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            int sum = calculateSum(filepath, product);
            myResponse.setSum((sum));
            System.out.println("Sum of amounts for '" + product + "': " + sum);
        } catch (IOException e) {
            e.printStackTrace();
        }
            return myResponse;
    }

    private static boolean isValidCsv(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            // Read the header
            String header = reader.readLine();
            if (header == null || !header.trim().equalsIgnoreCase("product,amount")) {
                return false; // Incorrect header
            }

            // Read and validate each data row
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 2) {
                    return false; // Invalid number of columns
                }

                // Validate additional criteria if needed
                String product = parts[0].trim();
                String amount = parts[1].trim();
                // Add your additional validation logic here

                // Example: Check if 'amount' is a numeric value
                if (!amount.matches("\\d+")) {
                    return false; // 'amount' is not a valid numeric value
                }
            }

            return true; // All rows are valid
        }
    }

    private static int calculateSum(String filePath, String targetProduct) throws IOException {
        int sum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip the header line
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String product = parts[0].trim();
                    int amount = Integer.parseInt(parts[1].trim());

                    // Check if the product matches the target
                    if (product.equalsIgnoreCase(targetProduct)) {
                        sum += amount;
                    }
                }
            }
        }

        return sum;
    }
}
