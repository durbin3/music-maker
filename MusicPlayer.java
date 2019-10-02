import com.sun.jdi.connect.Connector;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.ls.LSOutput;

import java.util.*;
import javax.crypto.spec.PSource;
import javax.sound.midi.*;
import javax.swing.*;

//TODO: connect rhythm to music chords
//TODO: chord inversions

class Notes {
    ArrayList<Integer> notes = new ArrayList<Integer>();
    static Integer[] major_scale = new Integer[]{1, 3, 5, 6, 8, 10, 12, 13};
    static Integer[] minor_scale = new Integer[]{1, 3, 4, 6, 8, 9, 11, 13};
    boolean minor;

    Notes(String chord) {
        if("cdefgab".contains(chord.toLowerCase())){
            notes.add(major_scale["cdefgab".indexOf(chord.toLowerCase().charAt(0))]);
        }
        else {
            if(chord.toLowerCase().contains("n")){ // adfsadf = new Note("N6")
                notes.add(major_scale[1] - 1);
            }
        }
        int root = notes.get(0);
        minor = chord.substring(0,1).toLowerCase().equals(chord.substring(0,1));
        if(minor)
        {
            notes.add(notes.get(0) + 3);
            notes.add(notes.get(0) + 7);
        } else {
            notes.add(notes.get(0) + 4);
            notes.add(notes.get(0) + 7);
        }
        if(chord.contains("7")) { notes.add(minor ? root + 10 : root + 11);}
        if(chord.contains("9")) { notes.add(minor ? root + 14 : root + 14);}
    }//constructor
}

class Nodes {
    String chord; //C, g7 Am7

    Nodes(String ch){
        chord = ch;;
    }
    Notes getNotes() {
        return new Notes(chord);
    }


}
class Connectors {
    int start; //index of the start node
    int end; ////index of the end   node
    double weight;

    static String[] roman = new String[]{"i","ii","iii","iv","v","vi","vii", "n", "nn", "r", "ni","nii","niii","niv","nv","nvi","nvii","mn","nr","mr","mi","mii","miii","miv","mv","mvi","mvii"};
    ///////////////////                  "C","D",  "E",  "F","G", "A", "B",  "N", "Ni", "R", "Ci", "Di", "Ei", "Fi", "Gi", "Ai", "Bi" , "MN", "NR", "MR","Cii",D////////
    static int toval(String inp) {
        for (int i=0;i<roman.length;i++) {
            if (roman[i].equals(inp)) {
                return i;
            }
        }
        return 0;
    }

    public Connectors(int s, int e, double w){
        start = s;
        end = e;
        weight = w;
    }

    public Connectors(String s, String s1, String s2) {  /// new Connectors("ii","iv",".48"); previous >> (1,3,.49)
        start = toval(s);
        end = toval(s1);
        weight = Double.parseDouble(s2);
    }
}

