package dronenet;

import dronenet.matching.MinimizeTotalMovementMatchingModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fatihsenel
 * date: 09.08.22
 */
public class DroneMatchingModelTest {
    public static void main(String[] args) {
        List<Drone> A = new ArrayList<>();
        List<Drone> B = new ArrayList<>();
        List<Drone> C = new ArrayList<>();

        for(int i=0;i<10;i++){
            A.add(new Drone(i));
            B.add(new Drone(i));
            C.add(new Drone(i));
        }

        A.get(0).setLocation(5,4);
        A.get(1).setLocation(2,6);
        A.get(2).setLocation(6,5);
        A.get(3).setLocation(3,7);
        A.get(4).setLocation(2,5);
        A.get(5).setLocation(5,6);
        A.get(6).setLocation(4,7);
//        A.get(7).setLocation(4,2);
//        A.get(8).setLocation(3,0);
//        A.get(9).setLocation(4,1);

        B.get(0).setLocation(6,3);
        B.get(1).setLocation(7,4);
        B.get(2).setLocation(6,5);
        B.get(3).setLocation(5,6);
        B.get(4).setLocation(4,7);
        B.get(5).setLocation(3,7);
        B.get(6).setLocation(2,6);
//        B.get(7).setLocation(0,2);
//        B.get(8).setLocation(5,3);
//        B.get(9).setLocation(6,4);

//        C.get(0).setLocation(5,1);
//        C.get(1).setLocation(6,2);
//        C.get(2).setLocation(7,3);
//        C.get(3).setLocation(0,2);
//        C.get(4).setLocation(2,3);
//        C.get(5).setLocation(1,1);
//        C.get(6).setLocation(4,3);
//        C.get(7).setLocation(3,2);
//        C.get(8).setLocation(4,2);
//        C.get(9).setLocation(2,1);

//        BottleneckAssignmentMatchingModel matchingModel = new BottleneckAssignmentMatchingModel();
        MinimizeTotalMovementMatchingModel matchingModel = new MinimizeTotalMovementMatchingModel();
        List<Drone> model = matchingModel.doMatching(A, B);
//        List<Drone> model2 = matchingModel.createModel(B, C);
        System.out.println();

    }
}
