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

    // getters basiques
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

    // setters basiques
    public void setAbs(double abs) {
        this.abs = abs;
    }

    public void setOrd(double ord) {
        this.ord = ord;
    }

    // Convertit une valeur en format TSPLIB (degres.minutes) en radians
    // Ex: 48.30 signifie 48 degres et 30 minutes, pas 48,30 degres
    private double geoTsplibToRadians(double valeur) {
        // partie entiere = degres, partie decimale = minutes
        int degres = (int) valeur;
        double minutes = valeur - degres;
        // convertit en degres decimaux: 30 minutes = 0.5 degre
        double angle = degres + (5.0 * minutes / 3.0);
        // convertit en radians
        return Math.PI * angle / 180.0;
    }

    // calcule la distance entre ce noeud et n
    public double distanceTo(Noeud n, boolean isGeo) {
        if (!isGeo) {
            double dAbs = this.abs - n.getAbs();
            double dOrd = this.ord - n.getOrd();
            return (double) Math.sqrt(dAbs * dAbs + dOrd * dOrd);
        } else {
            // format TSPLIB GEO:
            // - 1ere coordonnee = latitude
            // - 2eme coordonnee = longitude
            // - les valeurs sont en degres.minutes (pas des degres decimaux classiques)

            // conversion des deux noeuds en radians
            double latitude1 = geoTsplibToRadians(this.abs);
            double longitude1 = geoTsplibToRadians(this.ord);
            double latitude2 = geoTsplibToRadians(n.getAbs());
            double longitude2 = geoTsplibToRadians(n.getOrd());

            // formule de distance spherique TSPLIB (variante Haversine)
            double q1 = Math.cos(longitude1 - longitude2); // ecart en longitude
            double q2 = Math.cos(latitude1 - latitude2);   // ecart en latitude
            double q3 = Math.cos(latitude1 + latitude2);   // position sur le globe

            // calcule le cosinus de l angle entre les deux points vus du centre de la Terre
            double expression = 0.5 * ((1.0 + q1) * q2 - (1.0 - q1) * q3);
            // clamp entre -1 et 1 pour eviter une erreur dans acos (erreurs d arrondi flottant)
            expression = Math.max(-1.0, Math.min(1.0, expression));

            // rayon * angle = distance en km, arrondie a l entier superieur (norme TSPLIB)
            return (int) (TSPLIB_GEO_EARTH_RADIUS * Math.acos(expression) + 1.0);
        }
    }

}
