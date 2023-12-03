package test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<Node> example = objectMapper.readValue(new File("convert.json"), new TypeReference<List<Node>>(){});
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
