import java.util.ArrayList;

public class Noeud {
    private static final double TSPLIB_GEO_EARTH_RADIUS = 6378.388;

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
    public void setAbs(double abs) {
        this.abs = abs;
    }

    public void setOrd(double ord) {
        this.ord = ord;
    }

    private double geoTsplibToRadians(double valeur) {
        int degres = (int) valeur;
        double minutes = valeur - degres;
        double angle = degres + (5.0 * minutes / 3.0);
        return Math.PI * angle / 180.0;
    }

    // distance jusqua noeud n
    public double distanceTo(Noeud n, boolean isGeo) {
        if (!isGeo) {
            double dAbs = this.abs - n.getAbs();
            double dOrd = this.ord - n.getOrd();
            return (double) Math.sqrt(dAbs * dAbs + dOrd * dOrd);
        } else {
            // Format TSPLIB GEO:
            // - 1ere coordonnee = latitude
            // - 2eme coordonnee = longitude
            // - chaque valeur est exprimee en degres.minutes, pas en degres decimaux.
            double latitude1 = geoTsplibToRadians(this.abs);
            double longitude1 = geoTsplibToRadians(this.ord);
            double latitude2 = geoTsplibToRadians(n.getAbs());
            double longitude2 = geoTsplibToRadians(n.getOrd());

            double q1 = Math.cos(longitude1 - longitude2);
            double q2 = Math.cos(latitude1 - latitude2);
            double q3 = Math.cos(latitude1 + latitude2);

            double expression = 0.5 * ((1.0 + q1) * q2 - (1.0 - q1) * q3);
            expression = Math.max(-1.0, Math.min(1.0, expression));

            return (int) (TSPLIB_GEO_EARTH_RADIUS * Math.acos(expression) + 1.0);
        }
    }

}
