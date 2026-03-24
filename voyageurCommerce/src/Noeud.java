import java.util.ArrayList;

public class Noeud {
    private int id;
    private float abs;
    private float ord;
    private ArrayList<Arc> arcs;

    public Noeud(int id, float abs, float ord) {
        this.id = id;
        this.abs = abs;
        this.ord = ord;
        this.arcs = new ArrayList<>();
    }

    // getters
    public int getId() {
        return id;
    }

    public float getAbs() {
        return abs;
    }

    public float getOrd() {
        return ord;
    }

    public ArrayList<Arc> getArcs() {
        return arcs;
    }

    // setters
    public void setX(float abs) {
        this.abs = abs;
    }

    public void setY(float ord) {
        this.ord = ord;
    }

    // distance jusqua noeud n
    public float distanceTo(Noeud n) {
        double dAbs = this.abs - n.getAbs();
        double dOrd = this.ord - n.getOrd();
        return (float) Math.sqrt(dAbs * dAbs + dOrd * dOrd);
    }

    public float distanceToGeo(Noeud n) {
        final double R = 6371.0; // rayon de la Terre en km

        double lat1 = Math.toRadians(this.abs);
        double lon1 = Math.toRadians(this.ord);
        double lat2 = Math.toRadians(n.getAbs());
        double lon2 = Math.toRadians(n.getOrd());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return (float) (R * c); // distance en km
    }

}