class Path {
    double weight = 1.0; // product of the weights of every single path traveled
    ArrayList<Integer> paths; // [1,2,3,4] << connect to the indices of Nodes << order of nodes traveled
    Path thenTo(Connectors along) {
        // returns a Path object with (.weight, .paths >> [1,2,3,1]
        ArrayList<Integer> newPathist = new ArrayList<Integer>(paths);
        newPathist.add(along.end);
        return new Path(newPathist, weight * along.weight);
    }
    Path concatenate(Path then) {
        ArrayList<Integer> newPathist = new ArrayList<Integer>(paths);
        newPathist.addAll(then.paths);
        return new Path(newPathist,weight * then.weight);
    }
    Path(int start) {
        //construct a path based on the starting Node index
        paths = new ArrayList<Integer>();
        paths.add(start);
    }
    Path(int start,double w) {
        //construct a path based on the starting Node index
        paths = new ArrayList<Integer>();
        paths.add(start);
        weight = w;
    }
    private Path(ArrayList<Integer> npath,double w) {
        weight = w;
        paths = npath;
    }
}
class Graph {
    ArrayList<Nodes> nodes = new ArrayList<Nodes>(); // array [Nodes, Nodes, Nodes] >> ["C", "g", "a7"] >> "C".getNote().root >> 1 or 3 or 5 or etc.
    ArrayList<Connectors> connectors = new ArrayList<Connectors>(); // array [Connectors, Connectors] || Connectors >> .start, .end, .weight
    ArrayList<Connectors> getConnected(int node) { // returns array of connectors that connect to a specific node
        ArrayList<Connectors> connectionList = new ArrayList<Connectors>();
        for(Connectors connect : connectors){   // loop through each connection in the graph (connections array) and compares the start index to the current NODE
            if(connect.start == node) { // if they are equal add the connection to the possible connection list
                connectionList.add(connect);
            }
        }
        return connectionList;
    }
    HashMap<Pair<Integer,Integer>,Path> rando;
    ArrayList<Notes> randomProgression(int length,int start,int end) {
        rando = new HashMap();
        return toMusical(randomPathTo(length,start,end));
    }
    private Path randomPathTo(int length,int lastnode,int endnode) {
//        int lastnode = startpath.paths.get(startpath.paths.size()-1);
        Pair<Integer,Integer> key = new ImmutablePair(length,lastnode);
        if (rando.containsKey(key)) {return rando.get(key);}
        if (length == 1) {
            if (lastnode == endnode) {return new Path(lastnode);}
            return null;
        }
        ArrayList<Connectors> cArray = getConnected(lastnode);
        ArrayList<Pair<Path,Double>> pArray = new ArrayList();

        double rand = Math.random();
        for (Connectors connect:cArray) {
            Path nextpath = randomPathTo(length-1,connect.end,endnode);
            if (nextpath != null) {
                pArray.add(new ImmutablePair(nextpath,connect.weight));
            }
        }
        Path co = null;
        double totalWeight = 0;
        for(Pair<Path,Double> path : pArray) {
            totalWeight += path.getRight();
        }
        for (Pair<Path,Double> path : pArray) {
            rand -= path.getRight()/totalWeight; //e.g. rand = .39, .39 - .4 <0, co = this one
            if (rand < 0) {
                co = new Path(lastnode,path.getRight()).concatenate(path.getLeft());
                break;
            }
        }
        rando.put(key,co);
        return co;
    }
    ArrayList<Notes> toMusical(Path p) {
        //[1,2,5,1]
        ArrayList<Notes> notes= new ArrayList<Notes>();
        for (int index : p.paths){
            notes.add(nodes.get(index).getNotes());
        }
        return notes;
    }
    void updateNodes(String[] strings) {
        nodes.clear();
        for (String g : strings) {
            nodes.add(new Nodes(g));
        }
    }
    void updateConnections(String[][] strings) {
        connectors.clear();

        for(String[] c : strings){
            connectors.add(new Connectors(c[0],c[1],c[2]));
        }
    }
    private static Graph majorgraph = null;
    static Graph Majorgraph() {
        if (majorgraph != null) {return majorgraph;}
        majorgraph = new Graph();
        majorgraph.updateNodes(new String[]{"C","D","E","F","G","A","B","N","Ni","R","Ci","Di","Ei","Fi","Gi","Ai","Bi","Nii", "Ri","Rii", "Cii","Dii","Eii","Fii","Gii","Aii","Bii"});
                                    ////0,, 1   2   3   4   5   6   7   8    9   10   11   12   13   14   15   16   17     18   19     20     21    22    23    24    25    26
        majorgraph.updateConnections(new String[][]{
                //root chords
                {"i" , "ii" , ".18"},
                {"i" , "iii" , ".16"},
                {"i" , "iv" , ".18"},
                {"i" , "v" , ".16"},
                {"i" , "vi" , ".16"},
                {"i" , "vii" , ".16"},


                //"dissonant" chords
                {"iii","vi" , "1.0"},
                {"iii","vi" , "1.0"},
                {"vi", "ii" , "0.5"},
                {"vi", "iv" , "0.5"},

                //pre-dominants
                {"ii", "vii", "0.5"},
                {"ii", "v"  , "0.5"},
                {"iv", "v"  , "0.5"},
                {"iv", "vii", "0.2"},
                {"iv", "i"  , "0.3"},

                //dominants
                {"v" , "i"  , "1.0"},
                {"vii","i"  , "0.5"},
                {"vii","iii", "0.5"}
        });
        return majorgraph;
    }
    private static Graph minorgraph = null;
    static Graph Minorgraph() {
        if (minorgraph!=null) {return minorgraph;}
        minorgraph = new Graph();
        minorgraph.updateNodes(new String[]{"c","d","e","f","g","a","b","n","ni","r","ci","di","ei","fi","gi","ai","bi","nii", "ri","rii", "cii","dii","eii","fii","gii","aii","bii"});

        minorgraph.updateConnections(new String[][]{
                //root chords
                {"i", "ii", ".18"},
                {"i", "iii", ".16"},
                {"i", "iv", ".18"},
                {"i", "v", ".16"},
                {"i", "vi", ".16"},
                {"i", "vii", ".16"},


                //"dissonant" chords
                {"iii", "vi", "1.0"},
                {"vi", "ii", "0.5"},
                {"vi", "iv", "0.5"},

                //pre-dominants
                {"ii", "vii", "0.5"},
                {"ii", "v", "0.5"},
                {"iv", "v", "0.35"},
                {"iv", "vii", "0.25"},
                {"iv", "iii", "0.15"},
                {"iv", "i"  , "0.25"},

                //dominants
                {"v", "i", "1.0"},
                {"vii", "i", "1.0"}
        });
        return minorgraph;
    }
}// end graph

