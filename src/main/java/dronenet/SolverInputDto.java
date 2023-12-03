package dronenet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fatihsenel
 * date: 01.07.22
 */
public class SolverInputDto {
    List<List<List<Double>>> heatMaps = new ArrayList<>();
    List<List<Integer>> nonOccupiedCells = new ArrayList<>();
    Configuration configuration ;

    public SolverInputDto(List<List<List<Double>>> heatMaps, List<List<Integer>> nonOccupiedCells, Configuration configuration) {
        this.heatMaps = heatMaps;
        this.nonOccupiedCells = nonOccupiedCells;
        this.configuration = configuration;
    }

    public SolverInputDto() {
    }

    public List<List<List<Double>>> getHeatMaps() {
        return heatMaps;
    }

    public void setHeatMaps(List<List<List<Double>>> heatMaps) {
        this.heatMaps = heatMaps;
    }

    public List<List<Integer>> getNonOccupiedCells() {
        return nonOccupiedCells;
    }

    public void setNonOccupiedCells(List<List<Integer>> nonOccupiedCells) {
        this.nonOccupiedCells = nonOccupiedCells;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
