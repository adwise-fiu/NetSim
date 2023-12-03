package geometry.delaunay;

import network.Gateway;

import java.util.ArrayList;

/**
 * @author : Fatih Senel
 *         Date: 4/6/11
 *         Time: 10:08 PM
 */
public class DTAdapter {
    ArrayList<Gateway> NodesList = new ArrayList<Gateway>();
    private Triangulation dt;                   // Delaunay triangulation
    private DT_Triangle initialTriangle;
    private static int initialSize = 100000;     // Size of initial triangle
    ArrayList<Gateway[]> delaunayTriangles;
    public DTAdapter(ArrayList<Gateway> nodesList) {
        NodesList = nodesList;
        initialTriangle = new DT_Triangle(
                new Pnt(-initialSize, -initialSize),
                new Pnt(initialSize, -initialSize),
                new Pnt(0, initialSize));
        dt = new Triangulation(initialTriangle);
        for (int i = 0; i < nodesList.size(); i++) {
            Gateway gateway = nodesList.get(i);
            dt.delaunayPlace(new Pnt(gateway.getX(), gateway.getY()));
        }
        delaunayTriangles = getAllDelaunayTriangles();
    }

    private ArrayList<Gateway[]> getAllDelaunayTriangles() {
        ArrayList<DT_Triangle> result = new ArrayList<DT_Triangle>();
        int counter = 0;
        for (DT_Triangle triangle : dt) {
            boolean skip = false;
            Pnt[] vertices = triangle.toArray(new Pnt[0]);
            for (int i = 0; i < vertices.length; i++) {
                Pnt vertex = vertices[i];
                if (vertex.coord(0) == initialSize || vertex.coord(0) == -initialSize || vertex.coord(1) == initialSize || vertex.coord(1) == -initialSize) {
                    skip = true;
                }
            }
            if (!skip) {
                result.add(triangle);
            }
        }
//        System.out.println("Num of Gateways: "+NodesList.size());
//        System.out.println("Num of DTs: "+dt.size());
//        System.out.println("Num of pruned DTs: "+result.size());
        ArrayList<Gateway[]> triangles = new ArrayList<Gateway[]>();

        for (int i = 0; i < result.size(); i++) {
            Gateway[] entry = new Gateway[3];
            for (int j = 0; j < result.get(i).getItems().size(); j++) {
                Pnt pnt = result.get(i).getItems().get(j);
                double x = pnt.coord(0);
                double y = pnt.coord(1);

                for (int k = 0; k < NodesList.size(); k++) {
                    Gateway gateway = NodesList.get(k);
                    if(gateway.getX()==x && gateway.getY()==y){
                        entry[j]=gateway;
                    }
                }
            }
            triangles.add(entry);
        }
        return triangles;
    }

    public ArrayList<Gateway[]> getDelaunayTriangles() {
        return delaunayTriangles;
    }
}
