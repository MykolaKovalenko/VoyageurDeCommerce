import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Graphe {
    // Liste des villes.
    private ArrayList<Noeud> noeuds;
    // Liste de tous les arcs du graphe.
    private ArrayList<Arc> arcs;
    // Index rapide des arcs avec cle "idMin-idMax" pour eviter les doublons.
    private Map<String, Arc> arcsMap;

    // true: coordonnees GEO (Haversine), false: distance euclidienne.
    private boolean isGeo;

    // Graphe vide par defaut.
    public Graphe() {
        this.noeuds = new ArrayList<>();
        this.arcs = new ArrayList<>();
        this.arcsMap = new HashMap<>();
        this.isGeo = false;
    }

    // Constructeur utilitaire: cree k noeuds "vides" (position 0,0).
    // Utile pour des tests rapides.
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

    // Recherche lineaire d'un noeud par identifiant.
    public Noeud getNoeudById(int id) {
        for (Noeud n : noeuds) {
            if (n.getId() == id) {
                return n;
            }
        }
        return null;
    }

    // Cout total du graphe courant = somme des valuations de ses arcs.
    // Sur un graphe resultat de glouton, cela correspond a la longueur du circuit.
    public double cout() {
        double somme = 0;
        for (Arc a : arcs) {
            somme += a.getValeur();
        }
        return somme;
    }

    // Supprime un noeud + tous les arcs incidents.
    // Cette methode est utile pour des manipulations avancees de graphe.
    public void supprimerNoeud(int id) {
        Noeud n = getNoeudById(id);
        if (n != null) {
            noeuds.remove(n);

            // Supprimer tous les arcs connectes a ce noeud
            arcsMap.entrySet().removeIf(entry -> {
                String key = entry.getKey();
                // un arc est du type "id1-id2"
                return key.startsWith(n.getId() + "-") || key.endsWith("-" + n.getId());
            });

            // Supprimer les arcs des autres noeuds
            for (Noeud autre : noeuds) {
                autre.getArcs().removeIf(arc -> arc.getN1().getId() == id || arc.getN2().getId() == id);
            }
        }
    }

    // Ajoute un arc non oriente entre n1 et n2, valorise par leur distance.
    // Si l'arc existe deja, la methode ne fait rien.
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

    // Positionne chaque noeud aleatoirement dans le rectangle [0,maxX] x [0,maxY].
    // Cette methode sert surtout quand les noeuds ne viennent pas d'un CSV.
    public void positionner(double maxX, double maxY) {
        for (Noeud n : noeuds) {
            n.setAbs(Math.random() * maxX);
            n.setOrd(Math.random() * maxY);
        }
    }

    // Affichage debug complet du graphe.
    // Pratique pour verifier visuellement les noeuds et leurs arcs.
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

    // ------------------------------
    // Chargement depuis CSV
    // ------------------------------

    // Importe les noeuds depuis un CSV de format:
    // 1ere ligne: GEO ou 2D
    // lignes suivantes: id;abs;ord
    public void importer(String nomFichier) {
        try (BufferedReader br = new BufferedReader(new FileReader(nomFichier))) {
            // La 1ere ligne du fichier indique le type de distance a utiliser.
            String premiereLigne = br.readLine();
            if (premiereLigne != null && premiereLigne.equals("GEO")) {
                isGeo = true;
            } else if (premiereLigne != null && premiereLigne.equals("2D")) {
                isGeo = false;
            } else {
                throw new IllegalArgumentException("La premiere ligne doit etre 'GEO' ou '2D'");
            }

            String ligne;
            // on lit toutes les lignes jusque a la fin du fichier
            while ((ligne = br.readLine()) != null) {
                // on divise la ligne en morceaux avec ;
                String[] p = ligne.split(";");
                // on verifie qu'il y a exactement 3 valeurs
                if (p.length == 3) {
                    // Format attendu: id;abs;ord
                    int id = Integer.parseInt(p[0]);
                    // remplace une virgule par un point, ex : 95,59 -> 95.59
                    // si deja avec un point donc on ne touche pas, ex 95.59 -> 95.59
                    double abs = Double.parseDouble(p[1].trim().replace(",", "."));
                    double ord = Double.parseDouble(p[2].trim().replace(",", "."));

                    // Chaque ligne devient un objet Noeud.
                    noeuds.add(new Noeud(id, abs, ord));
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur de format : " + e.getMessage());
        }
    }

    // ------------------------------
    // Types de graphes
    // ------------------------------

    // Cree un graphe complet: chaque paire de noeuds est reliee.
    // Nombre d'arcs attendu: n*(n-1)/2.
    public void complet() {
        for (int i = 0; i < noeuds.size(); i++) {
            for (int j = i + 1; j < noeuds.size(); j++) {
                addArc(noeuds.get(i), noeuds.get(j));
            }
        }
    }

    public void partiel(int k) {
        if (k <= 1 || k >= noeuds.size()) {
            System.out.println("Le nombre des voisines doit etre 1 et Nombre_Villes_Total - 1 ");
            return;
        }

        // Pour chaque noeud, on ajoute des arcs vers ses k plus proches voisins.
        for (Noeud n1 : noeuds) {
            // Liste des candidats encore disponibles autour de n1.
            ArrayList<Noeud> nonVisites = new ArrayList<>(this.noeuds);
            // On retire n1 lui-meme: un noeud n'est pas voisin de lui-meme.
            nonVisites.remove(n1);

            // Selection gloutonne locale des k voisins les plus proches.
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
                    // On ajoute l'arc (si absent) puis on retire ce voisin des candidats
                    // pour obtenir les k plus proches distincts.
                    addArc(n1, plusProche);
                    nonVisites.remove(plusProche);
                }
            }
        }
    }

    // ------------------------------
    // Outil interne
    // ------------------------------

    // Recupere l'arc entre deux noeuds s'il existe, sinon null.
    private Arc getArc(Noeud n1, Noeud n2) {
        // Normalisation des ids pour representer un arc non oriente.
        int id1 = Math.min(n1.getId(), n2.getId());
        int id2 = Math.max(n1.getId(), n2.getId());
        return arcsMap.get(id1 + "-" + id2);
    }

    public Graphe glouton() {
        // Cas limite: pas de noeuds, pas de tour.
        if (noeuds.isEmpty()) {
            return null;
        }

        // chemin: ordre de visite construit par l'algorithme.
        ArrayList<Noeud> chemin = new ArrayList<>();
        // nonVisites: ensemble des noeuds qu'il reste a visiter.
        ArrayList<Noeud> nonVisites = new ArrayList<>(noeuds);

        // Le depart est aleatoire.
        // Cela evite de toujours produire exactement le meme tour.
        int index = (int) (Math.random() * nonVisites.size());
        Noeud courant = nonVisites.get(index);
        chemin.add(courant);
        nonVisites.remove(courant);

        // A chaque etape on va au plus proche voisin non visite.
        while (!nonVisites.isEmpty()) {
            Noeud plusProche = null;
            double minDistance = Double.MAX_VALUE;

            // Parcourir les arcs du nœud courant pour trouver le plus proche voisin
            // connecté
            for (Arc arc : courant.getArcs()) {
                // Récupérer l'autre extrémité de l'arc
                Noeud voisin = arc.getN1().getId() == courant.getId() ? arc.getN2() : arc.getN1();

                // Si ce voisin n'a pas été visité
                if (nonVisites.contains(voisin)) {
                    // La valeur de l'arc est deja la distance entre courant et voisin.
                    double dist = arc.getValeur();
                    if (dist < minDistance) {
                        minDistance = dist;
                        plusProche = voisin;
                    }
                }
            }

            // Si le graphe partiel bloque, on complete dynamiquement avec l'arc le plus court.
            // Cela garantit que l'algorithme peut terminer un circuit.
            if (plusProche == null) {
                for (Noeud n : nonVisites) {
                    // Ici on calcule la distance geometrique directement,
                    // puis on ajoute explicitement cet arc au graphe source.
                    double dist = courant.distanceTo(n, this.isGeo);
                    if (dist < minDistance) {
                        minDistance = dist;
                        plusProche = n;
                    }
                }

                if (plusProche == null) {
                    return null;
                }

                addArc(courant, plusProche);
            }

            chemin.add(plusProche);
            nonVisites.remove(plusProche);
            courant = plusProche;
        }

        // Fermer le cycle vers le noeud de depart.
        // Si l'arc n'existe pas (graphe partiel), on l'ajoute.
        if (getArc(courant, chemin.get(0)) == null) {
            addArc(courant, chemin.get(0));
        }

        // On ferme explicitement le chemin: dernier noeud = noeud de depart.
        chemin.add(chemin.get(0));

        Graphe resultat = new Graphe();
        resultat.isGeo = this.isGeo;

        // Graphe resultat = uniquement le tour construit (et ses arcs).
        // On copie les noeuds du chemin pour isoler le resultat du graphe source.
        for (Noeud n : chemin) {
            resultat.getNoeuds().add(new Noeud(n.getId(), n.getAbs(), n.getOrd()));
        }

        for (int i = 0; i < resultat.getNoeuds().size() - 1; i++) {
            Noeud n1 = resultat.getNoeuds().get(i);
            Noeud n2 = resultat.getNoeuds().get(i + 1);
            resultat.addArc(n1, n2);
        }

        return resultat;
    }

    // Construit un sous-graphe avec les nbNoeuds premiers noeuds,
    // puis reconstruit les arcs selon le mode demande (partiel/complet).
    private Graphe sousGraphe(int nbNoeuds, boolean modePartiel, int k) {
        Graphe sousGraphe = new Graphe();
        sousGraphe.isGeo = this.isGeo;

        int limite = Math.min(nbNoeuds, this.noeuds.size());

        for (int i = 0; i < limite; i++) {
            Noeud original = this.noeuds.get(i);
            sousGraphe.getNoeuds().add(new Noeud(original.getId(), original.getAbs(), original.getOrd()));
        }

        if (limite >= 2) {
            if (modePartiel) {
                int kLocal = k;
                if (kLocal >= limite) {
                    kLocal = limite - 1;
                }

                // partiel exige 1 < k < nombreNoeuds, sinon fallback en complet.
                if (kLocal > 1 && kLocal < limite) {
                    sousGraphe.partiel(kLocal);
                } else {
                    sousGraphe.complet();
                }
            } else {
                sousGraphe.complet();
            }
        }

        return sousGraphe;
    }

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

}
