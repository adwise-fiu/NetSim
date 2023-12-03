package dronenet;

/**
 * @author fatihsenel
 * date: 20.04.22
 */
public class SolverOutputTimeDto {
    int time;
    SolverOutputDto solverOutputDto;

    public SolverOutputTimeDto() {
    }

    public SolverOutputTimeDto(int time, SolverOutputDto solverOutputDto) {
        this.time = time;
        this.solverOutputDto = solverOutputDto;
    }

    public int getTime() {
        return time;
    }

    public SolverOutputDto getSolverOutputDto() {
        return solverOutputDto;
    }
}
