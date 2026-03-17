import java.util.ArrayList;

public class Noeud {
    private int id;
    private float abs;
    private float ord;
    private ArrayList<Arc> arcs;

    public Noeud(int id, float abs, float ord) {
        this.id = id;
        this.abs = abs;
        this.ord = ord;
        this.arcs = new ArrayList<>();
    }

    //getters
    public int getId(){
        return id;
    }
    public float getAbs(){
        return abs;
    }
    public float getOrd(){
        return ord;
    }
    public ArrayList<Arc> getArcs(){
        return arcs;
    }

    //setters
    public void setX(float abs){
        this.abs = abs;
    }
    public void setY(float ord){
        this.ord = ord;
    }

    // distance jusqua noeud n
    public float distanceTo(Noeud n) {
        double dAbs = this.abs - n.getAbs();
        double dOrd = this.ord - n.getOrd();
        return (float) Math.sqrt(dAbs * dAbs + dOrd * dOrd);
    }
    

}

