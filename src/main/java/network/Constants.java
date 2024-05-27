package network;

/**
 * @author : Fatih Senel
 * Date: Nov 18, 2007
 * Time: 8:45:42 PM
 */
public interface Constants {
    double finalx = 2000;
    double finaly = 2000;
    int FrameWidth = 1000;
    int FrameHeight = 700;

    int ApplicationAreaWidth = 1500;            // For spider use Width=1000 and Height=600, For Festa use W=1500 and H=1500
    int ApplicationAreaHeight = 1500;
//    double ActorTransmissionRange = 285;
    double ActorTransmissionRange = 200;
    double RNTransmissionRange = ActorTransmissionRange;
    double ActorActionRange = 45;
    double maxDroneSpeed = 20.83; // 75km/h ~= 1250 meter/minutes = 20.83 m/s

    /**
     * Size of the grid cell
     */
    double squareSize = RNTransmissionRange / 2;
//    final double squareSize = 400;

    /**
     * Labels of Spider
     */
    int FIRST = 1;
    int CUT = 2;
    int RING_ENTRANCE = 3;
    int RING = 4;

    int Inf = 100000;

    // kLCA Constants
    int MAX_N_SEGM = 18;
    int MAX_N_SP = MAX_N_SEGM - 2;
    int MAX_EL_ST = ((MAX_N_SEGM + MAX_N_SP) * (MAX_N_SEGM + MAX_N_SP - 1) / 2) * 2;
    int k_restricted = 3;

    double err = 0.01;

    boolean LOG_TRIANGLE = false;

    // number of mobile data collectors
//    final int MDTC = 5;
    // number of static relay nodes
    int SRN = 1;

}
