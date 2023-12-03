package dronenet.matching;

import dronenet.Drone;

import java.util.List;

public interface MatchingModel {
    List<Drone> doMatching(List<Drone> A, List<Drone> B);
}
