import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class Graphe {
    private ArrayList<Noeud> noeuds;
    private ArrayList<Arc> arcs;
    private boolean isGeo;

    public Graphe() {
        this.noeuds = new ArrayList<>();
        this.arcs = new ArrayList<>();
        this.isGeo = false;
    }

    public Graphe(int k) {
        this();
        for (int i = 0; i < k; i++) {
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

    public void supprimerNoeud(int id) {
        Noeud n = getNoeudById(id);
        if (n != null) {
            noeuds.remove(n);
            arcs.removeIf(a -> a.getN1().equals(n) || a.getN2().equals(n));
        }
    }

    // Ajouter un arc entre deux noeuds
    public void addArc(Noeud n1, Noeud n2) {
        if (arcExiste(n1, n2))
            return;
        double distance = n1.distanceTo(n2, this.isGeo);
        Arc arc = new Arc(n1, n2, distance);
        arcs.add(arc);
        n1.getArcs().add(arc);
        n2.getArcs().add(arc);
    }

    // positionner les noeuds aleatoirement
    // avec les valeurs max a inserer
    public void positionner(double maxX, double maxY) {
        for (Noeud n : noeuds) {
            n.setAbs(Math.random() * maxX);
            n.setOrd(Math.random() * maxY);
        }
    }

    public void afficher() {
        System.out.println("===== GRAPHE =====");
        System.out.println(this.isGeo ? "type GEO" : "type 2D");

        System.out.println("\n--- Noeuds ---");
        for (Noeud n : noeuds) {
            System.out.println("Noeud " + n.getId() + " (" + n.getAbs() + ", " + n.getOrd() + ")");
        }

        System.out.println("\n--- Arcs ---");
        for (Arc a : arcs) {
            System.out.println(a.getN1().getId() + " <--> " + a.getN2().getId() + " | distance = " + a.getValeur());
        }

        System.out.println("\nNombre de noeuds : " + noeuds.size());
        System.out.println("Nombre d'arcs : " + arcs.size());
    }

    // Importer les noeuds depuis un fichier CSV (id;x;y)
    public void importer(String nomFichier) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(nomFichier));
            String premiereLigne = br.readLine();
            if (premiereLigne != null && premiereLigne.equals("GEO")) {
                isGeo = true;
            } else if (premiereLigne != null && premiereLigne.equals("2D")) {
                isGeo = false;
            } else
                return;

            String ligne;

            while ((ligne = br.readLine()) != null) {
                String[] p = ligne.split(";"); // au cas de besoin changer ; par , par exemple
                if (p.length == 3) { // on verifie que il y a exactement 3 valeurs
                    int id = Integer.parseInt(p[0]);
                    double abs = Double.parseDouble(p[1].trim().replace(",", ".")); // remplace une vi
                    double ord = Double.parseDouble(p[2].trim().replace(",", "."));

                    Noeud n = new Noeud(id, abs, ord);
                    noeuds.add(n);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
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
        if (k <= 0 || k >= noeuds.size())
            return;

        // Pour chaque noeud du graphe
        for (Noeud n1 : noeuds) {
            // pour chaque noeud on fait une liste des noeud qui on été verifiés si ils sont
            // proches ou pas
            ArrayList<Noeud> nonVisites = new ArrayList<>(this.noeuds);
            // on suprime des le debut cette liste le noeud de dont les plus proches
            // voisines on cherche
            nonVisites.remove(n1);
            // on
            for (int i = 0; i < k; i++) {
                Noeud plusProche = null;
                double minDistance = Double.MAX_VALUE;

                // Cherche le plus proche qui n'est pas encore connecte a n1
                for (Noeud n2 : nonVisites) {
                    // si c'est pas le meme noeud ET qu'il y a pas deja un arc entre eux
                    if (!arcExiste(n1, n2)) {
                        double dist = n1.distanceTo(n2, this.isGeo);
                        // Garder celui avec la plus petite distance
                        if (dist < minDistance) {
                            minDistance = dist;
                            plusProche = n2;
                        }
                    } else {
                        i++;
                    }
                }
                if (plusProche != null) {
                    addArc(n1, plusProche);
                    nonVisites.remove(plusProche);
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
            ArrayList<Noeud> nonVisites = new ArrayList<>(this.noeuds);
            nonVisites.remove(n1);
            // Pour chaque noeud, on lui ajoute ses k plus proches voisins
            for (int i = 0; i < k; i++) {
                Noeud plusProche = null;
                double minDistance = Double.MAX_VALUE;

                for (Noeud n2 : nonVisites) {
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
                            double dist = n1.distanceTo(n2, this.isGeo);
                            if (dist < minDistance) {
                                minDistance = dist;
                                plusProche = n2;
                            }
                        } else {
                            i++;
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
                    nonVisites.remove(plusProche);
                }
            }
        }
    }

    // Verifier si un arc existe entre n1 et n2 (non-oriente)
    // O(m) - lent si beaucoup d'arcs!
    private boolean arcExiste(Noeud n1, Noeud n2) {
        for (Arc a : arcs) {
            if ((a.getN1().equals(n1) && a.getN2().equals(n2)) ||
                    (a.getN1().equals(n2) && a.getN2().equals(n1))) {
                return true;
            }
        }
        return false;
    }


    public Graphe glouton() {
        ArrayList<Noeud> chemin = new ArrayList<>();
        ArrayList<Noeud> nonVisites = new ArrayList<>(noeuds);

        int index = (int)(Math.random() * nonVisites.size()); // genere un indexe aleatoire
        Noeud courant = nonVisites.get(index); // si on a les point A B C D et que indexe=2 alors courant = C
        chemin.add(courant);
        nonVisites.remove(courant);

        while (!nonVisites.isEmpty()) {
            Noeud plusProche = null;
            float minDistance = Float.MAX_VALUE;

            for (Noeud n : nonVisites) {
                float dist = courant.distanceTo(n);
                if (dist < minDistance) {
                    minDistance = dist;
                    plusProche = n;
                }
            }
            chemin.add(plusProche);
            nonVisites.remove(plusProche);
            courant = plusProche;
        }

    chemin.add(chemin.get(0));// comme ça on ferme le circuit

    Graphe resultat = new Graphe();

    resultat.getNoeuds().addAll(chemin); // pour ajouter les noeuds

    for (int i = 0; i < chemin.size() - 1; i++) { // enfin ajouter les arcs
        Noeud n1 = chemin.get(i);
        Noeud n2 = chemin.get(i + 1);
        resultat.addArc(n1, n2);
    }

    return resultat;
    }
}
