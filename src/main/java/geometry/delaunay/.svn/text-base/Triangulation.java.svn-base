package geometry.delaunay;


/*
 * Copyright (c) 2005, 2007 by L. Paul Chew.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * A 2D Delaunay Triangulation (DT) with incremental site insertion.
 *
 * This is not the fastest way to build a DT, but it's a reasonable way to build
 * a DT incrementally and it makes a nice interactive display. There are several
 * O(n log n) methods, but they require that the sites are all known initially.
 *
 * A Triangulation is a Set of Triangles. A Triangulation is unmodifiable as a
 * Set; the only way to change it is to add sites (via delaunayPlace).
 *
 * @author Paul Chew
 *
 * Created July 2005. Derived from an earlier, messier version.
 *
 * Modified November 2007. Rewrote to use AbstractSet as parent class and to use
 * the Graph class internally. Tried to make the DT algorithm clearer by
 * explicitly creating a cavity.  Added code needed to find a Voronoi cell.
 *
 */
class Triangulation extends AbstractSet<DT_Triangle> {

    private DT_Triangle mostRecent = null;      // Most recently "active" triangle
    private Graph<DT_Triangle> triGraph;        // Holds triangles for navigation

    /**
     * All sites must fall within the initial triangle.
     * @param triangle the initial triangle
     */
    public Triangulation (DT_Triangle triangle) {
        triGraph = new Graph<DT_Triangle>();
        triGraph.add(triangle);
        mostRecent = triangle;
    }

    /* The following two methods are required by AbstractSet */

    @Override
    public Iterator<DT_Triangle> iterator () {
        return triGraph.nodeSet().iterator();
    }

    @Override
    public int size () {
        return triGraph.nodeSet().size();
    }

    @Override
    public String toString () {
        return "Triangulation with " + size() + " triangles";
    }

    /**
     * True iff triangle is a member of this triangulation.
     * This method isn't required by AbstractSet, but it improves efficiency.
     * @param triangle the object to check for membership
     */
    public boolean contains (Object triangle) {
        return triGraph.nodeSet().contains(triangle);
    }

    /**
     * Report neighbor opposite the given vertex of triangle.
     * @param site a vertex of triangle
     * @param triangle we want the neighbor of this triangle
     * @return the neighbor opposite site in triangle; null if none
     * @throws IllegalArgumentException if site is not in this triangle
     */
    public DT_Triangle neighborOpposite (Pnt site, DT_Triangle triangle) {
        if (!triangle.contains(site))
            throw new IllegalArgumentException("Bad vertex; not in triangle");
        for (DT_Triangle neighbor: triGraph.neighbors(triangle)) {
            if (!neighbor.contains(site)) return neighbor;
        }
        return null;
    }

    /**
     * Return the set of triangles adjacent to triangle.
     * @param triangle the triangle to check
     * @return the neighbors of triangle
     */
    public Set<DT_Triangle> neighbors(DT_Triangle triangle) {
        return triGraph.neighbors(triangle);
    }

    /**
     * Report triangles surrounding site in order (cw or ccw).
     * @param site we want the surrounding triangles for this site
     * @param triangle a "starting" triangle that has site as a vertex
     * @return all triangles surrounding site in order (cw or ccw)
     * @throws IllegalArgumentException if site is not in triangle
     */
    public List<DT_Triangle> surroundingTriangles (Pnt site, DT_Triangle triangle) {
        if (!triangle.contains(site))
            throw new IllegalArgumentException("Site not in triangle");
        List<DT_Triangle> list = new ArrayList<DT_Triangle>();
        DT_Triangle start = triangle;
        Pnt guide = triangle.getVertexButNot(site);        // Affects cw or ccw
        while (true) {
            list.add(triangle);
            DT_Triangle previous = triangle;
            triangle = this.neighborOpposite(guide, triangle); // Next triangle
            guide = previous.getVertexButNot(site, guide);     // Update guide
            if (triangle == start) break;
        }
        return list;
    }

    /**
     * Locate the triangle with point inside it or on its boundary.
     * @param point the point to locate
     * @return the triangle that holds point; null if no such triangle
     */
    public DT_Triangle locate (Pnt point) {
        DT_Triangle triangle = mostRecent;
        if (!this.contains(triangle)) triangle = null;

        // Try a directed walk (this works fine in 2D, but can fail in 3D)
        Set<DT_Triangle> visited = new HashSet<DT_Triangle>();
        while (triangle != null) {
            if (visited.contains(triangle)) { // This should never happen
                System.out.println("Warning: Caught in a locate loop");
                break;
            }
            visited.add(triangle);
            // Corner opposite point
            Pnt corner = point.isOutside(triangle.toArray(new Pnt[0]));
            if (corner == null) return triangle;
            triangle = this.neighborOpposite(corner, triangle);
        }
        // No luck; try brute force
        System.out.println("Warning: Checking all triangles for " + point);
        for (DT_Triangle tri: this) {
            if (point.isOutside(tri.toArray(new Pnt[0])) == null) return tri;
        }
        // No such triangle
        System.out.println("Warning: No triangle holds " + point);
        return null;
    }