public class MusicPlayer {
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
        System.out.println("How long do you want the melody to be?");
        int progresLen = s.nextInt();
        s.nextLine();
        System.out.println("Major, or minor?");
        String mood = s.nextLine().toLowerCase();
        Graph graph = null;
        if(mood.equals("minor")) {
            graph = Graph.Minorgraph();
        } else if(mood.equals("major")) {
            graph = Graph.Majorgraph();
        }
        int measure_length = 16;

        //rhythm generator (parker's code, eric's comments)
        System.out.println("2/4, 3/4, 4/4, 6/8, 9/8, or 12/8?");
        String type = s.nextLine().toLowerCase();
        Subdivider subdivider = null;
        Tree tree = null;
        ArrayList<BeatFraction> beats = new ArrayList();
        //get the time signature and populate Tree with beats
        for (int i = 0; i < progresLen; i++) {
            if(type.equals("2/4")) {
                measure_length = 8;
                subdivider = new Subdivider(new int[]{2,2});//the last number here is a guess... it's probably two... (for all of these)
                tree = new Tree(new BeatFraction(1,2));
            } else if(type.equals("3/4")) {
                measure_length = 12;
                subdivider = new Subdivider(new int[]{3,2});
                tree = new Tree(new BeatFraction(3,4));
            } else if(type.equals("4/4")) {
                measure_length = 16;
                subdivider = new Subdivider(new int[]{2,2,2});
                tree = new Tree(new BeatFraction(1,1));
            } else if(type.equals("6/8")) {
                measure_length = 8;
                subdivider = new Subdivider(new int[]{2,3,2});
                tree = new Tree(new BeatFraction(3,4));
            } else if(type.equals("9/8")) {
                measure_length = 12;
                subdivider = new Subdivider(new int[]{3,3,2});
                tree = new Tree(new BeatFraction(9,8));
            } else if(type.equals("12/8")) {
                measure_length = 16;
                subdivider = new Subdivider(new int[]{2,2,3,2});
                tree = new Tree(new BeatFraction(6,4));
            }
            tree.subdivide(subdivider);
            tree.populate(beats);
        }


        // scales that may or may not be useful later
        Integer[] major_scale = new Integer[]{1, 3, 5, 6, 8, 10, 12, 13};
        Integer[] minor_scale = new Integer[]{1, 3, 4, 6, 8, 9, 11, 13};
        Integer[] blues_scale = new Integer[]{1, 4, 6, 7, 8, 11, 13};


        //make the chord notes and then add them to the track
        ArrayList<Notes> notes = graph.randomProgression(progresLen, 0, 0);
        System.out.println("::::::Chords:::::::");
        for(Notes not: notes){
            System.out.println(Arrays.asList(major_scale).indexOf(not.notes.get(0)) + 1);
        }
        int btick = 1;
        int ctick = 1;
        int octave = 52;
        //tick, octave, array list of Notes,
        for (Notes note : notes){

            //creates the base chord
            for(int index : note.notes) {
                createNote(index + octave, ctick,15, track);
            }
            ctick += measure_length;
        }
        //end of chord notes

        System.out.println(":::::::Rhythm junk::::::");
        for(BeatFraction not: beats){
            System.out.print(not.num);
            System.out.print("/");
            System.out.print(not.denom);
            System.out.println();
        }
        //add the rhythm notes to track
        System.out.println("Size===" + beats.size());
        int tickCounter = 1;
        int chordNumber = 0;
        int chordRoot;
        int newticks;
        for (BeatFraction note : beats) {
            newticks = (int) (note.toDouble() * 16);
            tickCounter += newticks;
            chordRoot = notes.get(chordNumber).notes.get(0);
            if(tickCounter >= 16) {
                tickCounter -= 16;
                chordNumber +=1;
            }
            createNote(chordRoot+ octave + 12, btick, newticks, track);
            btick += newticks;
        }


        player.setSequence(seq);
        player.setTempoInBPM(180);
        player.start();


    }
    public static void main(String[] args) throws InvalidMidiDataException, MidiUnavailableException {
        Scanner s = new Scanner(System.in);
        MusicPlayer mp = new MusicPlayer();
        System.out.println("Welcome to the music generator.");
        boolean run = false;
        do {
            mp.play();
            System.out.println("Would you like to rerun the program?");
            run = s.nextLine().equals("yes");
        } while(run);

    }

}