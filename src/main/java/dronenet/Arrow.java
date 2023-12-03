package dronenet;

import dronenet.gui.DroneNetGui;
import geometry.AnalyticGeometry;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * @author fatihsenel
 * date: 16.01.23
 * Hatali calismiyor
 */
public class Arrow {
    double xStart, yStart, xEnd, yEnd;
    double width = 10;
    double headLength = 15;
    double x2, y2;

    double ax1, ax2, ay1, ay2;
    double[][] polygon = new double[7][2];
    int[] polygonX = new int[7];
    int[] polygonY = new int[7];
    Color color = Color.BLACK;

    public Arrow(double x1, double y1, double x2, double y2) {
        this.xStart = x1;
        this.yStart = y1;
        this.xEnd = x2;
        this.yEnd = y2;


        double length = AnalyticGeometry.euclideanDistance(xStart, yStart, xEnd, yEnd) - headLength;
        Point2D coordinates = AnalyticGeometry.getCoordinates(xStart, yStart, xEnd, yEnd, length);
        this.x2 = coordinates.getX();
        this.y2 = coordinates.getY();

        //        polygon[0]
        Point2D[] start = AnalyticGeometry.getCoordinatesOnAPerpendicularLine(xEnd, yEnd, xStart, yStart, width);
        polygonX[0] = (int) start[0].getX();
        polygonY[0] = (int) start[0].getY();
        polygonX[6] = (int) start[1].getX();
        polygonY[6] = (int) start[1].getY();

        start = AnalyticGeometry.getCoordinatesOnAPerpendicularLine(xStart, yStart, this.x2, this.y2, width);
        polygonX[1] = (int) start[0].getX();
        polygonY[1] = (int) start[0].getY();
        polygonX[5] = (int) start[1].getX();
        polygonY[5] = (int) start[1].getY();

        start = AnalyticGeometry.getCoordinatesOnAPerpendicularLine(xStart, yStart, this.x2, this.y2, 4 * width);
        polygonX[2] = (int) start[0].getX();
        polygonY[2] = (int) start[0].getY();
        polygonX[4] = (int) start[1].getX();
        polygonY[4] = (int) start[1].getY();

        polygonX[3] = (int) xEnd;
        polygonY[3] = (int) yEnd;

//        for (int i = 0; i < polygonX.length; i++) {
//            polygonX[i] = (int) (DroneNetGui.left+ DroneNetGui.scale*polygonX[i]);
//            polygonY[i] = (int) (DroneNetGui.left+DroneNetGui.scale*polygonY[i]);
//
//        }
    }

    public void draw(Graphics g) {
//        int[] x = new int[7];
//        int[] y = new int[7];
//        for (int i = 0; i < polygonX.length; i++) {
//            x[i] = (int) (DroneNetGui.left + DroneNetGui.scale * polygonX[i]);
//            y[i] = (int) (DroneNetGui.left + DroneNetGui.scale * polygonY[i]);
//
//        }
//        g.setColor(Color.blue);
//        g.drawLine((int) (DroneNetGui.left + DroneNetGui.scale * xStart), (int) (DroneNetGui.left + DroneNetGui.scale * yStart), (int) (DroneNetGui.left + DroneNetGui.scale * xEnd), (int) (DroneNetGui.left + DroneNetGui.scale * yEnd));
    }
}