package test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestMovement {
    public static void main(String[] args) {
        List<List<Double>> list = new ArrayList<>();

        try (Stream<String> lines = Files.lines(Paths.get("scenario1.movements"), Charset.defaultCharset())) {
            lines.forEachOrdered(line -> process(list, line));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    private static void process(List<List<Double>> list, String line) {
        list.add(Arrays.stream(line.split(" "))
                .map(Double::parseDouble)
                .collect(Collectors.toList()));
    }
}
