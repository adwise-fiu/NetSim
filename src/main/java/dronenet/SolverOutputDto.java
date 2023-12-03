package dronenet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fatihsenel
 * date: 07.04.22
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SolverOutputDto {
    SolutionDetails details;
    List<List<Integer>> drones = new ArrayList<>();
    List<List<Integer>> coverage = new ArrayList<>();
    List<List<Double>> heatmap = new ArrayList<>();
    Configuration configuration ;
    int time;

    public SolverOutputDto() {
    }

    public SolverOutputDto(SolutionDetails details, List<List<Integer>> drones, List<List<Integer>> coverage, List<List<Double>> heatmap, Configuration configuration, int time) {
        this.details = details;
        this.drones = drones;
        this.coverage = coverage;
        this.heatmap = heatmap;
        this.configuration = configuration;
        this.time = time;
    }

    public SolverOutputDto(List<List<Integer>> drones, List<List<Integer>> coverage, List<List<Double>> heatmap, Configuration configuration) {
        this.drones = drones;
        this.coverage = coverage;
        this.heatmap = heatmap;
        this.configuration = configuration;
    }

    public SolverOutputDto(List<List<Integer>> drones, List<List<Integer>> coverage, List<List<Double>> heatmap, Configuration configuration, int time) {
        this.drones = drones;
        this.coverage = coverage;
        this.heatmap = heatmap;
        this.configuration = configuration;
        this.time = time;
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        try {
            json = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public void printToFile(String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
        writer.write(toJSON());
        writer.close();
    }

    public static SolverOutputDto loadFromJsonFile(String filename) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filename), SolverOutputDto.class);
    }

    public List<List<Integer>> getDrones() {
        return drones;
    }

    public List<List<Integer>> getCoverage() {
        return coverage;
    }

    public List<List<Double>> getHeatmap() {
        return heatmap;
    }


    public SolutionDetails getDetails() {
        return details;
    }

    public void setDetails(SolutionDetails details) {
        this.details = details;
    }

    public int getTime() {
        return time;
    }

    public String getLabel(){
        return "Time at: "+ (time-1);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
