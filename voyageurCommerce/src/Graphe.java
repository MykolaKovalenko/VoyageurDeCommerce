import java.util.ArrayList;

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
}
