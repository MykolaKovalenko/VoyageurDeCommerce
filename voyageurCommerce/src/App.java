import java.io.FileWriter;
import java.io.IOException;

public class App {

    // Genere un fichier CSV de n noeuds aleatoires.
    // Ce fichier sert de base commune pour les tests partiel/complet.
    public static void genererCSV(String nomFichier, int n) {
        try (FileWriter writer = new FileWriter(nomFichier)) {

            // Premiere ligne
            writer.write("GEO\n");

            for (int i = 1; i <= n; i++) {
                // Coordonnees GEO aleatoires (x=longitude, y=latitude)
                double x = 10 + Math.random() * 20;
                double y = 90 + Math.random() * 10;

                String ligne = i + ";" +
                        String.format("%.2f", x).replace(".", ",") + ";" +
                        String.format("%.2f", y).replace(".", ",") + "\n";

                writer.write(ligne);
            }

            System.out.println("Fichier genere avec " + n + " noeuds !");
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 

    public static void main(String[] args) throws Exception {
        System.out.println("===== TEST VOYAGEUR DE COMMERCE =====\n");

        // 1) Preparation des donnees de test.
        // Meme donnee source pour rendre la comparaison plus juste.
        genererCSV("voyageurCommerce/data/1000.csv", 1000);

        // 2) Import des noeuds depuis le CSV.
        Graphe graphe = new Graphe();
        graphe.importer("voyageurCommerce/data/1000.csv");
        System.out.println("Noeuds importes : " + graphe.getNoeuds().size() + "\n");

        // 3) Construction d'un graphe partiel (chaque noeud relie a k voisins).
        int k = 4;
        System.out.println("Creation du graphe partiel avec k=" + k + "...");
        graphe.partiel(k);
        System.out.println("Arcs crees : " + graphe.getArcs().size() + "\n");

        // 4) Resolution approchee avec le plus proche voisin (glouton).
        // Le tour renvoye contient n+1 noeuds (retour au point de depart).
        System.out.println("Execution de l'algorithme glouton...");
        Graphe resultat = graphe.glouton();
        if (resultat != null) {
            System.out.println("Longueur du circuit : " + resultat.cout() + "\n");
        } else {
            System.out.println("Aucun circuit valide trouve sur ce graphe partiel.\n");
        }

        System.out.println("Evaluation complexite sur graphe partiel...");
        // Mesure le temps de glouton pour plusieurs tailles de sous-graphes.
        graphe.evaluerComplexite(true, k);

        // 5) Meme test sur un graphe complet pour comparer le comportement.
        System.out.println("\n--- Test avec graphe COMPLET ---");
        Graphe grapheComplet = new Graphe();
        grapheComplet.importer("voyageurCommerce/data/1000.csv");
        System.out.println("Creation du graphe complet...");
        grapheComplet.complet();
        System.out.println("Arcs crees : " + grapheComplet.getArcs().size());
        Graphe resultatComplet = grapheComplet.glouton();
        if (resultatComplet != null) {
            System.out.println("Longueur du circuit : " + resultatComplet.cout() + "\n");
        } else {
            System.out.println("Aucun circuit valide trouve sur ce graphe complet.\n");
        }

        System.out.println("Evaluation complexite sur graphe complet...");
        // Meme protocole, mais sur un graphe beaucoup plus dense.
        grapheComplet.evaluerComplexite(false, 0);
    }
}