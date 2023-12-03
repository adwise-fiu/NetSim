package dronenet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author fatihsenel
 * date: 01.04.22
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TemporalHeatmapDto {
    List<double[][]> heatMaps;
    int[][] nonOccupiedCells;
    Configuration configuration;

    public TemporalHeatmapDto() {
    }

    public TemporalHeatmapDto(List<double[][]> heatMaps, int[][] nonOccupiedCells, Configuration configuration) {
        this.heatMaps = heatMaps;
        this.nonOccupiedCells = nonOccupiedCells;
        this.configuration = configuration;
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        try {
            json = mapper.writeValueAsString(this);
            //System.out.println(json);
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

    public static TemporalHeatmapDto loadFromJsonFile(String filename) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filename), TemporalHeatmapDto.class);
    }

    public List<double[][]> getHeatMaps() {
        return heatMaps;
    }

    public int[][] getNonOccupiedCells() {
        return nonOccupiedCells;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
