package dronenet;

/**
 * @author fatihsenel
 * date: 31.05.22
 */
public class HeatmapGen {

    public static void main(String[] args) {
        DroneNet dn = new DroneNet();
        String path = "mobility_data/";
        Configuration config = new Configuration(2200, 1800, 200, 285, 0, 600, 20);
        for (int i = 26; i <= 50; i++) {
            String inputMovementFileName = path+"rwp" + i + "_1500.movements";
            System.out.println(inputMovementFileName);
            dn.readMobilityFile(inputMovementFileName, config);

        }

//        dn.readMobilityFile(path+"rwp.movements", config);

    }
}
