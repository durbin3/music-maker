import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import java.util.*;

//dotted quarter subdivisions:
//    quarter eighth
//    three eights
//

class BeatFraction {
    int num;
    int denom;
    BeatFraction(int num,int denom) {
        this.num = num;
        this.denom = denom;
    }
    double toDouble() {
        return (double)num/(double)denom;
    }
    BeatFraction times(BeatFraction other) {
        return new BeatFraction(this.num*other.num,this.denom*other.denom);
    }
}
class Subdivider {
    static ArrayList<ArrayList<ArrayList<Integer>>> shortcut = new ArrayList();
    static ArrayList<ArrayList<Integer>> allpartitions(int tot) {
        while (tot>=shortcut.size()) {
            ArrayList<ArrayList<Integer>> res = new ArrayList();
            ArrayList<Integer> temptemp = new ArrayList();
            temptemp.add(shortcut.size()+1);
            res.add(temptemp);
            for (int i = 0; i < shortcut.size(); i++) {
                for (ArrayList<Integer> possibility:shortcut.get(i)) {
                    ArrayList<Integer> jeu = new ArrayList(possibility);
                    jeu.add(shortcut.size() - i);
                    res.add(jeu);
                }
            }
            shortcut.add(res);
        }
        return shortcut.get(tot);
    }
    ArrayList<ArrayList<Double>> weights;
    ArrayList<Integer> subdivseries;
    Subdivider(ArrayList<ArrayList<Double>> weights,ArrayList<Integer> subdivseries) {
        this.weights = weights;
        this.subdivseries = subdivseries;
    }
    Subdivider(int[] subdivseries) {
        this.weights = new ArrayList();
        this.subdivseries = new ArrayList();
        for (int i:subdivseries) {
            this.subdivseries.add(i-1);
        }
    }
    ArrayList<Double> getWeights(int subdiv) {
        allpartitions(subdiv);
        while(weights.size()<=subdiv) {
            ArrayList<Double> nlayer = new ArrayList();
            for (int i = 0; i < shortcut.get(weights.size()).size(); i++) { nlayer.add(1.0); }
            weights.add(nlayer);
        }
        return weights.get(subdiv);
    }
    void setWeight(int subdiv,int[] notes,double nweight) {
        getWeights(subdiv-1);
        for (int i=0;i<shortcut.get(subdiv-1).size();i++) {
            if (notes.length != shortcut.get(subdiv-1).get(i).size()) {continue;}
            boolean same = true;
            for (int j = 0; j < notes.length; j++) {
                if (shortcut.get(subdiv-1).get(i).get(j) != notes[j]) {
                    same = false;
                    break;
                }
            }
            if (same) {
                weights.get(subdiv-1).set(i,nweight);
            }
        }
    }
    Subdivider next() {
        return new Subdivider(weights, (ArrayList<Integer>) subdivseries.subList(1,subdivseries.size()));
    }
    ArrayList<Tree> subdivide(BeatFraction beats) {
        if (subdivseries.size()==0) {return null;}
        int subdiv = subdivseries.get(0);
        ArrayList<Double> wrow = getWeights(subdiv);
        double total = 0;
        for (int i = 0; i < wrow.size(); i++) {total += wrow.get(i);}
        double rand = Math.random()*total;
        for (int i = 0; i < wrow.size(); i++) {
            rand -= wrow.get(i);
            if (rand < 0) {
                if (i == 0) {return null;}//they don't think it be like it is... but it do...
                ArrayList<Tree> res = new ArrayList();
                for (Integer not:allpartitions(subdiv).get(i)) {
                    beats.times(new BeatFraction(not,subdiv+1));
                }
                return res;
            }
        }
        return null;
    }
}
class Tree {
    BeatFraction beats;
    ArrayList<Tree> subtrees;
    public Tree(BeatFraction beats) {
        this.beats = beats;
    }
    public void subdivide(Subdivider subdivider){
        if (subtrees == null) {
            subtrees = subdivider.subdivide(beats);
        }
        if (subtrees != null) {//i know this looks weird... but this is how it gotta be...
            for (Tree t : subtrees) {
                t.subdivide(subdivider.next());
            }
        }
    }
    public void populate(ArrayList<BeatFraction> rhythm) {
        if (subtrees == null) {
            rhythm.add(beats);//what a line
        } else {
            for (Tree t : subtrees) {
                t.populate(rhythm);
            }
        }
    }
}



public class RhythmPlayer {
    public static void main(String[] args) throws InvalidMidiDataException, MidiUnavailableException {
        Scanner s = new Scanner(System.in);
        MusicPlayer mp = new MusicPlayer();
        System.out.println("Welcome to the rhythm generator.");
        boolean run = false;
        do{
            mp.play();
            System.out.println("Would you like to rerun the program?");
            run = s.nextLine().equals("yes");
        } while(run);
    }

}
