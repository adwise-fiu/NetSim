package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author fatihsenel
 * date: 14.06.22
 */
public class FileComparison {
    private static boolean isEqual(Path firstFile, Path secondFile) {
        try {
            long size = Files.size(firstFile);
            if (size != Files.size(secondFile)) {
                return false;
            }

            if (size < 2048) {
                return Arrays.equals(Files.readAllBytes(firstFile),
                        Files.readAllBytes(secondFile));
            }

            // Compare character-by-character
            try (BufferedReader bf1 = Files.newBufferedReader(firstFile);
                 BufferedReader bf2 = Files.newBufferedReader(secondFile)) {
                int ch;
                while ((ch = bf1.read()) != -1) {
                    if (ch != bf2.read()) {
                        return false;
                    }
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {


//        String oldPath = "mobility_heatmaps/";
//        String newPath = "mobility_heatmaps/newHM";
        File oldPath = new File("mobility_heatmaps/");
        File newPath = new File("mobility_heatmaps/newHM/");

        String[] oldpathnames = oldPath.list();
        String[] newpathnames = newPath.list();
        List<String> filePrefix = new ArrayList<>();
        // For each pathname in the pathnames array
        for (String np : newpathnames) {
            for (String op : oldpathnames) {
                File firstFile = new File("mobility_heatmaps/" + op);
                File secondFile = new File("mobility_heatmaps/newHM/" + np);
                boolean equal = isEqual(firstFile.toPath(), secondFile.toPath());
                if (equal) {
                    System.out.println("Files are equal. old: " + op + "\tnp: " + np);
                    break;
                } else {
//                    System.out.println("Files are not equal.");
                }
            }

        }


    }
}