    /**
     * Place a new site into the DT.
     * Nothing happens if the site matches an existing DT vertex.
     * @param site the new Pnt
     * @throws IllegalArgumentException if site does not lie in any triangle
     */
    public void delaunayPlace (Pnt site) {
        // Uses straightforward scheme rather than best asymptotic time

        // Locate containing triangle
        DT_Triangle triangle = locate(site);
        // Give up if no containing triangle or if site is already in DT
        if (triangle == null)
            throw new IllegalArgumentException("No containing triangle");
        if (triangle.contains(site)) return;

        // Determine the cavity and update the triangulation
        Set<DT_Triangle> cavity = getCavity(site, triangle);
        mostRecent = update(site, cavity);
    }

    /**
     * Determine the cavity caused by site.
     * @param site the site causing the cavity
     * @param triangle the triangle containing site
     * @return set of all triangles that have site in their circumcircle
     */
    private Set<DT_Triangle> getCavity (Pnt site, DT_Triangle triangle) {
        Set<DT_Triangle> encroached = new HashSet<DT_Triangle>();
        Queue<DT_Triangle> toBeChecked = new LinkedList<DT_Triangle>();
        Set<DT_Triangle> marked = new HashSet<DT_Triangle>();
        toBeChecked.add(triangle);
        marked.add(triangle);
        while (!toBeChecked.isEmpty()) {
            triangle = toBeChecked.remove();
            if (site.vsCircumcircle(triangle.toArray(new Pnt[0])) == 1)
                continue; // Site outside triangle => triangle not in cavity
            encroached.add(triangle);
            // Check the neighbors
            for (DT_Triangle neighbor: triGraph.neighbors(triangle)){
                if (marked.contains(neighbor)) continue;
                marked.add(neighbor);
                toBeChecked.add(neighbor);
            }
        }
        return encroached;
    }

    /**
     * Update the triangulation by removing the cavity triangles and then
     * filling the cavity with new triangles.
     * @param site the site that created the cavity
     * @param cavity the triangles with site in their circumcircle
     * @return one of the new triangles
     */
    private DT_Triangle update (Pnt site, Set<DT_Triangle> cavity) {
        Set<Set<Pnt>> boundary = new HashSet<Set<Pnt>>();
        Set<DT_Triangle> theTriangles = new HashSet<DT_Triangle>();

        // Find boundary facets and adjacent triangles
        for (DT_Triangle triangle: cavity) {
            theTriangles.addAll(neighbors(triangle));
            for (Pnt vertex: triangle) {
                Set<Pnt> facet = triangle.facetOpposite(vertex);
                if (boundary.contains(facet)) boundary.remove(facet);
                else boundary.add(facet);
            }
        }
        theTriangles.removeAll(cavity);        // Adj triangles only

        // Remove the cavity triangles from the triangulation
        for (DT_Triangle triangle: cavity) triGraph.remove(triangle);

        // Build each new triangle and add it to the triangulation
        Set<DT_Triangle> newTriangles = new HashSet<DT_Triangle>();
        for (Set<Pnt> vertices: boundary) {
            vertices.add(site);
            DT_Triangle tri = new DT_Triangle(vertices);
            triGraph.add(tri);
            newTriangles.add(tri);
        }

        // Update the graph links for each new triangle
        theTriangles.addAll(newTriangles);    // Adj triangle + new triangles
        for (DT_Triangle triangle: newTriangles)
            for (DT_Triangle other: theTriangles)
                if (triangle.isNeighbor(other))
                    triGraph.add(triangle, other);

        // Return one of the new triangles
        return newTriangles.iterator().next();
    }

    /**
     * Main program; used for testing.
     */
    public static void main (String[] args) {
        DT_Triangle tri =
            new DT_Triangle(new Pnt(-100,100), new Pnt(100,100), new Pnt(0,-100));
        System.out.println("DT_Triangle created: " + tri);
        Triangulation dt = new Triangulation(tri);
        System.out.println("DelaunayTriangulation created: " + dt);
        dt.delaunayPlace(new Pnt(0,0));
        dt.delaunayPlace(new Pnt(1,0));
        dt.delaunayPlace(new Pnt(0,1));
        System.out.println("After adding 3 points, we have a " + dt);
        DT_Triangle.moreInfo = true;
        System.out.println("Triangles: " + dt.triGraph.nodeSet());
    }
}