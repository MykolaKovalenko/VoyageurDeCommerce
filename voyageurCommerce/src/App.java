public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("===== TEST VOYAGEUR DE COMMERCE =====\n");

        Graphe g1 = new Graphe();
        g1.importer("voyageurCommerce/data/14_points.csv");
        g1.complet();
        // g1.afficher();

        Graphe g2 = g1.glouton();
        // g2.afficher();

        Graphe g3 = new Graphe();
        g3.importer("voyageurCommerce/data/14_points.csv");
        g3.partiel(2);
        g3.afficher();
       
        // Graphe g4 = new Graphe();
        // g4.importer("voyageurCommerce/data/14_points_avec_point.csv");
        // g4.partielOptimise(5);
        // g4.afficher();
    }
}
