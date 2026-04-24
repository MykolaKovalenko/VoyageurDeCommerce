import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class App {

    // =========================
    // Config du test
    // =========================
    // CSV a tester (dans data/)
    private static final String NOM_FICHIER_CSV = "16-points (avec-virgules).csv";

    // Generation optionelle d'un CSV de test.
    // true  -> genere un nouveau fichier puis l'utilise
    // false -> garde NOM_FICHIER_CSV
    private static final boolean GENERER_CSV = false;
    private static final String NOM_FICHIER_GENERE = "2Driri500.csv";
    private static final int NOMBRE_NOEUDS_GENERES = 500;
    // Type du fichier genere: "GEO" ou "2D"
    private static final String TYPE_CSV_GENERE = "2D";

    private static final int K_VOISINS_PARTIEL = 3;

    private static final boolean AFFICHER_KRUSKAL = false;
    private static final boolean AFFICHER_MM = false;

    private static final boolean BENCHMARK_KRUSKAL = false;
    private static final boolean BENCHMARK_MM = false;
    
    private static final boolean TESTER_CHRISTOFIDES = true;
    private static final boolean BENCHMARK_CHRISTOFIDES = true;

    private static void afficherResultat(String nom, Graphe resultat) {
        if (resultat == null) {
            System.out.println(nom + ": aucun resultat");
            return;
        }
        System.out.println(nom + ": noeuds=" + resultat.getNoeuds().size()
                + " | arcs=" + resultat.getArcs().size()
                + " | cout=" + resultat.cout());
    }

    private static void testerAlgorithmes(String label, Graphe g) {
        System.out.println("\n--- Test sur graphe " + label + " ---");
        System.out.println("Noeuds: " + g.getNoeuds().size() + " | Arcs: " + g.getArcs().size());

        Graphe tourGlouton = g.glouton();
        boolean besoinMst = AFFICHER_KRUSKAL || AFFICHER_MM || BENCHMARK_MM || TESTER_CHRISTOFIDES;
        Graphe mst = besoinMst ? g.kruskal() : null;
        Graphe mm = null;
        String erreurMM = null;
        if (AFFICHER_MM && mst != null) {
            try {
                mm = g.minimumMatching(mst);
            } catch (IllegalStateException e) {
                erreurMM = e.getMessage();
            }
        }
        Graphe tourTspViaMst = g.mstApprox();
        Graphe tourChristofides = null;
        String erreurChristofides = null;
        if (TESTER_CHRISTOFIDES) {
            try {
                tourChristofides = g.christofides();
            } catch (IllegalStateException e) {
                erreurChristofides = e.getMessage();
            }
        }

        afficherResultat("glouton", tourGlouton);
        if (AFFICHER_KRUSKAL && mst != null) {
            afficherResultat("kruskal (MST, pas un TSP)", mst);
        }
        if (AFFICHER_MM) {
            if (mm != null) {
                afficherResultat("minimumMatching", mm);
            } else {
                System.out.println("minimumMatching: indisponible (" + erreurMM + ")");
            }
        }
        afficherResultat("tsp via MST (mstApprox)", tourTspViaMst);
        if (TESTER_CHRISTOFIDES) {
            if (tourChristofides != null) {
                afficherResultat("christofides", tourChristofides);
            } else {
                System.out.println("christofides: indisponible (" + erreurChristofides + ")");
            }
        }

        StringBuilder benchmarkLabel = new StringBuilder("Benchmark: 100 iterations (glouton, tsp-via-MST");
        if (BENCHMARK_KRUSKAL) {
            benchmarkLabel.append(", kruskal/MST");
        }
        if (BENCHMARK_MM) {
            benchmarkLabel.append(", MM");
        }
        if (BENCHMARK_CHRISTOFIDES) {
            benchmarkLabel.append(", christofides");
        }
        benchmarkLabel.append(")");
        System.out.println(benchmarkLabel.toString());
        try {
            g.evaluerComplexite(BENCHMARK_KRUSKAL, BENCHMARK_MM, BENCHMARK_CHRISTOFIDES);
        } catch (IllegalStateException e) {
            System.out.println("Benchmark partiel: MM exact/christofides indisponible sur cette taille (" + e.getMessage() + ")");
        }
    }

    // Affiche quelques CSV dispo dans data/.
    public static void afficherFichiersDisponibles() {
        System.out.println("\nFichiers CSV disponibles dans data/ :");
        System.out.println("   - 14-points.csv (14 villes, rapide)");
        System.out.println("   - 14-points (avec point et non virgule pour les doubles) (2).csv");
        System.out.println("   - 16-points (avec-points).csv");
        System.out.println("   - 16-points (avec-virgules).csv");
        System.out.println("   - 52-points (avec-points).csv");
        System.out.println("   - 52-points (avec-virgules).csv");
        System.out.println("   - generated_*.csv (fichiers aleatoires)");
        System.out.println("\nModifier NOM_FICHIER_CSV en haut du fichier App.java pour en choisir un.\n");
    }

    private static String formaterNombreCsv(double valeur) {
        return String.format("%.2f", valeur).replace('.', ',');
    }

    private static double versGeoTsplib(double valeurDecimale) {
        int degres = (int) valeurDecimale;
        double minutes = (valeurDecimale - degres) * 60.0;
        return degres + (minutes / 100.0);
    }

    // Genere un CSV de n noeuds en GEO ou en 2D.
    private static void genererCSV(String cheminFichier, int n, String type) {
        String typeNormalise = type == null ? "2D" : type.trim().toUpperCase();
        if (!"GEO".equals(typeNormalise) && !"2D".equals(typeNormalise)) {
            throw new IllegalArgumentException("TYPE_CSV_GENERE doit etre 'GEO' ou '2D'.");
        }

        try (FileWriter writer = new FileWriter(cheminFichier)) {
            writer.write(typeNormalise + "\n");

            for (int i = 1; i <= n; i++) {
                double x;
                double y;

                if ("GEO".equals(typeNormalise)) {
                    double latitude = ThreadLocalRandom.current().nextDouble(-89.0, 89.0);
                    double longitude = ThreadLocalRandom.current().nextDouble(-179.0, 179.0);
                    x = versGeoTsplib(latitude);
                    y = versGeoTsplib(longitude);
                } else {
                    x = Math.random() * 1000;
                    y = Math.random() * 1000;
                }

                String ligne = i + ";"
                        + formaterNombreCsv(x) + ";"
                        + formaterNombreCsv(y) + "\n";
                writer.write(ligne);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la generation du CSV : " + e.getMessage());
        }
    }

    // Trouve le bon chemin du CSV (peu importe le dossier courant).
    private static String resoudreChemin(String nomFichier) {
        java.io.File fichier;
        
        // Essai 1: depuis la racine du projet (voyageurCommerce/data/)
        fichier = new java.io.File("voyageurCommerce/data/" + nomFichier);
        if (fichier.exists()) {
            return fichier.getPath();
        }
        
        // Essai 2: depuis le dossier voyageurCommerce (data/)
        fichier = new java.io.File("data/" + nomFichier);
        if (fichier.exists()) {
            return fichier.getPath();
        }
        
        // Sinon on retourne le chemin "racine" par defaut
        return "voyageurCommerce/data/" + nomFichier;
    }

    // Retourne un chemin d'ecriture dans data/ selon ou on lance le prog.
    private static String resoudreCheminEcritureData(String nomFichier) {
        File dossierRacine = new File("voyageurCommerce/data");
        if (dossierRacine.exists() && dossierRacine.isDirectory()) {
            return new File(dossierRacine, nomFichier).getPath();
        }

        File dossierLocal = new File("data");
        if (dossierLocal.exists() && dossierLocal.isDirectory()) {
            return new File(dossierLocal, nomFichier).getPath();
        }

        return "voyageurCommerce/data/" + nomFichier;
    }

    // Charge un graphe depuis un CSV.
    private static Graphe chargerGraphe(String cheminFichier) {
        Graphe g = new Graphe();
        g.importer(cheminFichier);
        return g;
    }

    public static void main(String[] args) {
        System.out.println("===== TEST VOYAGEUR DE COMMERCE =====\n");

        String nomFichierActif = GENERER_CSV ? NOM_FICHIER_GENERE : NOM_FICHIER_CSV;

        if (GENERER_CSV) {
            String cheminSortie = resoudreCheminEcritureData(NOM_FICHIER_GENERE);
            System.out.println("Generation CSV activee: " + NOM_FICHIER_GENERE
                    + " | n=" + NOMBRE_NOEUDS_GENERES
                    + " | type=" + TYPE_CSV_GENERE);
            genererCSV(cheminSortie, NOMBRE_NOEUDS_GENERES, TYPE_CSV_GENERE);
        }

        // Trouver le bon chemin du fichier
        String cheminFichier = resoudreChemin(nomFichierActif);

        System.out.println("Fichier CSV : " + nomFichierActif);
        System.out.println("Parametres: k=" + K_VOISINS_PARTIEL);

        // 1) Chargement du graphe source
        Graphe grapheSource = chargerGraphe(cheminFichier);

        if (grapheSource.getNoeuds().isEmpty()) {
            System.err.println("\nErreur: le fichier n'a pas pu etre charge ou est vide.");
            afficherFichiersDisponibles();
            return;
        }

        int n = grapheSource.getNoeuds().size();
        System.out.println("Fichier charge avec " + n + " villes\n");

        // 2) Test sur graphe partiel
        Graphe graphePartiel = chargerGraphe(cheminFichier);
        graphePartiel.partiel(K_VOISINS_PARTIEL);
        testerAlgorithmes("PARTIEL", graphePartiel);

        // 3) Test sur graphe complet
        Graphe grapheComplet = chargerGraphe(cheminFichier);
        grapheComplet.complet();
        testerAlgorithmes("COMPLET", grapheComplet);
    }
}