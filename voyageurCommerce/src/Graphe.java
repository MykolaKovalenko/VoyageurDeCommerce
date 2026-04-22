import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graphe {

    private static final int MAX_2OPT_PASSES = 30;
    private static final int MAX_NOEUDS_2OPT_BENCH = 200;

    // ==============================
    // Etat interne
    // ==============================

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

    // ==============================
    // Getters
    // ==============================
    public ArrayList<Noeud> getNoeuds() {
        return noeuds;
    }

    public ArrayList<Arc> getArcs() {
        return arcs;
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

    // ==============================
    // Helpers internes
    // ==============================

    private void vider() {
        this.noeuds.clear();
        this.arcs.clear();
        this.arcsMap.clear();
    }

    private void fermerCycle(ArrayList<Noeud> chemin) {
        if (!chemin.isEmpty()) {
            chemin.add(chemin.get(0));
        }
    }

    private Graphe construireTourDepuisChemin(ArrayList<Noeud> chemin) {
        Graphe resultat = new Graphe();
        resultat.isGeo = this.isGeo;

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

    private Map<Integer, Noeud> indexNoeudsParId() {
        Map<Integer, Noeud> index = new HashMap<>();
        for (Noeud n : this.noeuds) {
            index.put(n.getId(), n);
        }
        return index;
    }

    private ArrayList<Integer> extraireOrdreIdsDepuisTour(Graphe tour) {
        ArrayList<Integer> ordre = new ArrayList<>();
        if (tour == null || tour.getNoeuds().isEmpty()) {
            return ordre;
        }

        for (Noeud n : tour.getNoeuds()) {
            ordre.add(n.getId());
        }

        if (ordre.size() > 1 && ordre.get(0).intValue() == ordre.get(ordre.size() - 1).intValue()) {
            ordre.remove(ordre.size() - 1);
        }
        return ordre;
    }

    private double distanceEntreIds(int idA, int idB, Map<Integer, Noeud> noeudsParId) {
        Noeud a = noeudsParId.get(idA);
        Noeud b = noeudsParId.get(idB);
        if (a == null || b == null) {
            return Double.POSITIVE_INFINITY;
        }
        return a.distanceTo(b, this.isGeo);
    }

    private void inverserSousSequence(ArrayList<Integer> ordre, int debut, int fin) {
        while (debut < fin) {
            int tmp = ordre.get(debut);
            ordre.set(debut, ordre.get(fin));
            ordre.set(fin, tmp);
            debut++;
            fin--;
        }
    }

    public Graphe deuxOpt(Graphe tour) {
        if (tour == null || tour.getNoeuds().size() < 5) {
            return tour;
        }

        ArrayList<Integer> ordre = extraireOrdreIdsDepuisTour(tour);
        int taille = ordre.size();
        if (taille < 4) {
            return tour;
        }

        Map<Integer, Noeud> noeudsParId = indexNoeudsParId();

        boolean amelioration = true;
        int passe = 0;

        while (amelioration && passe < MAX_2OPT_PASSES) {
            amelioration = false;
            passe++;

            for (int i = 1; i < taille - 1; i++) {
                boolean ameliorationLocale = false;

                for (int j = i + 1; j < taille; j++) {
                    if (j == i + 1) {
                        continue;
                    }

                    // Eviter le cas degeneré qui inverse tout le cycle sans gain structurel.
                    if (i == 1 && j == taille - 1) {
                        continue;
                    }

                    int a = ordre.get(i - 1);
                    int b = ordre.get(i);
                    int c = ordre.get(j);
                    int d = ordre.get((j + 1) % taille);

                    double ancienCout = distanceEntreIds(a, b, noeudsParId) + distanceEntreIds(c, d, noeudsParId);
                    double nouveauCout = distanceEntreIds(a, c, noeudsParId) + distanceEntreIds(b, d, noeudsParId);

                    if (nouveauCout + 1e-9 < ancienCout) {
                        inverserSousSequence(ordre, i, j);
                        amelioration = true;
                        ameliorationLocale = true;
                        break;
                    }
                }

                if (ameliorationLocale) {
                    break;
                }
            }
        }

        ArrayList<Noeud> chemin = new ArrayList<>();
        for (int id : ordre) {
            Noeud source = noeudsParId.get(id);
            if (source == null) {
                return tour;
            }
            chemin.add(source);
        }

        fermerCycle(chemin);
        return construireTourDepuisChemin(chemin);
    }

    // ==============================
    // Chargement depuis CSV
    // ==============================

    // Importe les noeuds depuis un CSV de format:
    // 1ere ligne: GEO ou 2D
    // lignes suivantes: id;abs;ord
    public void importer(String nomFichier) {
        // Repartir d'un etat propre pour eviter l'accumulation apres plusieurs imports.
        this.vider();
        Set<Integer> ids = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(nomFichier))) {
            // La 1ere ligne du fichier indique le type de distance a utiliser.
            String premiereLigne = br.readLine();
            if (premiereLigne == null) {
                throw new IllegalArgumentException("Le fichier est vide");
            }

            String typeDistance = premiereLigne.replace("\uFEFF", "").trim().toUpperCase();
            if (typeDistance.equals("GEO")) {
                isGeo = true;
            } else if (typeDistance.equals("2D")) {
                isGeo = false;
            } else {
                throw new IllegalArgumentException("La premiere ligne doit etre 'GEO' ou '2D'");
            }

            String ligne;
            int numeroLigne = 1;
            // on lit toutes les lignes jusque a la fin du fichier
            while ((ligne = br.readLine()) != null) {
                numeroLigne++;
                ligne = ligne.trim();

                if (ligne.isEmpty()) {
                    continue;
                }

                // on divise la ligne en morceaux avec ;
                String[] p = ligne.split(";");
                // on verifie qu'il y a exactement 3 valeurs
                if (p.length != 3) {
                    throw new IllegalArgumentException("Ligne " + numeroLigne + " invalide: format attendu id;abs;ord");
                }

                int id;
                double abs;
                double ord;
                try {
                    // Format attendu: id;abs;ord
                    id = Integer.parseInt(p[0].trim());
                    // remplace une virgule par un point, ex : 95,59 -> 95.59
                    // si deja avec un point donc on ne touche pas, ex 95.59 -> 95.59
                    abs = Double.parseDouble(p[1].trim().replace(",", "."));
                    ord = Double.parseDouble(p[2].trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Ligne " + numeroLigne + " invalide: valeurs numeriques attendues", e);
                }

                if (!ids.add(id)) {
                    throw new IllegalArgumentException("ID duplique detecte: " + id + " (ligne " + numeroLigne + ")");
                }

                if (isGeo) {
                    if (abs < -90 || abs > 90) {
                        throw new IllegalArgumentException(
                                "Ligne " + numeroLigne + " invalide: latitude GEO hors intervalle [-90,90]");
                    }
                    if (ord < -180 || ord > 180) {
                        throw new IllegalArgumentException(
                                "Ligne " + numeroLigne + " invalide: longitude GEO hors intervalle [-180,180]");
                    }
                }

                // Chaque ligne devient un objet Noeud.
                noeuds.add(new Noeud(id, abs, ord));
            }
        } catch (IOException e) {
            this.vider();
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            this.vider();
            System.err.println("Erreur de format : " + e.getMessage());
        }
    }

    // ==============================
    // Types de graphes
    // ==============================

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

    // Trouve la racine d'un noeud avec compression de chemin.
    private Noeud find(Map<Noeud, Noeud> parent, Noeud n) {
        Noeud p = parent.get(n);
        if (p != n) {
            parent.put(n, find(parent, p));
        }
        return parent.get(n);
    }

    // Fusionne deux ensembles Union-Find avec union par rang.
    private void union(Map<Noeud, Noeud> parent, Map<Noeud, Integer> rank, Noeud a, Noeud b) {
        Noeud racineA = find(parent, a);
        Noeud racineB = find(parent, b);

        if (racineA == racineB) {
            return;
        }

        int rankA = rank.get(racineA);
        int rankB = rank.get(racineB);

        if (rankA < rankB) {
            parent.put(racineA, racineB);
        } else if (rankA > rankB) {
            parent.put(racineB, racineA);
        } else {
            parent.put(racineB, racineA);
            rank.put(racineA, rankA + 1);
        }
    }

    // Retourne les noeuds de degre impair d'un graphe.
    private ArrayList<Noeud> noeudsDegreImpair(Graphe g) {
        ArrayList<Noeud> impairs = new ArrayList<>();
        for (Noeud n : g.getNoeuds()) {
            if ((n.getArcs().size() % 2) != 0) {
                impairs.add(n);
            }
        }
        return impairs;
    }

    // Minimum perfect matching EXACT sur les noeuds de degre impair du MST.
    // Implementation: DP sur bitmask (complexite exponentielle en nb de noeuds impairs).
    // Pour rester praticable en memoire, on limite a 22 noeuds impairs.
    public Graphe minimumMatching(Graphe mst) {
        Graphe matching = new Graphe();

        if (mst == null) {
            return matching;
        }

        matching.isGeo = mst.isGeo;

        // Copier tous les noeuds du MST pour garder des IDs compatibles pour la suite.
        Map<Integer, Noeud> copieNoeuds = new HashMap<>();
        for (Noeud original : mst.getNoeuds()) {
            Noeud clone = new Noeud(original.getId(), original.getAbs(), original.getOrd());
            matching.getNoeuds().add(clone);
            copieNoeuds.put(clone.getId(), clone);
        }

        // Recuperer uniquement les noeuds de degre impair du MST.
        ArrayList<Noeud> impairs = noeudsDegreImpair(mst);

        if (impairs.isEmpty()) {
            return matching;
        }

        if ((impairs.size() % 2) != 0) {
            throw new IllegalStateException("MM exact impossible: nombre impair de noeuds impairs.");
        }

        int m = impairs.size();
        int maxImpairsExact = 22;
        if (m > maxImpairsExact) {
            throw new IllegalStateException(
                    "MM exact trop couteux pour cette instance (" + m + " noeuds impairs > "
                            + maxImpairsExact + ").");
        }

        double[][] dist = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                double d = impairs.get(i).distanceTo(impairs.get(j), mst.isGeo);
                dist[i][j] = d;
                dist[j][i] = d;
            }
        }

        int fullMask = (1 << m) - 1;
        double[] memo = new double[1 << m];
        int[] choix = new int[1 << m];
        Arrays.fill(memo, Double.NaN);
        Arrays.fill(choix, -1);

        // Lance la DP pour remplir memo/choix.
        coutMatchingExactDp(fullMask, dist, memo, choix);

        // Reconstruire le matching optimal a partir des choix memorises.
        int mask = fullMask;
        while (mask != 0) {
            int i = Integer.numberOfTrailingZeros(mask);
            int j = choix[mask];
            if (j < 0) {
                throw new IllegalStateException("Reconstruction MM exact impossible.");
            }

            matching.addArc(
                    copieNoeuds.get(impairs.get(i).getId()),
                    copieNoeuds.get(impairs.get(j).getId()));

            mask = mask & ~(1 << i);
            mask = mask & ~(1 << j);
        }

        return matching;
    }

    // DP de matching parfait minimum.
    // mask encode l'ensemble des indices impairs restant a apparier.
    private double coutMatchingExactDp(int mask, double[][] dist, double[] memo, int[] choix) {
        if (mask == 0) {
            return 0.0;
        }

        if (!Double.isNaN(memo[mask])) {
            return memo[mask];
        }

        int i = Integer.numberOfTrailingZeros(mask);
        int maskSansI = mask & ~(1 << i);

        double best = Double.POSITIVE_INFINITY;
        int bestJ = -1;

        for (int j = i + 1; j < dist.length; j++) {
            if ((maskSansI & (1 << j)) == 0) {
                continue;
            }

            int suivant = maskSansI & ~(1 << j);
            double cout = dist[i][j] + coutMatchingExactDp(suivant, dist, memo, choix);

            if (cout < best) {
                best = cout;
                bestJ = j;
            }
        }

        choix[mask] = bestJ;
        memo[mask] = best;
        return best;
    }

    // ==============================
    // Algorithmes TSP / auxiliaires
    // ==============================

    public Graphe glouton() {
        // Cas limite: pas de noeuds, pas de tour.
        if (noeuds.isEmpty()) {
            return null;
        }

        // chemin: ordre de visite construit par l'algorithme.
        ArrayList<Noeud> chemin = new ArrayList<>();
        // nonVisites: ensemble des noeuds qu'il reste a visiter.
        ArrayList<Noeud> nonVisites = new ArrayList<>(noeuds);
        Set<Noeud> nonVisitesSet = new HashSet<>(nonVisites);

        // Le depart est aleatoire.
        // Cela evite de toujours produire exactement le meme tour.
        int index = (int) (Math.random() * nonVisites.size());
        Noeud courant = nonVisites.get(index);
        chemin.add(courant);
        nonVisites.remove(courant);
        nonVisitesSet.remove(courant);

        // A chaque etape on va au plus proche voisin non visite.
        while (!nonVisites.isEmpty()) {
            Noeud plusProche = null;
            double minDistance = Double.MAX_VALUE;

            // Parcourir les arcs du noeud courant pour trouver le plus proche voisin connecte.
            for (Arc arc : courant.getArcs()) {
                // Recuperer l'autre extremite de l'arc.
                Noeud voisin = arc.getN1().getId() == courant.getId() ? arc.getN2() : arc.getN1();

                // Si ce voisin n'a pas ete visite.
                if (nonVisitesSet.contains(voisin)) {
                    // La valeur de l'arc est deja la distance entre courant et voisin.
                    double dist = arc.getValeur();
                    if (dist < minDistance) {
                        minDistance = dist;
                        plusProche = voisin;
                    }
                }
            }

            // Si le graphe partiel bloque, on choisit le plus proche non visite
            // sans modifier le graphe source.
            if (plusProche == null) {
                for (Noeud n : nonVisites) {
                    // Distance geometrique directe en fallback.
                    double dist = courant.distanceTo(n, this.isGeo);
                    if (dist < minDistance) {
                        minDistance = dist;
                        plusProche = n;
                    }
                }

                if (plusProche == null) {
                    return null;
                }
            }

            chemin.add(plusProche);
            nonVisites.remove(plusProche);
            nonVisitesSet.remove(plusProche);
            courant = plusProche;
        }

        // On ferme explicitement le chemin: dernier noeud = noeud de depart.
        fermerCycle(chemin);
        return construireTourDepuisChemin(chemin);
    }

    public void evaluerComplexite(boolean benchmarkKruskal, boolean benchmarkMM, boolean benchmarkChristofides) {
        int n = this.noeuds.size();
        int repetitions = 100;
        boolean mesurer2Opt = n <= MAX_NOEUDS_2OPT_BENCH;
        long totalGlouton = 0;
        long totalGlouton2Opt = 0;
        long totalKruskal = 0;
        long totalMstApprox = 0;
        long totalMstApprox2Opt = 0;
        long totalMM = 0;
        long totalMstPlusMM = 0;
        long totalChristofides = 0;
        long totalChristofides2Opt = 0;

        double bestGlouton = Double.POSITIVE_INFINITY;
        double worstGlouton = Double.NEGATIVE_INFINITY;
        double sommeGlouton = 0.0;
        int nbGloutonValides = 0;

        double bestGlouton2Opt = Double.POSITIVE_INFINITY;
        double worstGlouton2Opt = Double.NEGATIVE_INFINITY;
        double sommeGlouton2Opt = 0.0;
        int nbGlouton2OptValides = 0;

        double sommeMstApprox2Opt = 0.0;
        int nbMstApprox2OptValides = 0;

        double sommeChristofides2Opt = 0.0;
        int nbChristofides2OptValides = 0;

        // 100 iterations glouton
        for (int r = 0; r < repetitions; r++) {
            long debut = System.nanoTime();
            Graphe tourGlouton = this.glouton();
            totalGlouton += System.nanoTime() - debut;
            if (tourGlouton != null) {
                double cout = tourGlouton.cout();
                bestGlouton = Math.min(bestGlouton, cout);
                worstGlouton = Math.max(worstGlouton, cout);
                sommeGlouton += cout;
                nbGloutonValides++;

                if (mesurer2Opt) {
                    long debut2Opt = System.nanoTime();
                    Graphe tourGlouton2Opt = this.deuxOpt(tourGlouton);
                    totalGlouton2Opt += System.nanoTime() - debut2Opt;

                    if (tourGlouton2Opt != null) {
                        double cout2Opt = tourGlouton2Opt.cout();
                        bestGlouton2Opt = Math.min(bestGlouton2Opt, cout2Opt);
                        worstGlouton2Opt = Math.max(worstGlouton2Opt, cout2Opt);
                        sommeGlouton2Opt += cout2Opt;
                        nbGlouton2OptValides++;
                    }
                }
            }
        }

        // 100 iterations kruskal (MST)
        if (benchmarkKruskal) {
            for (int r = 0; r < repetitions; r++) {
                long debut = System.nanoTime();
                this.kruskal();
                totalKruskal += System.nanoTime() - debut;
            }
        }

        // 100 iterations TSP via MST (mstApprox)
        for (int r = 0; r < repetitions; r++) {
            long debut = System.nanoTime();
            Graphe tourMst = this.mstApprox();
            totalMstApprox += System.nanoTime() - debut;

            if (mesurer2Opt && tourMst != null) {
                long debut2Opt = System.nanoTime();
                Graphe tourMst2Opt = this.deuxOpt(tourMst);
                totalMstApprox2Opt += System.nanoTime() - debut2Opt;
                if (tourMst2Opt != null) {
                    sommeMstApprox2Opt += tourMst2Opt.cout();
                    nbMstApprox2OptValides++;
                }
            }
        }

        // 100 iterations minimumMatching (MST pre-calcule hors mesure)
        if (benchmarkMM) {
            for (int r = 0; r < repetitions; r++) {
                Graphe mst = this.kruskal();
                long debut = System.nanoTime();
                this.minimumMatching(mst);
                totalMM += System.nanoTime() - debut;
            }
        }

        // 100 iterations pipeline complet MST + MM
        if (benchmarkMM) {
            for (int r = 0; r < repetitions; r++) {
                long debut = System.nanoTime();
                Graphe mst = this.kruskal();
                this.minimumMatching(mst);
                totalMstPlusMM += System.nanoTime() - debut;
            }
        }

        // 100 iterations Christofides (pipeline complet TSP)
        if (benchmarkChristofides) {
            for (int r = 0; r < repetitions; r++) {
                long debut = System.nanoTime();
                Graphe tourChristofides = this.christofides();
                totalChristofides += System.nanoTime() - debut;

                if (mesurer2Opt && tourChristofides != null) {
                    long debut2Opt = System.nanoTime();
                    Graphe tourChristofides2Opt = this.deuxOpt(tourChristofides);
                    totalChristofides2Opt += System.nanoTime() - debut2Opt;
                    if (tourChristofides2Opt != null) {
                        sommeChristofides2Opt += tourChristofides2Opt.cout();
                        nbChristofides2OptValides++;
                    }
                }
            }
        }

        double sGlouton = (totalGlouton / (double) repetitions) / 1_000_000_000.0;
        double sGlouton2OptSeul = mesurer2Opt
            ? (totalGlouton2Opt / (double) repetitions) / 1_000_000_000.0
            : Double.NaN;
        double sKruskal = benchmarkKruskal ? (totalKruskal / (double) repetitions) / 1_000_000_000.0 : Double.NaN;
        double sMstApprox = (totalMstApprox / (double) repetitions) / 1_000_000_000.0;
        double sMstApprox2OptSeul = mesurer2Opt
            ? (totalMstApprox2Opt / (double) repetitions) / 1_000_000_000.0
            : Double.NaN;
        double sMM = benchmarkMM ? (totalMM / (double) repetitions) / 1_000_000_000.0 : Double.NaN;
        double sMstPlusMM = benchmarkMM ? (totalMstPlusMM / (double) repetitions) / 1_000_000_000.0 : Double.NaN;
        double sChristofides = benchmarkChristofides
            ? (totalChristofides / (double) repetitions) / 1_000_000_000.0
            : Double.NaN;
        double sChristofides2OptSeul = (benchmarkChristofides && mesurer2Opt)
            ? (totalChristofides2Opt / (double) repetitions) / 1_000_000_000.0
            : Double.NaN;

        double sGlouton2Opt = mesurer2Opt ? sGlouton + sGlouton2OptSeul : Double.NaN;
        double sMstApprox2Opt = mesurer2Opt ? sMstApprox + sMstApprox2OptSeul : Double.NaN;
        double sChristofides2Opt = (benchmarkChristofides && mesurer2Opt)
            ? sChristofides + sChristofides2OptSeul
            : Double.NaN;

        System.out.println("--- Evaluation des temps (" + repetitions + " iterations, n=" + n + ") ---");
        System.out.printf("glouton                  : %.6f s%n", sGlouton);
        if (mesurer2Opt) {
            System.out.printf("glouton + 2-opt          : %.6f s%n", sGlouton2Opt);
        }
        if (benchmarkKruskal) {
            System.out.printf("kruskal (MST)            : %.6f s%n", sKruskal);
        }
        System.out.printf("tsp via MST (mstApprox)  : %.6f s%n", sMstApprox);
        if (mesurer2Opt) {
            System.out.printf("mstApprox + 2-opt        : %.6f s%n", sMstApprox2Opt);
        }
        if (benchmarkMM) {
            System.out.printf("minimumMatching (MM seul): %.6f s%n", sMM);
            System.out.printf("pipeline (MST + MM)      : %.6f s%n", sMstPlusMM);
        }
        if (benchmarkChristofides) {
            System.out.printf("christofides             : %.6f s%n", sChristofides);
            if (mesurer2Opt) {
                System.out.printf("christofides + 2-opt     : %.6f s%n", sChristofides2Opt);
            }
        }
        if (!mesurer2Opt) {
            System.out.println("2-opt non mesure (n > " + MAX_NOEUDS_2OPT_BENCH + ")");
        }

        double moyenneGlouton = nbGloutonValides > 0 ? sommeGlouton / nbGloutonValides : Double.NaN;
        System.out.println("--- Resultats glouton (" + repetitions + " iterations) ---");
        System.out.println("meilleur cout glouton    : "
            + (Double.isFinite(bestGlouton) ? bestGlouton : "n/a"));
        System.out.println("pire cout glouton        : "
            + (Double.isFinite(worstGlouton) ? worstGlouton : "n/a"));
        System.out.println("cout moyen glouton       : "
            + (Double.isFinite(moyenneGlouton) ? moyenneGlouton : "n/a"));

        if (mesurer2Opt) {
            double moyenneGlouton2Opt = nbGlouton2OptValides > 0
                ? sommeGlouton2Opt / nbGlouton2OptValides
                : Double.NaN;
            double moyenneMstApprox2Opt = nbMstApprox2OptValides > 0
                ? sommeMstApprox2Opt / nbMstApprox2OptValides
                : Double.NaN;
            double moyenneChristofides2Opt = nbChristofides2OptValides > 0
                ? sommeChristofides2Opt / nbChristofides2OptValides
                : Double.NaN;

            System.out.println("--- Resultats glouton + 2-opt (" + repetitions + " iterations) ---");
            System.out.println("meilleur cout glouton    : "
                + (Double.isFinite(bestGlouton2Opt) ? bestGlouton2Opt : "n/a"));
            System.out.println("pire cout glouton        : "
                + (Double.isFinite(worstGlouton2Opt) ? worstGlouton2Opt : "n/a"));
            System.out.println("cout moyen glouton       : "
                + (Double.isFinite(moyenneGlouton2Opt) ? moyenneGlouton2Opt : "n/a"));

            System.out.println("--- Resultats methodes + 2-opt (" + repetitions + " iterations) ---");
            System.out.println("cout moyen mstApprox     : "
                + (Double.isFinite(moyenneMstApprox2Opt) ? moyenneMstApprox2Opt : "n/a"));
            if (benchmarkChristofides) {
                System.out.println("cout moyen christofides  : "
                    + (Double.isFinite(moyenneChristofides2Opt) ? moyenneChristofides2Opt : "n/a"));
            }
        }
    }

    public Graphe kruskal() {
        // Graphe qui contiendra l'arbre couvrant minimal
        Graphe mst = new Graphe();
        mst.isGeo = this.isGeo;

        // Cloner les noeuds pour ne pas partager les objets avec le graphe source.
        Map<Integer, Noeud> copieNoeuds = new HashMap<>();
        for (Noeud original : this.noeuds) {
            Noeud clone = new Noeud(original.getId(), original.getAbs(), original.getOrd());
            mst.getNoeuds().add(clone);
            copieNoeuds.put(clone.getId(), clone);
        }

        // 1) Trier les arcs par distance croissante
        ArrayList<Arc> arcsTries = new ArrayList<>(this.arcs);
        arcsTries.sort((a, b) -> Double.compare(a.getValeur(), b.getValeur()));

        // 2) Structure Union-Find (pour eviter les cycles)
        Map<Noeud, Noeud> parent = new HashMap<>();
        Map<Noeud, Integer> rank = new HashMap<>();

        // Au début chaque noeud est son propre parent
        for (Noeud n : this.noeuds) {
            parent.put(n, n);
            rank.put(n, 0);
        }

        // 3) Parcourir les arcs tries
        for (Arc a : arcsTries) {

            // Trouver les racines des deux noeuds
            Noeud r1 = find(parent, a.getN1());
            Noeud r2 = find(parent, a.getN2());

            // Si racines différentes → pas de cycle
            if (r1 != r2) {

                // On ajoute l'arc avec les noeuds clones du MST.
                Noeud n1Mst = copieNoeuds.get(a.getN1().getId());
                Noeud n2Mst = copieNoeuds.get(a.getN2().getId());
                mst.addArc(n1Mst, n2Mst);

                // On fusionne les deux ensembles
                union(parent, rank, r1, r2);

                // Un arbre couvrant sur n noeuds contient exactement n-1 arcs.
                if (mst.getArcs().size() == this.noeuds.size() - 1) {
                    break;
                }
            }
        }

        return mst;
    }

    private void dfs(Noeud n, Set<Noeud> visite, ArrayList<Noeud> parcours) {

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

        // 1) Construire le MST
        Graphe mst = this.kruskal();

        if (mst.getNoeuds().isEmpty()) {
            return mst;
        }

        // 2) DFS pour obtenir un parcours
        ArrayList<Noeud> parcours = new ArrayList<>();
        Set<Noeud> visite = new HashSet<>();

        // Parcours DFS de toutes les composantes pour ne pas perdre de noeuds
        // si le graphe est deconnecte.
        for (Noeud depart : mst.getNoeuds()) {
            if (!visite.contains(depart)) {
                mst.dfs(depart, visite, parcours);
            }
        }

        // 3) Supprimer les doublons -> chemin hamiltonien
        ArrayList<Noeud> chemin = new ArrayList<>();
        Set<Noeud> dejaAjoutes = new HashSet<>();

        for (Noeud n : parcours) {
            // On garde seulement la premiere apparition.
            if (dejaAjoutes.add(n)) {
                chemin.add(n);
            }
        }

        // 4) Fermer le cycle (revenir au depart)
        fermerCycle(chemin);

        // 5) Construire le graphe resultat
        return construireTourDepuisChemin(chemin);
    }

    // Circuit eulerien par l'algorithme de Hierholzer sur un multigraphe.
    // Precondition: tous les noeuds ont un degre pair.
    // Retourne la liste ordonnee des noeuds formant le circuit.
    private ArrayList<Noeud> hierholzer(Map<Integer, Noeud> noeudsParId, ArrayList<Arc> arcs) {

        // Liste d'adjacence: id -> liste de [idVoisin, indexArc].
        // Chaque arc non oriente apparait deux fois (une par extremite).
        Map<Integer, ArrayList<int[]>> adj = new HashMap<>();
        for (int i = 0; i < arcs.size(); i++) {
            Arc a = arcs.get(i);
            int id1 = a.getN1().getId();
            int id2 = a.getN2().getId();
            adj.computeIfAbsent(id1, x -> new ArrayList<>()).add(new int[] { id2, i });
            adj.computeIfAbsent(id2, x -> new ArrayList<>()).add(new int[] { id1, i });
        }

        // arcUtilise[i] = true si l'arc d'index i a deja ete emprunte.
        boolean[] arcUtilise = new boolean[arcs.size()];

        // Pointeur par noeud: evite de re-scanner les arcs deja utilises.
        Map<Integer, Integer> ptr = new HashMap<>();
        for (int id : adj.keySet()) {
            ptr.put(id, 0);
        }

        // Depart: premier noeud disponible.
        int depart = noeudsParId.keySet().iterator().next();

        // Pile de Hierholzer (version iterative pour eviter les debordements de pile).
        Deque<Integer> pile = new ArrayDeque<>();
        ArrayList<Noeud> circuit = new ArrayList<>();
        pile.push(depart);

        while (!pile.isEmpty()) {
            int v = pile.peek();
            ArrayList<int[]> voisins = adj.getOrDefault(v, new ArrayList<>());
            int p = ptr.getOrDefault(v, 0);

            // Avancer le pointeur jusqu'au prochain arc non utilise.
            while (p < voisins.size() && arcUtilise[voisins.get(p)[1]]) {
                p++;
            }
            ptr.put(v, p);

            if (p == voisins.size()) {
                // Plus d'arcs disponibles depuis v: on l'ajoute au circuit.
                circuit.add(noeudsParId.get(pile.pop()));
            } else {
                // Emprunter l'arc vers le voisin.
                int[] suivant = voisins.get(p);
                arcUtilise[suivant[1]] = true;
                ptr.put(v, p + 1);
                pile.push(suivant[0]);
            }
        }

        return circuit;
    }

    // Algorithme de Christofides:
    // 1. MST de Kruskal
    // 2. Minimum matching sur les noeuds impairs du MST
    // 3. Fusion MST + matching en multigraphe (tous les degres deviennent pairs)
    // 4. Circuit eulerien (Hierholzer)
    // 5. Shortcut des repetitions → tour hamiltonien
    public Graphe christofides() {

        if (noeuds.isEmpty()) {
            return null;
        }

        // 1. MST
        Graphe mst = this.kruskal();

        // 2. Minimum matching sur les noeuds impairs du MST
        Graphe mm = this.minimumMatching(mst);

        // 3. Construire le multigraphe MST + MM.
        // On mappe les IDs vers les noeuds du graphe source pour avoir les bonnes coords.
        Map<Integer, Noeud> noeudsParId = new HashMap<>();
        for (Noeud n : this.noeuds) {
            noeudsParId.put(n.getId(), n);
        }

        // Accumuler tous les arcs du MST et du matching.
        ArrayList<Arc> tousArcs = new ArrayList<>();
        for (Arc a : mst.getArcs()) {
            Noeud n1 = noeudsParId.get(a.getN1().getId());
            Noeud n2 = noeudsParId.get(a.getN2().getId());
            tousArcs.add(new Arc(n1, n2, a.getValeur()));
        }
        for (Arc a : mm.getArcs()) {
            Noeud n1 = noeudsParId.get(a.getN1().getId());
            Noeud n2 = noeudsParId.get(a.getN2().getId());
            tousArcs.add(new Arc(n1, n2, a.getValeur()));
        }

        if (tousArcs.isEmpty()) {
            return null;
        }

        // 4. Circuit eulerien sur le multigraphe.
        ArrayList<Noeud> circuit = hierholzer(noeudsParId, tousArcs);

        if (circuit.isEmpty()) {
            return null;
        }

        // 5. Shortcut: garder seulement la premiere apparition de chaque noeud.
        ArrayList<Noeud> chemin = new ArrayList<>();
        Set<Integer> dejaVus = new HashSet<>();
        for (Noeud n : circuit) {
            if (dejaVus.add(n.getId())) {
                chemin.add(n);
            }
        }

        // Fermer le cycle.
        fermerCycle(chemin);

        // 6. Construire le graphe resultat.
        return construireTourDepuisChemin(chemin);
    }

}
