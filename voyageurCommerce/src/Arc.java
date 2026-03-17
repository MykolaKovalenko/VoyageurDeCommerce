public class Arc {
    private Noeud n1;
    private Noeud n2;
    private float valeur;

    public Arc(Noeud n1, Noeud n2){
        this.n1 = n1;
        this.n2 = n2;
    }

    public Arc(Noeud n1, Noeud n2, float valeur){
        this.n1 = n1;
        this.n2 = n2;
        this.valeur = valeur;
    }

    public float getValeur(){
        return valeur;
    }

    public void setValeur(float valeur){
        this.valeur = valeur;
    }

}
