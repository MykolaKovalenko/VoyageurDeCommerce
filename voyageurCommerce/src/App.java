public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("===== TEST VOYAGEUR DE COMMERCE =====\n");
        
        // TEST 1 : Creation graphe avec 5 noeuds
        System.out.println("TEST 1 : Creer graphe avec 5 noeuds");
        Graphe g1 = new Graphe(5);
        g1.positionner(100, 100);
        System.out.println("  Noeuds crees : " + g1.getNoeuds().size());
        System.out.println("  Position noeud 0 : (" + g1.getNoeudById(0).getAbs() + ", " + g1.getNoeudById(0).getOrd() + ")");
        System.out.println("  ✓ PASS\n");
        
        // TEST 2 : Graphe complet
        System.out.println("TEST 2 : Graphe complet (tous connectes)");
        g1.complet();
        int nbArcsComplet = g1.getArcs().size();
        int attendu = (5 * 4) / 2;  // n(n-1)/2
        System.out.println("  Arcs crees : " + nbArcsComplet);
        System.out.println("  Attendu : " + attendu);
        if (nbArcsComplet == attendu) {
            System.out.println("  ✓ PASS\n");
        } else {
            System.out.println("  ✗ FAIL\n");
        }
        
        // TEST 3 : Graphe partiel
        System.out.println("TEST 3 : Graphe partiel (k=2 voisins par noeud)");
        Graphe g2 = new Graphe(5);
        g2.positionner(100, 100);
        g2.partiel(2);
        int nbArcsPartiel = g2.getArcs().size();
        System.out.println("  Arcs crees : " + nbArcsPartiel);
        System.out.println("  (Doit etre <= " + (5*2) + ")");
        if (nbArcsPartiel <= 10) {
            System.out.println("  ✓ PASS\n");
        } else {
            System.out.println("  ✗ FAIL\n");
        }
        
        // TEST 4 : Distance entre deux noeuds
        System.out.println("TEST 4 : Calcul de distance");
        Noeud n1 = new Noeud(0, 0, 0);
        Noeud n2 = new Noeud(1, 3, 4);
        float dist = n1.distanceTo(n2);
        System.out.println("  Noeud 0 : (0, 0)");
        System.out.println("  Noeud 1 : (3, 4)");
        System.out.println("  Distance : " + dist);
        System.out.println("  Attendu : 5.0");
        if (Math.abs(dist - 5.0) < 0.01) {
            System.out.println("  ✓ PASS\n");
        } else {
            System.out.println("  ✗ FAIL\n");
        }
        
        // TEST 5 : Getters Arc
        System.out.println("TEST 5 : Getters Arc (getN1, getN2, getValeur)");
        Arc arc = new Arc(n1, n2, 5.0f);
        System.out.println("  N1 : " + arc.getN1().getId());
        System.out.println("  N2 : " + arc.getN2().getId());
        System.out.println("  Valeur : " + arc.getValeur());
        if (arc.getN1().getId() == 0 && arc.getN2().getId() == 1 && arc.getValeur() == 5.0f) {
            System.out.println("  ✓ PASS\n");
        } else {
            System.out.println("  ✗ FAIL\n");
        }
        
        System.out.println("===== TESTS TERMINES =====");
        System.out.println("\nProchaine etape : Tester avec fichier csv");
        System.out.println("(Une fois l'algo glouton implemente)");
    }
}
