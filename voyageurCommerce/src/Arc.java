// Un arc relie deux noeuds et stocke la distance entre eux
public class Arc {
    private Noeud n1;      // premier noeud de l arc
    private Noeud n2;      // deuxieme noeud de l arc
    private double valeur; // distance entre n1 et n2

    public Arc(Noeud n1, Noeud n2, double valeur){
        this.n1 = n1;
        this.n2 = n2;
        this.valeur = valeur;
    }

    public double getValeur(){
        return valeur;
    }
    public Noeud getN1(){
        return n1;
    }

    public Noeud getN2(){
        return n2;
    }

    public void setValeur(double valeur){
        this.valeur = valeur;
    }
}
