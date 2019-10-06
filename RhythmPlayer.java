import javax.sound.midi.*;
import java.lang.reflect.Array;
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
    Boolean greaterThanEqualTo(BeatFraction other) {
        return this.num*other.denom>=other.num*this.denom;
    }
    BeatFraction plus(BeatFraction other) {
        return new BeatFraction(this.num*other.denom+other.num*this.denom,this.denom*other.denom);
    }
    public BeatFraction minus(BeatFraction other) {
        return new BeatFraction(this.num*other.denom-other.num*this.denom,this.denom*other.denom);
    }
    public void setToZero(){
        this.num = 0;
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
        ArrayList<Integer> nnex = new ArrayList();
        for (int i=1;i<subdivseries.size();i++) {
            nnex.add(subdivseries.get(i));
        }
        return new Subdivider(weights,nnex);
    }
    ArrayList<Tree> subdivide(BeatFraction beats) {
        if (subdivseries.size()==0) {return null;}
        System.out.println("subdividing...");
        int subdiv = subdivseries.get(0);
        System.out.print("subdiv: ");
        System.out.println(subdiv);
        ArrayList<Double> wrow = getWeights(subdiv);
        double total = 0;
        for (int i = 0; i < wrow.size(); i++) {total += wrow.get(i);}
        double orand = Math.random()*total;
        double rand = orand;
        for (int i = 0; i < wrow.size(); i++) {
            rand -= wrow.get(i);
            if (rand < 0) {
                if (i == 0) {return null;}//they don't think it be like it is... but it do...
                ArrayList<Tree> res = new ArrayList();
                for (Integer not:allpartitions(subdiv).get(i)) {
                    res.add(new Tree(beats.times(new BeatFraction(not,subdiv+1))));
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
//            System.out.println(beats.num);
            rhythm.add(beats);//what a line
        } else {
            for (Tree t : subtrees) {
                t.populate(rhythm);
            }
        }
    }
}



public class RhythmPlayer {


    public void createNote(int note, int tick,int length, Track track) throws InvalidMidiDataException {
        ShortMessage a = new ShortMessage();
        a.setMessage(144,1,note,100); //144 = on, 1 = keyboard, 44 = note, 100 = how loud and hard
        MidiEvent noteOn = new MidiEvent(a, tick); // start at tick 1
        track.add(noteOn);
        ShortMessage b = new ShortMessage();
        b.setMessage(128, 1, note, 100); //note off
        MidiEvent noteOff = new MidiEvent(b, tick+length); // stop at tick 16
        track.add(noteOff);
    }
    public void play() throws MidiUnavailableException, InvalidMidiDataException {
//            GUI();
        Sequencer player = MidiSystem.getSequencer();
        player.open();
        Sequence seq = new Sequence(Sequence.PPQ, 4);
        Track track = seq.createTrack();

        Scanner s = new Scanner(System.in);
        System.out.println("How many measures of phat beats do you want?");
        int progresLen = s.nextInt();
        s.nextLine();

        System.out.println("2/4, 3/4, 4/4, 6/8, 9/8, or 12/8?");
        String type = s.nextLine().toLowerCase();
        Subdivider subdivider = null;
        Tree tree = null;
        ArrayList<BeatFraction> beats = new ArrayList();
        for (int i = 0; i < progresLen; i++) {
        if(type.equals("2/4")) {
            subdivider = new Subdivider(new int[]{2,2});//the last number here is a guess... it's probably two... (for all of these)
            tree = new Tree(new BeatFraction(1,2));
        } else if(type.equals("3/4")) {
            subdivider = new Subdivider(new int[]{3,2});
            tree = new Tree(new BeatFraction(3,4));
        } else if(type.equals("4/4")) {
            subdivider = new Subdivider(new int[]{2,2,2});
            tree = new Tree(new BeatFraction(1,1));
        } else if(type.equals("6/8")) {
            subdivider = new Subdivider(new int[]{2,3,2});
            tree = new Tree(new BeatFraction(3,4));
        } else if(type.equals("9/8")) {
            subdivider = new Subdivider(new int[]{3,3,2});
            tree = new Tree(new BeatFraction(9,8));
        } else if(type.equals("12/8")) {
            subdivider = new Subdivider(new int[]{2,2,3,2});
            tree = new Tree(new BeatFraction(6,4));
        }
            tree.subdivide(subdivider);
            tree.populate(beats);
        }

        for(BeatFraction not: beats){
            System.out.print(not.num);
            System.out.print("/");
            System.out.print(not.denom);
            System.out.println();
        }
        int tick = 1;
        int octave = 52;
        //tick, octave, array list of Notes,
        for (BeatFraction note : beats){
            int newticks = (int)(note.toDouble()*16);
            createNote(octave,tick,newticks,track);
            tick += newticks;
        }
        player.setSequence(seq);
        player.setTempoInBPM(180);
        player.start();


    }
    public static void main(String[] args) throws InvalidMidiDataException, MidiUnavailableException {
        Scanner s = new Scanner(System.in);
        RhythmPlayer mp = new RhythmPlayer();
        System.out.println("Welcome to the rhythm generator.");
        boolean run = false;
        do{
            mp.play();
            System.out.println("Would you like to rerun the program?");
            run = s.nextLine().equals("yes");
        } while(run);
    }

}
