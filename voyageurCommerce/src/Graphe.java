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

    // nombre max de passes 2-opt pour eviter une boucle infinie
    private static final int MAX_2OPT_PASSES = 30;
    // au-dela de cette taille, on desactive le 2-opt dans le benchmark (trop lent)
    private static final int MAX_NOEUDS_2OPT_BENCH = 200;

    // ==============================
    // Donnees du graphe
    // ==============================

    // Liste des noeuds (villes)
    private ArrayList<Noeud> noeuds;
    // Liste de tous les arcs
    private ArrayList<Arc> arcs;
    // Index rapide avec une cle "idMin-idMax" pour pas dupliquer les arcs
    private Map<String, Arc> arcsMap;

    // true = mode GEO, false = mode 2D euclidien
    private boolean isGeo;

    // Constructeur: graphe vide
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

    // Cout total = somme des distances de tous les arcs
    // Sur un tour TSP, ca donne la longueur du circuit
    public double cout() {
        double somme = 0;
        for (Arc a : arcs) {
            somme += a.getValeur();
        }
        return somme;
    }

    // Ajoute un arc non oriente entre n1 et n2
    // Si il existe deja, on ne fait rien
    public void addArc(Noeud n1, Noeud n2) {

        // cle unique de la forme "idMin-idMax" pour retrouver l arc facilement
        int id1 = Math.min(n1.getId(), n2.getId());
        int id2 = Math.max(n1.getId(), n2.getId());
        String key = id1 + "-" + id2;

        // si l arc existe deja dans la map, on sort directement
        if (arcsMap.containsKey(key))
            return;

        // calcule la distance et cree l arc
        double distance = n1.distanceTo(n2, this.isGeo);
        Arc arc = new Arc(n1, n2, distance);
        arcs.add(arc);
        arcsMap.put(key, arc);
        // on ajoute l arc aux deux noeuds pour pouvoir naviguer depuis chacun
        n1.getArcs().add(arc);
        n2.getArcs().add(arc);
    }

    // ==============================
    // Outils internes
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

    // Prend une liste ordonnee de noeuds et construit un graphe avec les arcs entre consecutifs
    // On clone les noeuds pour ne pas modifier le graphe source
    private Graphe construireTourDepuisChemin(ArrayList<Noeud> chemin) {
        Graphe resultat = new Graphe();
        resultat.isGeo = this.isGeo;

        // clone de chaque noeud du chemin
        for (Noeud n : chemin) {
            resultat.getNoeuds().add(new Noeud(n.getId(), n.getAbs(), n.getOrd()));
        }

        // relie chaque noeud avec le suivant dans l ordre du chemin
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

    // Extrait la liste des IDs des noeuds dans l ordre du tour
    // Si le tour est un cycle ferme (premier == dernier), on retire le doublon final
    private ArrayList<Integer> extraireOrdreIdsDepuisTour(Graphe tour) {
        ArrayList<Integer> ordre = new ArrayList<>();
        if (tour == null || tour.getNoeuds().isEmpty()) {
            return ordre;
        }

        for (Noeud n : tour.getNoeuds()) {
            ordre.add(n.getId());
        }

        // supprime le dernier si c est le meme que le premier (tour ferme)
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

    // Ameliore un tour TSP en echangeant des paires d arcs (algorithme 2-opt)
    // Principe: si croiser deux arcs coute moins cher, on les inverse
    public Graphe deuxOpt(Graphe tour) {
        if (tour == null || tour.getNoeuds().size() < 5) {
            return tour;
        }

        // on travaille sur la liste des IDs dans l ordre du tour
        ArrayList<Integer> ordre = extraireOrdreIdsDepuisTour(tour);
        int taille = ordre.size();
        if (taille < 4) {
            return tour;
        }

        // index id -> noeud pour calculer les distances rapidement
        Map<Integer, Noeud> noeudsParId = indexNoeudsParId();

        boolean amelioration = true;
        int passe = 0;

        // on repete tant qu on trouve une amelioration (ou max de passes atteint)
        while (amelioration && passe < MAX_2OPT_PASSES) {
            amelioration = false;
            passe++;

            for (int i = 1; i < taille - 1; i++) {
                boolean ameliorationLocale = false;

                for (int j = i + 1; j < taille; j++) {
                    // arcs adjacents: pas de gain possible
                    if (j == i + 1) {
                        continue;
                    }

                    // Evite un cas un peu degenerate: on retournerait presque tout le cycle pour rien
                    if (i == 1 && j == taille - 1) {
                        continue;
                    }

                    // les 4 noeuds autour des deux arcs candidats a l echange
                    int a = ordre.get(i - 1);
                    int b = ordre.get(i);
                    int c = ordre.get(j);
                    int d = ordre.get((j + 1) % taille);

                    // compare le cout actuel (a-b + c-d) vs le cout apres echange (a-c + b-d)
                    double ancienCout = distanceEntreIds(a, b, noeudsParId) + distanceEntreIds(c, d, noeudsParId);
                    double nouveauCout = distanceEntreIds(a, c, noeudsParId) + distanceEntreIds(b, d, noeudsParId);

                    // si c est mieux, on inverse la sous-sequence entre i et j
                    if (nouveauCout + 1e-9 < ancienCout) {
                        inverserSousSequence(ordre, i, j);
                        amelioration = true;
                        ameliorationLocale = true;
                        break;
                    }
                }

                // une amelioration trouvee: on repart depuis le debut
                if (ameliorationLocale) {
                    break;
                }
            }
        }

        // reconstruit le chemin depuis les IDs dans le nouvel ordre
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
    // Chargement CSV
    // ==============================

    // Format attendu du CSV:
    // 1ere ligne: GEO ou 2D
    // ensuite: id;abs;ord
    public void importer(String nomFichier) {
        // On vide avant de charger, sinon on accumule les anciens noeuds
        this.vider();
        Set<Integer> ids = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(nomFichier))) {
            // La 1ere ligne dit si on est en GEO ou 2D
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
            // on lit les lignes jusqu a la fin
            while ((ligne = br.readLine()) != null) {
                numeroLigne++;
                ligne = ligne.trim();

                if (ligne.isEmpty()) {
                    continue;
                }

                // on coupe la ligne avec ;
                String[] p = ligne.split(";");
                // il faut exactement 3 colonnes
                if (p.length != 3) {
                    throw new IllegalArgumentException("Ligne " + numeroLigne + " invalide: format attendu id;abs;ord");
                }

                int id;
                double abs;
                double ord;
                try {
                    // format: id;abs;ord
                    id = Integer.parseInt(p[0].trim());
                    // accepte 95,59 et 95.59 en remplacant la virgule par un point
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

                // chaque ligne devient un Noeud
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
    // Construction de graphes
    // ==============================

    // Graphe complet: chaque paire de noeuds est reliee
    // Nombre d'arcs attendu: n*(n-1)/2
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

        // Pour chaque noeud, on relie les k voisins les plus proches
        for (Noeud n1 : noeuds) {
            // Candidats pas encore choisis autour de n1
            ArrayList<Noeud> nonVisites = new ArrayList<>(this.noeuds);
            // n1 ne peut pas etre son propre voisin
            nonVisites.remove(n1);

            // Petit glouton local: on prend les k plus proches
            for (int i = 0; i < k; i++) {
                Noeud plusProche = null;
                double minDistance = Double.MAX_VALUE;

                // cherche le plus proche restant
                for (Noeud n2 : nonVisites) {
                    double dist = n1.distanceTo(n2, this.isGeo);
                    // on garde le plus petit
                    if (dist < minDistance) {
                        minDistance = dist;
                        plusProche = n2;
                    }
                }

                if (plusProche != null) {
                    // on ajoute l'arc puis on retire ce voisin des candidats
                    addArc(n1, plusProche);
                    nonVisites.remove(plusProche);
                }
            }
        }
    }

    // Union-Find: trouve la racine du groupe avec compression de chemin
    // La compression: on redirige directement n vers sa racine pour accelerer les prochains appels
    private Noeud find(Map<Noeud, Noeud> parent, Noeud n) {
        Noeud p = parent.get(n);
        // si n n est pas sa propre racine, on cherche recursivement
        if (p != n) {
            // compression: on met a jour le parent directement vers la racine
            parent.put(n, find(parent, p));
        }
        return parent.get(n);
    }

    // Union-Find: fusion de deux ensembles (union par rang)
    // On attache le plus petit arbre sous le plus grand pour rester efficace
    private void union(Map<Noeud, Noeud> parent, Map<Noeud, Integer> rank, Noeud a, Noeud b) {
        Noeud racineA = find(parent, a);
        Noeud racineB = find(parent, b);

        // deja dans le meme groupe, rien a faire
        if (racineA == racineB) {
            return;
        }

        int rankA = rank.get(racineA);
        int rankB = rank.get(racineB);

        // on attache le plus petit sous le plus grand
        if (rankA < rankB) {
            parent.put(racineA, racineB);
        } else if (rankA > rankB) {
            parent.put(racineB, racineA);
        } else {
            // rang egal: on choisit et on incremente le rang du nouveau parent
            parent.put(racineB, racineA);
            rank.put(racineA, rankA + 1);
        }
    }

    // Retourne les noeuds de degre impair
    private ArrayList<Noeud> noeudsDegreImpair(Graphe g) {
        ArrayList<Noeud> impairs = new ArrayList<>();
        for (Noeud n : g.getNoeuds()) {
            if ((n.getArcs().size() % 2) != 0) {
                impairs.add(n);
            }
        }
        return impairs;
    }

    // Matching parfait minimum exact sur les noeuds impairs du MST
    // Implementation en DP bitmask (ca coute cher quand ca grossit)
    // On limite a 22 noeuds impairs pour rester gerable
    public Graphe minimumMatching(Graphe mst) {
        Graphe matching = new Graphe();

        if (mst == null) {
            return matching;
        }

        matching.isGeo = mst.isGeo;

        // Copie des noeuds du MST pour garder les memes IDs
        Map<Integer, Noeud> copieNoeuds = new HashMap<>();
        for (Noeud original : mst.getNoeuds()) {
            Noeud clone = new Noeud(original.getId(), original.getAbs(), original.getOrd());
            matching.getNoeuds().add(clone);
            copieNoeuds.put(clone.getId(), clone);
        }

        // On prend seulement les noeuds impairs
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

        // fullMask = tous les bits a 1, represente tous les noeuds impairs
        int fullMask = (1 << m) - 1;
        // memo[mask] = cout minimum pour apparier les noeuds du masque
        double[] memo = new double[1 << m];
        // choix[mask] = indice j du noeud apparie avec le premier bit de mask
        int[] choix = new int[1 << m];
        Arrays.fill(memo, Double.NaN); // NaN = pas encore calcule
        Arrays.fill(choix, -1);        // -1 = pas encore de choix

        // Lance la DP et remplit memo/choix
        coutMatchingExactDp(fullMask, dist, memo, choix);

        // Reconstruit le matching optimal depuis les choix memorises
        // a chaque iteration: on prend le premier noeud disponible (bit le plus bas)
        // et on l apparie avec le noeud j enregistre dans choix[mask]
        int mask = fullMask;
        while (mask != 0) {
            // indice du premier noeud impair encore a apparier
            int i = Integer.numberOfTrailingZeros(mask);
            int j = choix[mask];
            if (j < 0) {
                throw new IllegalStateException("Reconstruction MM exact impossible.");
            }

            matching.addArc(
                    copieNoeuds.get(impairs.get(i).getId()),
                    copieNoeuds.get(impairs.get(j).getId()));

            // retire i et j du mask: ils sont apparies
            mask = mask & ~(1 << i);
            mask = mask & ~(1 << j);
        }

        return matching;
    }

    // DP du matching: mask represente les indices impairs encore a apparier
    // Principe: on prend le premier bit du mask, on l apparie avec chaque autre bit possible
    // et on garde la combinaison qui minimise le cout total
    private double coutMatchingExactDp(int mask, double[][] dist, double[] memo, int[] choix) {
        // plus rien a apparier
        if (mask == 0) {
            return 0.0;
        }

        // resultat deja calcule: on le reutilise directement (memoisation)
        if (!Double.isNaN(memo[mask])) {
            return memo[mask];
        }

        // prend le premier noeud impair encore dans le mask
        int i = Integer.numberOfTrailingZeros(mask);
        // mask sans le noeud i
        int maskSansI = mask & ~(1 << i);

        double best = Double.POSITIVE_INFINITY;
        int bestJ = -1;

        // essaie d apparier i avec chaque autre j encore dans le mask
        for (int j = i + 1; j < dist.length; j++) {
            if ((maskSansI & (1 << j)) == 0) {
                continue;
            }

            // mask restant apres avoir apparie i et j
            int suivant = maskSansI & ~(1 << j);
            double cout = dist[i][j] + coutMatchingExactDp(suivant, dist, memo, choix);

            if (cout < best) {
                best = cout;
                bestJ = j;
            }
        }

        // sauvegarde le meilleur choix et le cout pour ce mask
        choix[mask] = bestJ;
        memo[mask] = best;
        return best;
    }

    // ==============================
    // Algo TSP
    // ==============================

    public Graphe glouton() {
        // Cas limite: pas de noeud -> pas de tour
        if (noeuds.isEmpty()) {
            return null;
        }

        // chemin = ordre de visite qu on construit
        ArrayList<Noeud> chemin = new ArrayList<>();
        // noeuds qu il reste a visiter
        ArrayList<Noeud> nonVisites = new ArrayList<>(noeuds);
        Set<Noeud> nonVisitesSet = new HashSet<>(nonVisites);

        // depart aleatoire pour eviter toujours le meme tour
        int index = (int) (Math.random() * nonVisites.size());
        Noeud courant = nonVisites.get(index);
        chemin.add(courant);
        nonVisites.remove(courant);
        nonVisitesSet.remove(courant);

        // a chaque etape on prend le voisin non visite le plus proche
        while (!nonVisites.isEmpty()) {
            Noeud plusProche = null;
            double minDistance = Double.MAX_VALUE;

            // on regarde les arcs du noeud courant
            for (Arc arc : courant.getArcs()) {
                // on recupere l autre extremite de l arc
                Noeud voisin = arc.getN1().getId() == courant.getId() ? arc.getN2() : arc.getN1();

                // si pas encore visite
                if (nonVisitesSet.contains(voisin)) {
                    // la distance est deja stockee dans l arc
                    double dist = arc.getValeur();
                    if (dist < minDistance) {
                        minDistance = dist;
                        plusProche = voisin;
                    }
                }
            }

            // fallback: si graphe partiel bloque, on calcule direct les distances
            if (plusProche == null) {
                for (Noeud n : nonVisites) {
                    // distance geometrique directe
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

        // ferme le tour en revenant au depart
        fermerCycle(chemin);
        return construireTourDepuisChemin(chemin);
    }

    // Lance des benchmarks sur les algos TSP: mesure les temps d execution et les couts
    // sur 100 repetitions pour avoir des moyennes stables
    public void evaluerComplexite(boolean benchmarkKruskal, boolean benchmarkMM, boolean benchmarkChristofides) {
        int n = this.noeuds.size();
        int repetitions = 100;
        // 2-opt desactive si trop de noeuds (sinon ca prend trop de temps)
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

        // 100 runs glouton
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

        // 100 runs kruskal (MST)
        if (benchmarkKruskal) {
            for (int r = 0; r < repetitions; r++) {
                long debut = System.nanoTime();
                this.kruskal();
                totalKruskal += System.nanoTime() - debut;
            }
        }

        // 100 runs TSP via MST (mstApprox)
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

        // 100 runs minimumMatching (MST calcule hors chrono)
        if (benchmarkMM) {
            for (int r = 0; r < repetitions; r++) {
                Graphe mst = this.kruskal();
                long debut = System.nanoTime();
                this.minimumMatching(mst);
                totalMM += System.nanoTime() - debut;
            }
        }

        // 100 runs pipeline complet MST + MM
        if (benchmarkMM) {
            for (int r = 0; r < repetitions; r++) {
                long debut = System.nanoTime();
                Graphe mst = this.kruskal();
                this.minimumMatching(mst);
                totalMstPlusMM += System.nanoTime() - debut;
            }
        }

        // 100 runs Christofides (pipeline complet TSP)
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

        // nanoTime en nanosecondes -> divise par 10^9 pour avoir des secondes
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
        // graphe resultat pour stocker le MST
        Graphe mst = new Graphe();
        mst.isGeo = this.isGeo;

        // on clone les noeuds pour pas melanger avec le graphe source
        Map<Integer, Noeud> copieNoeuds = new HashMap<>();
        for (Noeud original : this.noeuds) {
            Noeud clone = new Noeud(original.getId(), original.getAbs(), original.getOrd());
            mst.getNoeuds().add(clone);
            copieNoeuds.put(clone.getId(), clone);
        }

        // 1) tri des arcs par distance croissante
        ArrayList<Arc> arcsTries = new ArrayList<>(this.arcs);
        arcsTries.sort((a, b) -> Double.compare(a.getValeur(), b.getValeur()));

        // 2) Union-Find pour eviter les cycles
        Map<Noeud, Noeud> parent = new HashMap<>();
        Map<Noeud, Integer> rank = new HashMap<>();

        // au debut chaque noeud est son propre parent
        for (Noeud n : this.noeuds) {
            parent.put(n, n);
            rank.put(n, 0);
        }

        // 3) parcourir les arcs tries
        for (Arc a : arcsTries) {

            // trouve les racines des deux noeuds
            Noeud r1 = find(parent, a.getN1());
            Noeud r2 = find(parent, a.getN2());

            // racines differentes -> pas de cycle
            if (r1 != r2) {

                // ajoute l arc en utilisant les noeuds clones du MST
                Noeud n1Mst = copieNoeuds.get(a.getN1().getId());
                Noeud n2Mst = copieNoeuds.get(a.getN2().getId());
                mst.addArc(n1Mst, n2Mst);

                // fusion des deux ensembles
                union(parent, rank, r1, r2);

                // un MST sur n noeuds a exactement n-1 arcs
                if (mst.getArcs().size() == this.noeuds.size() - 1) {
                    break;
                }
            }
        }

        return mst;
    }

    private void dfs(Noeud n, Set<Noeud> visite, ArrayList<Noeud> parcours) {

        // marque le noeud comme visite
        visite.add(n);

        // ajoute le noeud au parcours
        parcours.add(n);

        // regarde tous ses voisins
        for (Arc a : n.getArcs()) {

            // trouve le voisin (autre extremite de l arc)
            Noeud voisin = a.getN1().equals(n) ? a.getN2() : a.getN1();

            // si pas encore visite, on continue en profondeur
            if (!visite.contains(voisin)) {
                dfs(voisin, visite, parcours);
            }
        }
    }

    public Graphe mstApprox() {

        // 1) construire le MST
        Graphe mst = this.kruskal();

        if (mst.getNoeuds().isEmpty()) {
            return mst;
        }

        // 2) DFS pour obtenir un ordre de parcours
        ArrayList<Noeud> parcours = new ArrayList<>();
        Set<Noeud> visite = new HashSet<>();

        // on lance DFS sur toutes les composantes si le graphe est deconnecte
        for (Noeud depart : mst.getNoeuds()) {
            if (!visite.contains(depart)) {
                mst.dfs(depart, visite, parcours);
            }
        }

        // 3) supprime les doublons -> chemin hamiltonien
        ArrayList<Noeud> chemin = new ArrayList<>();
        Set<Noeud> dejaAjoutes = new HashSet<>();

        for (Noeud n : parcours) {
            // on garde juste la 1ere apparition
            if (dejaAjoutes.add(n)) {
                chemin.add(n);
            }
        }

        // 4) fermer le cycle (retour depart)
        fermerCycle(chemin);

        // 5) construire le graphe resultat
        return construireTourDepuisChemin(chemin);
    }

    // Circuit eulerien avec Hierholzer sur un multigraphe
    // Precondition: tous les noeuds doivent avoir un degre pair
    // Retourne la liste ordonnee des noeuds du circuit
    private ArrayList<Noeud> hierholzer(Map<Integer, Noeud> noeudsParId, ArrayList<Arc> arcs) {

        // Liste d adjacence: id -> [idVoisin, indexArc]
        // Chaque arc non oriente apparait 2 fois (une fois par extremite)
        Map<Integer, ArrayList<int[]>> adj = new HashMap<>();
        for (int i = 0; i < arcs.size(); i++) {
            Arc a = arcs.get(i);
            int id1 = a.getN1().getId();
            int id2 = a.getN2().getId();
            adj.computeIfAbsent(id1, x -> new ArrayList<>()).add(new int[] { id2, i });
            adj.computeIfAbsent(id2, x -> new ArrayList<>()).add(new int[] { id1, i });
        }

        // arcUtilise[i] = true si l arc i a deja ete pris
        boolean[] arcUtilise = new boolean[arcs.size()];

        // pointeur par noeud pour eviter de rescanner des arcs deja utilises
        Map<Integer, Integer> ptr = new HashMap<>();
        for (int id : adj.keySet()) {
            ptr.put(id, 0);
        }

        // depart: premier noeud dispo
        int depart = noeudsParId.keySet().iterator().next();

        // pile de Hierholzer (version iterative)
        Deque<Integer> pile = new ArrayDeque<>();
        ArrayList<Noeud> circuit = new ArrayList<>();
        pile.push(depart);

        while (!pile.isEmpty()) {
            int v = pile.peek();
            ArrayList<int[]> voisins = adj.getOrDefault(v, new ArrayList<>());
            int p = ptr.getOrDefault(v, 0);

            // avance jusqu au prochain arc non utilise
            while (p < voisins.size() && arcUtilise[voisins.get(p)[1]]) {
                p++;
            }
            ptr.put(v, p);

            if (p == voisins.size()) {
                // plus d arc depuis v -> on l ajoute au circuit
                circuit.add(noeudsParId.get(pile.pop()));
            } else {
                // emprunte l arc vers le voisin
                int[] suivant = voisins.get(p);
                arcUtilise[suivant[1]] = true;
                ptr.put(v, p + 1);
                pile.push(suivant[0]);
            }
        }

        return circuit;
    }

    // Algo de Christofides:
    // 1) MST
    // 2) matching minimum sur noeuds impairs
    // 3) fusion en multigraphe (degres pairs)
    // 4) circuit eulerien
    // 5) shortcut -> tour hamiltonien
    public Graphe christofides() {

        if (noeuds.isEmpty()) {
            return null;
        }

        // 1) MST
        Graphe mst = this.kruskal();

        // 2) matching minimum sur noeuds impairs
        Graphe mm = this.minimumMatching(mst);

        // 3) construit le multigraphe MST + MM
        // on mappe les IDs vers les noeuds source pour garder les bonnes coordonnees
        Map<Integer, Noeud> noeudsParId = new HashMap<>();
        for (Noeud n : this.noeuds) {
            noeudsParId.put(n.getId(), n);
        }

        // ajoute tous les arcs du MST puis du matching
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

        // 4) circuit eulerien sur le multigraphe
        ArrayList<Noeud> circuit = hierholzer(noeudsParId, tousArcs);

        if (circuit.isEmpty()) {
            return null;
        }

        // 5) shortcut: on garde la 1ere apparition de chaque noeud
        ArrayList<Noeud> chemin = new ArrayList<>();
        Set<Integer> dejaVus = new HashSet<>();
        for (Noeud n : circuit) {
            if (dejaVus.add(n.getId())) {
                chemin.add(n);
            }
        }

        // ferme le cycle
        fermerCycle(chemin);

        // 6) construit le graphe resultat
        return construireTourDepuisChemin(chemin);
    }

}
