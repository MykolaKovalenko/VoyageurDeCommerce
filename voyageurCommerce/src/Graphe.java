import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Graphe {
    private ArrayList<Noeud> noeuds;
    private ArrayList<Arc> arcs;
    // Set garontie que il y a pas de repetitionn
    private Map<String, Arc> arcsMap;

    private boolean isGeo;

    public Graphe() {
        this.noeuds = new ArrayList<>();
        this.arcs = new ArrayList<>();
        this.arcsMap = new HashMap<>();
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

    public double cout() {
        double somme = 0;
        for (Arc a : arcs) {
            somme += a.getValeur();
        }
        return somme;
    }

    public void supprimerNoeud(int id) {
        Noeud n = getNoeudById(id);
        if (n != null) {
            noeuds.remove(n);

            // 2. Supprimer tous les arcs dans arcsMap
            arcsMap.entrySet().removeIf(entry -> {
                String key = entry.getKey();
                // un arc est du type "id1-id2"
                return key.startsWith(n.getId() + "-") || key.endsWith("-" + n.getId());
            });
            for (Noeud autre : noeuds) {
                autre.getArcs().remove(n);
            }
        }
    }

    private boolean arcExiste(Noeud n1, Noeud n2) {
        int id1 = Math.min(n1.getId(), n2.getId());
        int id2 = Math.max(n1.getId(), n2.getId());
        String key = id1 + "-" + id2;

        return arcsMap.containsKey(key);
    }

    // Ajouter un arc entre deux noeuds
    public void addArc(Noeud n1, Noeud n2) {

        int id1 = Math.min(n1.getId(), n2.getId());
        int id2 = Math.max(n1.getId(), n2.getId());
        String key = id1 + "-" + id2;

        if (arcsMap.containsKey(key))
            return;

        double distance = n1.distanceTo(n2, this.isGeo);
        Arc arc = new Arc(n1, n2, distance);
        arcs.add(arc);
        arcsMap.put(key, arc);
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
            } else {
                throw new IllegalArgumentException(
                        "Format inconnu : la première ligne doit être 'GEO' ou '2D'.");
            }

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
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur de format : " + e.getMessage());
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

    public void partiel(int k) {
        if (k <= 0 || k >= noeuds.size()) {
            System.out.println("Le nombre des voisines inferier au nombre de tout les noeuds");
            return;
        }

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
                    double dist = n1.distanceTo(n2, this.isGeo);
                    // Garder celui avec la plus petite distance
                    if (dist < minDistance) {
                        minDistance = dist;
                        plusProche = n2;
                    }
                }

                if (plusProche != null) {
                    addArc(n1, plusProche);
                    nonVisites.remove(plusProche);
                }
            }
        }
    }

    public Graphe glouton() {
        ArrayList<Noeud> chemin = new ArrayList<>();
        ArrayList<Noeud> nonVisites = new ArrayList<>(noeuds);

        int index = (int) (Math.random() * nonVisites.size()); // genere un indexe aleatoire
        Noeud courant = nonVisites.get(index); // si on a les point A B C D et que indexe=2 alors courant = C
        chemin.add(courant);
        nonVisites.remove(courant);

        while (!nonVisites.isEmpty()) {
            Noeud plusProche = null;
            double minDistance = Double.MAX_VALUE;

            for (Noeud n : nonVisites) {
                double dist = courant.distanceTo(n, this.isGeo);
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
        resultat.isGeo = this.isGeo;

        resultat.getNoeuds().addAll(chemin); // pour ajouter les noeuds

        for (int i = 0; i < chemin.size() - 1; i++) { // enfin ajouter les arcs
            Noeud n1 = chemin.get(i);
            Noeud n2 = chemin.get(i + 1);
            resultat.addArc(new Noeud(n1.getId(), n1.getAbs(), n1.getOrd()), new Noeud(n2.getId(), n2.getAbs(), n2.getOrd()));
        }

        return resultat;
    }


    public void evaluerComplexite() {

        Graphe rechaufement = new Graphe();
        rechaufement.isGeo = this.isGeo;

        for (int i = 0; i < 100; i++) {
            Graphe tmp = new Graphe();
            tmp.isGeo = this.isGeo;

            for (Noeud n : this.noeuds) {
                tmp.getNoeuds().add(new Noeud(n.getId(), n.getAbs(), n.getOrd()));
        }

        tmp.glouton();
        }
        int nombreVilles = this.noeuds.size();
        int[] tailles = {
            nombreVilles / 8,
            nombreVilles / 4,
            nombreVilles / 2,
            nombreVilles
        };

        int repetitions = 100;

        for (int nb : tailles) {
            long total = 0;
            for (int r = 0; r < repetitions; r++) {

                Graphe tmp = new Graphe();
                tmp.isGeo = this.isGeo;

                for (int i = 0; i < nb; i++) {
                    Noeud original = this.noeuds.get(i);
                    tmp.getNoeuds().add(new Noeud(original.getId(), original.getAbs(), original.getOrd()));
                }

                long debut = System.nanoTime();
                tmp.glouton();
                long fin = System.nanoTime();

                total += (fin - debut);
            }

            long moyenne = total / repetitions;
            double tempsSec = moyenne / 1_000_000_000.0;

            System.out.println("nombre villes = " + nb + " | temps moyen = " + tempsSec + " s");
        }
    }

}
