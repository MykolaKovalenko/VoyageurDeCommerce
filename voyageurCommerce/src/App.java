public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("===== TEST VOYAGEUR DE COMMERCE =====\n");

        // Graphe g1 = new Graphe();
        // g1.importer("voyageurCommerce/data/14_points.csv");

        // for(int i = 0; i < 10; i++) {
        // Graphe g2 = g1.glouton();
        // System.out.println("Cout du graphe : " + g2.cout());
        // }

        // Graphe g3 = new Graphe();
        // g3.importer("voyageurCommerce/data/14_points.csv");
        // g3.partiel(13);
        // g3.afficher();
        // System.out.println("Cout du graphe : " + g3.cout());

        Graphe g4 = new Graphe();
        g4.importer("voyageurCommerce/data/80_points.csv");
        g4.evaluerComplexite();
        
    }
}
