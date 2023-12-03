package dronenet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author fatihsenel
 * date: 01.04.22
 */
public class Configuration {
    int width;
    int height;
    int cellWidth;
    int transmissionRange;
    int simulationStart;
    int simulationEnd;
    int timeInterval;

    public Configuration() {
    }

    public Configuration(int width, int height, int cellWidth, int transmissionRange, int simulationStart, int simulationEnd, int timeInterval) {
        this.width = width;
        this.height = height;
        this.cellWidth = cellWidth;
        this.transmissionRange = transmissionRange;
        this.simulationStart = simulationStart;
        this.simulationEnd = simulationEnd;
        this.timeInterval = timeInterval;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public void setCellWidth(int cellWidth) {
        this.cellWidth = cellWidth;
    }

    public int getTransmissionRange() {
        return transmissionRange;
    }

    public void setTransmissionRange(int transmissionRange) {
        this.transmissionRange = transmissionRange;
    }

    public int getSimulationStart() {
        return simulationStart;
    }

    public void setSimulationStart(int simulationStart) {
        this.simulationStart = simulationStart;
    }

    public int getSimulationEnd() {
        return simulationEnd;
    }

    public void setSimulationEnd(int simulationEnd) {
        this.simulationEnd = simulationEnd;
    }

    public int getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(int timeInterval) {
        this.timeInterval = timeInterval;
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
}
