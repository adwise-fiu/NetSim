package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fatihsenel
 * date: 02.11.23
 */
public class BinaryUtils {
    public static final char ZERO = '0';
    public static final char ONE = '1';

    public static String convertToBinaryWithLeadingZeros(int x, int totalBits) {
        String binaryString = Integer.toBinaryString(x);
        int leadingZeros = totalBits - binaryString.length();
        if (leadingZeros > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < leadingZeros; i++) {
                sb.append('0');
            }
            sb.append(binaryString);
            return sb.toString();
        } else {
            return binaryString;
        }
    }

    public static int[] findIndicesOf(String binaryString, char bit) {
        List<Integer> indicesList = new ArrayList<>();

        for (int i = binaryString.length() - 1; i >= 0; i--) {
            if (binaryString.charAt(i) == bit) {
                indicesList.add(binaryString.length() - i - 1);
            }
        }

        int[] indices = new int[indicesList.size()];
        for (int i = 0; i < indicesList.size(); i++) {
            indices[i] = indicesList.get(i);
        }

        return indices;
    }
}
