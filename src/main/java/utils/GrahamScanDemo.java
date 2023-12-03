package utils;// Main class: GrahamScanDemo
// Project type: applet
// Arguments: 
// Compile command: javac -g -deprecation
/* $Id: GrahamScanDemo.java,v 1.1 1999/05/13 07:41:54 adm36 Exp $ */
/** GrahamScanDemo.java
 *  Demonstartion of Graham Scan Algorithm
 *  Using AWT
 *  @author Andrew McDonald
 *  @version Dec 1997
 */
/* This source code is copyright (c)1997 Andrew McDonald */
/* This source code is licensed under the GNU GPL version 2 or later */

import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class GrahamScanDemo extends Applet {

    int numpts;
    Plane space;

    static private String appletInfo =
            "Graham Scan Demonstration\nAndrew McDonald\nDec 1997";

    public String getAppletInfo() {
        return (appletInfo);
    }

    static private String[][] parameterInfo =
            {{"numpoints", "int", "Number of points"}};

    public String[][] getParameterInfo() {
        return (parameterInfo);
    }

    /*
    public static void main(String args[])
    {
      Frame appletFrame= new Frame("Graham Scan Demo");
      Applet gsApplet = new GrahamScanDemo();
      gsApplet.init();
      gsApplet.start();
      appletFrame.setLayout(new FlowLayout());
      appletFrame.add(gsApplet);
      appletFrame.setSize(500,300);
      appletFrame.show();
    }
    */

    public void init() {
        Button goButton = new Button("Go!");
        space = new Plane(getSize().width - 50, getSize().height - 10);
        numpts = getParameter("numpoints") == null ? 10 :
                new Integer(getParameter("numpoints"));
        // numpts = 10;
        goButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                space.emptyPt();
                for (int i = 0; i < numpts; i++) {
                    space.addPt();
                }
                space.repaint();
                space.GrahamScan();
                space.repaint();
            }


        });
        add(space);
        add(goButton);

        space.repaint();


    }

}

class Plane extends Canvas {

    Vector point = new Vector(2);
    Vector convexHull = new Vector(2);

    int height, width;

    Plane(int swidth, int sheight) {
        setSize(swidth, sheight);
        Dimension bigness = getSize();
        width = bigness.width;
        height = bigness.height;
        //    setBackground(Color.yellow);
    }

    public void paint(Graphics g) {
        g.setColor(Color.red);
        for (int i = 0; i < point.size(); i++) {
            Plot(g, (PPoint) point.elementAt(i));
        }
        Polygon ch = new Polygon();
        g.setColor(Color.blue);
        for (int i = 0; i < convexHull.size(); i++) {
            PPoint npoint = (PPoint) convexHull.elementAt(i);
            ch.addPoint(npoint.x, height - npoint.y);
            //      System.out.println("("+npoint.x+","+height+"-"+npoint.y+")");
        }
        g.drawPolygon(ch);

    }

    void Plot(Graphics g, PPoint p) {
        int x, y;
        x = p.x - 1;
        y = (height - p.y) - 1;

        g.drawOval(x, y, 2, 2);
    }

    public void emptyPt() {
        point.removeAllElements();
    }

    void addPt() {
        PPoint pt = new PPoint();
        pt.x = (int) (Math.random() * (width - 2) + 1);
        pt.y = (int) (Math.random() * (height - 2) + 1);

        pt.angle = 0;
        point.addElement(pt);
    }

    void GrahamScan() {
        Vector p = new Vector(point.size());
        convexHull.removeAllElements();

        PPoint alpha = new PPoint();
        alpha.x = width;
        alpha.y = height;
        int nalpha = -1;

        for (int i = 0; i < point.size(); i++) {
            PPoint npoint = ((PPoint) point.elementAt(i));
            if (npoint.y <= alpha.y) {
                if (npoint.y < alpha.y) {
                    alpha = npoint;
                    nalpha = i;
                } else {
                    if (npoint.x < alpha.x) {
                        alpha = npoint;
                        nalpha = i;
                    }
                }
            }
        }

        alpha.angle = 0;
        convexHull.addElement(alpha);

        for (int i = 0; i < point.size(); i++) {
            PPoint npoint = (PPoint) point.elementAt(i);
            // angle will be in range 0 - pi
            npoint.angle = (float) Math.atan2(npoint.y - alpha.y, npoint.x - alpha.x);

            boolean ptin = false;
            for (int j = 0; j < p.size(); j++) {
                if (i == nalpha) {
                    ptin = true;
                    break;
                }

                PPoint ppoint = (PPoint) p.elementAt(j);
                if (npoint.angle == ppoint.angle) {
                    // abandon nearest
                    if (Math.sqrt(ppoint.x * ppoint.x + ppoint.y * ppoint.y) <
                            Math.sqrt(npoint.x * npoint.x + npoint.y * npoint.y)) {
                        p.setElementAt(npoint, j);
                    }
                    ptin = true;
                    break;
                }
                if (npoint.angle < ppoint.angle) {
                    p.insertElementAt(npoint, j);
                    ptin = true;
                    break;
                }
            }
            if (!ptin) {
                p.addElement(npoint);
            }
        }

        // added all points to p
        // now go through them!

        nalpha = p.size();
        convexHull.addElement(p.elementAt(0));
        convexHull.addElement(p.elementAt(1));

        for (int i = 2; i < nalpha; i++) {
            PPoint p1, p2, pn;
            pn = (PPoint) p.elementAt(i);
            p1 = (PPoint) convexHull.elementAt(convexHull.size() - 2);
            p2 = (PPoint) convexHull.elementAt(convexHull.size() - 1);
            while (NonLeftTurn(p1, p2, pn, (PPoint) convexHull.firstElement())) {
                convexHull.removeElementAt(convexHull.size() - 1);
                p2 = p1;
                p1 = (PPoint) convexHull.elementAt(convexHull.size() - 2);
            }
            convexHull.addElement(pn);
        }

        // add the first element again to close polygon
//        convexHull.addElement(convexHull.firstElement());

    }

    boolean NonLeftTurn(PPoint p1, PPoint p2, PPoint p3, PPoint p0) {
        double l1, l2, l4, l5, l6, angle1, angle2, angle;

        l1 = Math.sqrt(Math.pow(p2.y - p1.y, 2) + Math.pow(p2.x - p1.x, 2));
        l2 = Math.sqrt(Math.pow(p3.y - p2.y, 2) + Math.pow(p3.x - p2.x, 2));
        l4 = Math.sqrt(Math.pow(p3.y - p0.y, 2) + Math.pow(p3.x - p0.x, 2));
        l5 = Math.sqrt(Math.pow(p1.y - p0.y, 2) + Math.pow(p1.x - p0.x, 2));
        l6 = Math.sqrt(Math.pow(p2.y - p0.y, 2) + Math.pow(p2.x - p0.x, 2));

        angle1 = Math.acos(((l2 * l2) + (l6 * l6) - (l4 * l4)) / (2 * l2 * l6));
        angle2 = Math.acos(((l6 * l6) + (l1 * l1) - (l5 * l5)) / (2 * l6 * l1));

        angle = (Math.PI - angle1) - angle2;

        if (angle <= 0.0) {
            return (true);
        } else {
            return (false);
        }
    }

}

class PPoint {
    int x, y;
    float angle;
}
