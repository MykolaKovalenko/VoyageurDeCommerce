import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class Graphe {
    private ArrayList<Noeud> noeuds;
    private ArrayList<Arc> arcs;

    public Graphe() {
        this.noeuds = new ArrayList<>();
        this.arcs = new ArrayList<>();
    }

    public Graphe(int k) {
        this.noeuds = new ArrayList<>();
        this.arcs = new ArrayList<>();

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
        float distance = n1.distanceTo(n2);
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
            String ligne;

            while ((ligne = br.readLine()) != null) {
                String[] p = ligne.split(";"); // au cas de besoin changer ; par , par exemple
                if (p.length == 3) { // on verifie que il y a exactement 3 valeurs
                    int id = Integer.parseInt(p[0]);
                    float x = Float.parseFloat(p[1]);
                    float y = Float.parseFloat(p[2]);

                    Noeud n = new Noeud(id, x, y);
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
                        float dist = n1.distanceTo(n2);
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
                        // Creer string "3-5" (toujours petit-grand)  Arc 3->5 = Arc 5->3 (meme chose!)

                        // Verifier rapidement si cet arc existe deja (O(1) avec HashSet)
                        if (!connectes.contains(key)) {
                            float dist = n1.distanceTo(n2);
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
<<<<<<< Updated upstream
=======

    public void evaluerComplexite(boolean modePartiel, int k) {

        // Nombre total de villes disponibles dans ce graphe.
        int nombreVilles = this.noeuds.size();
        // Tailles cibles pour l'experimentation.
        int[] tailles = {10, 100, 500, 1000};

        // Filtrer les tailles valides selon la taille reelle du graphe.
        ArrayList<Integer> taillesValides = new ArrayList<>();
        for (int t : tailles) {
            if (t >= 2 && t <= nombreVilles) {
                taillesValides.add(t);
            }
        }
        if (!taillesValides.contains(nombreVilles)) {
            // On ajoute toujours la taille maximale reelle pour avoir le cas complet.
            taillesValides.add(nombreVilles);
        }

        // Nombre de repetitions pour lisser les variations dues a l'aleatoire.
        int repetitions = 100;

        for (int nb : taillesValides) {
            long total = 0;
            for (int r = 0; r < repetitions; r++) {

                // Important: preparation hors chrono.
                // On mesure uniquement le temps de glouton.
                Graphe tmp = sousGraphe(nb, modePartiel, k);

                long debut = System.nanoTime();
                tmp.glouton();
                long fin = System.nanoTime();

                total += (fin - debut);
            }

            long moyenne = total / repetitions;
            double tempsSec = moyenne / 1000000000.0;

            // Affichage final de la performance moyenne pour cette taille.
            String mode = modePartiel ? "partiel(k=" + k + ")" : "complet";
            System.out.println("glouton | mode = " + mode + " | nombre villes = " + nb + " | repetitions = " + repetitions
                    + " | temps moyen = " + tempsSec + " s");
        }
    }

    public Graphe kruskal() {
    // Graphe qui contiendra l'arbre couvrant minimal
    Graphe mst = new Graphe();
    mst.isGeo = this.isGeo;

    // On copie les noeuds (mais pas les arcs)
    mst.getNoeuds().addAll(this.noeuds);

    // 1️⃣ Trier les arcs par distance croissante
    ArrayList<Arc> arcsTries = new ArrayList<>(this.arcs);
    arcsTries.sort((a, b) -> Double.compare(a.getValeur(), b.getValeur()));

    // 2️⃣ Structure Union-Find (pour éviter les cycles)
    Map<Noeud, Noeud> parent = new HashMap<>();

    // Au début chaque noeud est son propre parent
    for (Noeud n : this.noeuds) {
        parent.put(n, n);
    }

    // Fonction pour trouver la racine d’un noeud
    java.util.function.Function<Noeud, Noeud> find = new java.util.function.Function<>() {
        public Noeud apply(Noeud n) {
            // On remonte jusqu'au parent final
            while (parent.get(n) != n) {
                n = parent.get(n);
            }
            return n;
        }
    };

    // 3️⃣ Parcourir les arcs triés
    for (Arc a : arcsTries) {

        // Trouver les racines des deux noeuds
        Noeud r1 = find.apply(a.getN1());
        Noeud r2 = find.apply(a.getN2());

        // Si racines différentes → pas de cycle
        if (r1 != r2) {

            // On ajoute l’arc dans le MST
            mst.addArc(a.getN1(), a.getN2());

            // On fusionne les deux ensembles
            parent.put(r1, r2);
        }
    }

    return mst;
}

private void dfs(Noeud n, ArrayList<Noeud> visite, ArrayList<Noeud> parcours) {

    // On marque le noeud comme visité
    visite.add(n);

    // On l'ajoute au parcours
    parcours.add(n);

    // On regarde tous ses voisins
    for (Arc a : n.getArcs()) {

        // Trouver le voisin (l'autre extrémité de l'arc)
        Noeud voisin = a.getN1().equals(n) ? a.getN2() : a.getN1();

        // Si pas encore visité → on continue en profondeur
        if (!visite.contains(voisin)) {
            dfs(voisin, visite, parcours);
        }
    }
}

public Graphe mstApprox() {

    // 1️⃣ Construire le MST
    Graphe mst = this.kruskal();

    // 2️⃣ DFS pour obtenir un parcours
    ArrayList<Noeud> parcours = new ArrayList<>();
    ArrayList<Noeud> visite = new ArrayList<>();

    // On choisit un noeud de départ
    Noeud depart = mst.getNoeuds().get(0);

    mst.dfs(depart, visite, parcours);

    // 3️⃣ Supprimer les doublons → chemin hamiltonien
    ArrayList<Noeud> chemin = new ArrayList<>();

    for (Noeud n : parcours) {
        // On garde seulement la première apparition
        if (!chemin.contains(n)) {
            chemin.add(n);
        }
    }

    // 4️⃣ Fermer le cycle (revenir au départ)
    chemin.add(chemin.get(0));

    // 5️⃣ Construire le graphe résultat
    Graphe resultat = new Graphe();
    resultat.isGeo = this.isGeo;

    resultat.getNoeuds().addAll(chemin);

    // Ajouter les arcs du chemin
    for (int i = 0; i < chemin.size() - 1; i++) {
        resultat.addArc(chemin.get(i), chemin.get(i + 1));
    }

    return resultat;
}

>>>>>>> Stashed changes
}
