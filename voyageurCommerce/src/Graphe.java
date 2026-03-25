import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class Graphe {
    private ArrayList<Noeud> noeuds;
    private ArrayList<Arc> arcs;
    private boolean geoDistance;

    public Graphe() {
        this.noeuds = new ArrayList<>();
        this.arcs = new ArrayList<>();
        this.geoDistance = false;
    }

    public Graphe(int k) {
        this.noeuds = new ArrayList<>();
        this.arcs = new ArrayList<>();
        this.geoDistance = false;

        for (int i = 0; i < k; i++) {
            // On crée un nœud avec id 'i'
            // On initialise x et y a 0 par défaut (on appellera positionner() plus tard)
            this.noeuds.add(new Noeud(i, 0, 0));
        }
    }

    // Getters
    public ArrayList<Noeud> getNoeuds() {
        return noeuds;
    }

    public ArrayList<Arc> getArcs() {
        return arcs;
    }

    public Noeud getNoeudById(int id) {
        for (Noeud n : noeuds) {
            if (n.getId() == id) {
                return n;
            }
        }
        return null;
    }

    // Ajouter un arc entre deux noeuds
    public void addArc(Noeud n1, Noeud n2) {
        float distance = n1.distanceTo(n2, this.geoDistance);
        Arc arc = new Arc(n1, n2, distance);
        arcs.add(arc);
        n1.getArcs().add(arc);
    }

    // positionner les noeuds aleatoirement
    // avec les valeurs max a inserer
    public void positionner(float maxX, float maxY) {
        for (Noeud n : noeuds) {
            float x = (float) (Math.random() * maxX);
            float y = (float) (Math.random() * maxY);
            n.setX(x);
            n.setY(y);
        }
    }

    // Importer les noeuds depuis un fichier CSV (id;x;y)
    public void importer(String nomFichier) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(nomFichier));
            String premiereLigne = br.readLine();
            if (premiereLigne.equals("GEO")) {
                geoDistance = true;
            }
            String ligne;

            while ((ligne = br.readLine()) != null) {
                String[] p = ligne.split(";"); // au cas de besoin changer ; par , par exemple
                if (p.length == 3) { // on verifie que il y a exactement 3 valeurs
                    int id = Integer.parseInt(p[0]);
                    float abs = Float.parseFloat(p[1].trim().replace(",", ".")); //remplace une vi
                    float ord = Float.parseFloat(p[2].trim().replace(",", "."));

                    Noeud n = new Noeud(id, abs, ord);
                    noeuds.add(n);
                }
            }
        } finally {
            // gestion derreurs si fichier nest pas trouvé
            if (br != null) {
                br.close();
                System.err.println("erreur fichier");
            }
        }
    }


    // Creer un graphe complet (tous les noeuds connectes entre eux)
    public void complet() {
        for (int i = 0; i < noeuds.size(); i++) {
            for (int j = i + 1; j < noeuds.size(); j++) {
                addArc(noeuds.get(i), noeuds.get(j));
            }
        }
    }

    // Partiel avec arcExiste()
    // Version plus facile, mais peut etre lente avec beaucoup de noeuds...
    // Complexite : O(n*k*m) - m = nombre d'arcs
    public void partiel(int k) {
        // Pour chaque noeud du graphe
        for (Noeud n1 : noeuds) {
            // On doit le connecter a ses k plus proches voisins
            for (int i = 0; i < k; i++) {
                Noeud plusProche = null;
                float minDistance = Float.MAX_VALUE;

                // Cherche le plus proche qui n'est pas encore connecte a n1
                for (Noeud n2 : noeuds) {
                    // si c'est pas le meme noeud ET qu'il y a pas deja un arc entre eux
                    if (n1.getId() != n2.getId() && !arcExiste(n1, n2)) {
                        float dist = n1.distanceTo(n2, this.geoDistance);
                        // Garder celui avec la plus petite distance
                        if (dist < minDistance) {
                            minDistance = dist;
                            plusProche = n2;
                        }
                    }
                }

                // Si on a trouve un candidat, on le connecte
                if (plusProche != null) {
                    addArc(n1, plusProche);
                }
            }
        }
    }

    // Partiel plus optimisé avec HashSet
    // O(n²k) ,mieux que methode 1 si nous avons beaucoup d'arcs
    public void partielOptimise(int k) {
        // HashSet pour tracker les arcs crees
        HashSet<String> connectes = new HashSet<>();

        for (Noeud n1 : noeuds) {
            // Pour chaque noeud, on lui ajoute ses k plus proches voisins
            for (int i = 0; i < k; i++) {
                Noeud plusProche = null;
                float minDistance = Float.MAX_VALUE;

                for (Noeud n2 : noeuds) {
                    if (n1.getId() != n2.getId()) {
                        // Creation d'une clé unique pour le couple (non-orienté)
                        // min-max pour assurer que A-B et B-A donnent la meme clé
                        int id1 = Math.min(n1.getId(), n2.getId());
                        // min = le plus petit ID entre n1 et n2 Exemple : n1=5, n2=3 -> id1=3
                        int id2 = Math.max(n1.getId(), n2.getId()); // pareil pour max

                        String key = id1 + "-" + id2;
                        // Creer string "3-5" (toujours petit-grand) Arc 3->5 = Arc 5->3 (meme chose!)

                        // Verifier rapidement si cet arc existe deja (O(1) avec HashSet)
                        if (!connectes.contains(key)) {
                            float dist = n1.distanceTo(n2, this.geoDistance);
                            if (dist < minDistance) {
                                minDistance = dist;
                                plusProche = n2;
                            }
                        }
                    }
                }

                // Si on a trouve le plus proche, on le connecte et on le marque
                if (plusProche != null) {
                    int id1 = Math.min(n1.getId(), plusProche.getId());
                    int id2 = Math.max(n1.getId(), plusProche.getId());
                    String key = id1 + "-" + id2;

                    // Marquer comme connecte dans le HashSet
                    connectes.add(key);
                    // Creer l'arc dans le graphe
                    addArc(n1, plusProche);
                }
            }
        }
    }

    // Verifier si un arc existe entre n1 et n2 (non-oriente)
    // O(m) - lent si beaucoup d'arcs!
    private boolean arcExiste(Noeud n1, Noeud n2) {
        for (Arc a : arcs) {
            if ((a.getN1().getId() == n1.getId() && a.getN2().getId() == n2.getId()) ||
                    (a.getN1().getId() == n2.getId() && a.getN2().getId() == n1.getId())) {
                return true;
            }
        }
        return false;
    }
}
