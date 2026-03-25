import java.util.ArrayList;

public class Noeud {
    private int id;
    private double abs;
    private double ord;
    private ArrayList<Arc> arcs;

    public Noeud(int id, double abs, double ord) {
        this.id = id;
        this.abs = abs;
        this.ord = ord;
        this.arcs = new ArrayList<>();
    }

    // getters
    public int getId() {
        return id;
    }

    public double getAbs() {
        return abs;
    }

    public double getOrd() {
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
    public float distanceTo(Noeud n, boolean isGeo) {
        if (!isGeo) {
            double dAbs = this.abs - n.getAbs();
            double dOrd = this.ord - n.getOrd();
            return (float) Math.sqrt(dAbs * dAbs + dOrd * dOrd);
        } else {
            // Rayon de la Terre en kilomètres
            final double R = 6371.0;

            // Conversion degrés → radians
            double lat1 = Math.toRadians(this.abs);
            double lon1 = Math.toRadians(this.ord);
            double lat2 = Math.toRadians(n.getAbs());
            double lon2 = Math.toRadians(n.getOrd());

            // Différences
            double dLat = lat2 - lat1;
            double dLon = lon2 - lon1;

            // Formule de Haversine
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(lat1) * Math.cos(lat2)
                            * Math.sin(dLon / 2) * Math.sin(dLon / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            // Distance finale
            return (float) (R * c);
        }
    }

}
