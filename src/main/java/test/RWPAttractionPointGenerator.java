package test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author fatihsenel
 * date: 20.06.22
 */
public class RWPAttractionPointGenerator {
    public static void main(String[] args) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter("rwp26.sh", false));
        for(int i=26;i<=50;i++) {
            String script = generate(i);
            pw.println(script);
        }
        pw.close();

    }

    private static String generate(int k) {
        int num = getRandomNumber(5, 8);

        int[] x = new int[num];
        int[] y = new int[num];
        int[] w = new int[num];

        List<Integer> list = new ArrayList<Integer>();
        list.add(1);
        list.add(2);
        list.add(4);
        java.util.Collections.shuffle(list);
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < num; i++) {
            int yp = getRandomNumber(0, 9);
            int xp = getRandomNumber(0, 11);
            String s = xp + "," + yp;
            if (set.contains(s) || (xp < 5 && yp <= 4)) {
                i--;
            } else {
                set.add(s);
                y[i] = yp;
                x[i] = xp;
                int j;
                if (xp >= 5 && yp <= 4) {
                    j = 0;
                } else if (xp < 5) {
                    j = 1;
                } else {
                    j = 2;
                }
                w[i] = list.get(j);
            }
        }

//        y[0] = getRandomNumber(5, 9);
//        y[1] = getRandomNumber(5, 9);
//        y[2] = getRandomNumber(5, 9);
//        y[3] = getRandomNumber(5, 9);
//        y[4] = getRandomNumber(0, 4);
//
//        x[0] = getRandomNumber(0, 5);
//        x[1] = getRandomNumber(0, 5);
//        x[2] = getRandomNumber(6, 11);
//        x[3] = getRandomNumber(6, 11);
//        x[4] = getRandomNumber(6, 11);
//        w[0] = 4;
//        w[1] = 4;
//        w[2] = 2;
//        w[3] = 2;
//        w[4] = 1;
        StringBuilder sb = new StringBuilder();
        sb.append("java -Xmx8g -Xss1g -jar target/bonnmotion.jar -f rwp RandomWaypoint -n 1500 -l 0.5 -h 2 -p 600 -o 3 -x 2200 -y 1800 -d 36000 -a ");
        for (int i = 0; i < num; i++) {
            int x1 = 100 + x[i] * 200;
            int y1 = 100 + y[i] * 200;
            sb.append(x1 + "," + y1 + "," + w[i] + ",50.0,50.0");
            if (i < num - 1) {
                sb.append(",");
            }
        }
        sb.append("\n");
        sb.append("gunzip rwp.movements.gz").append("\n");
        sb.append("mv rwp.movements ./mobility_data/rwp").append(k).append("_1500.movements").append("\n");
        sb.append("rm rwp*").append("\n");
        return sb.toString();
    }

    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}
