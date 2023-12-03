package dronenet.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageViewer extends JFrame {
    private JLabel imageLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JList<String> imageList;
    private DefaultListModel<String> listModel;
    private String[] imageFiles;
    private String[] imageTitles;

    private int currentImageIndex;
    private JLabel titleLabel;

    public ImageViewer() {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        imageFiles = new String[]{
                "subplots_5_avg.png",
                "subplots_6_avg.png",
                "subplots_7_avg.png",
                // Add more image file paths as needed
        };

        imageTitles = new String[]{
                "Average of 5 Drones",
                "Average of 6 Drones",
                "Average of 7 Drones",
        };
        currentImageIndex = 0;

        titleLabel = new JLabel("<html><div style='font-size: 16px; text-align: center; vertical-align: middle;'>Title Goes Here</div></html>");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setVerticalAlignment(JLabel.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        add(imageLabel, BorderLayout.CENTER);

        prevButton = new JButton("Previous");
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPreviousImage();
            }
        });

        nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextImage();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        add(buttonPanel, BorderLayout.SOUTH);

        listModel = new DefaultListModel<>();
        imageList = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(imageList);
        listScrollPane.setPreferredSize(new Dimension(150, 0));
        add(listScrollPane, BorderLayout.WEST);

        imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        imageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                currentImageIndex = imageList.getSelectedIndex();
                loadImage();
            }
        });

        loadImages();
    }

    private void loadImages() {
        listModel.clear();
        for (String imageFile : imageFiles) {
            listModel.addElement(imageFile);
        }
        loadImage();
    }

    private void loadImage() {
        try {
            BufferedImage image = ImageIO.read(new File(imageFiles[currentImageIndex]));
            int newWidth = 800; // Set the desired width
            int newHeight = (int) (((double) newWidth / image.getWidth()) * image.getHeight());
            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaledImage);
            imageLabel.setIcon(icon);
            titleLabel.setText("<html><div style='font-size: 16px; text-align: center; vertical-align: middle;'>" + imageTitles[currentImageIndex] + "</div></html>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPreviousImage() {
        if (currentImageIndex > 0) {
            currentImageIndex--;
            loadImage();
            imageList.setSelectedIndex(currentImageIndex);
        }
    }

    private void showNextImage() {
        if (currentImageIndex < imageFiles.length - 1) {
            currentImageIndex++;
            loadImage();
            imageList.setSelectedIndex(currentImageIndex);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ImageViewer viewer = new ImageViewer();
                viewer.setSize(1200, 1000);
                viewer.setVisible(true);
            }
        });
    }
}
