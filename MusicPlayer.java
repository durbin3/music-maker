import com.sun.jdi.connect.Connector;
import org.w3c.dom.ls.LSOutput;

import java.util.*;
import javax.crypto.spec.PSource;
import javax.sound.midi.*;
import javax.swing.*;

//TODO: rhythm generator
//TODO: chord inversions

class Tree {
    Tree left = null;
    Tree right = null;
    int payload = 0;
    public void addfive() {
        payload+=5;
        if (left != null) {left.addfive();}
        if (right != null) {right.addfive();}
    }
    public void iterate(){
        if(Math.random() > .5) {
            if (left == null){
                left = new Tree();
            } else {
                left.iterate();
            }
        } else {
            if(right == null){
                right = new Tree();
            } else {
                right.iterate();
            }
        }
    }
}
class Notes {

    static Integer[] major_scale = new Integer[]{1, 3, 5, 6, 8, 10, 12, 13};
    static Integer[] minor_scale = new Integer[]{1, 3, 4, 6, 8, 9, 11, 13};
    int root;
    int third;
    int fifth;
    int seventh;
    int ninth;
    boolean minor;

    Notes(String chord) {
        if("cdefgab".contains(chord.toLowerCase())){ root = major_scale["cdefgab".indexOf(chord.toLowerCase().charAt(0))]; }
        else {
            if(chord.toLowerCase().contains("n")){ // adfsadf = new Note("N6")
                root = major_scale[1] - 1;
            }
        }
        minor = chord.substring(0,1).toLowerCase().equals(chord.substring(0,1));
        if(minor) {
            third = root + 3;
            fifth = root + 7;
        } else {
            third = root + 4;
            fifth = root + 7;
        }
        if (chord.toLowerCase().indexOf(1) != -1 && Character.isDigit(chord.toLowerCase().charAt(chord.length()-1))) {
            int i = (chord.charAt(chord.length() - 1)) - 48;
            if(i != 7){
                seventh = 0;
            }
            if (i != 9) {
                ninth = 0;
            }
        }
        if ( chord.toLowerCase().indexOf(1) == -1){
            seventh = 0;
            ninth = 0;
        }
    }//constructor

    public void addSeventh() {
        seventh = minor ? root + 10 : root + 11;
    }
    public void addNinth() {
        ninth = minor ? root + 14 : root + 14;
    }
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

    static String[] roman = new String[]{"i","ii","iii","iv","v","vi","vii", "n"};
    static int toval(String inp) {
        for (int i=0;i<roman.length;i++) {
            if (roman[i].equals(inp)) {
                return i;
            }
        }
        return 0;
    }
    public Connectors() {
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
    Path(int start) {
        //construct a path based on the starting Node index
        paths = new ArrayList<Integer>();
        paths.add(start);
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
    Path randomPathTo(int length,Path startpath,int endnode) {
        int lastnode = startpath.paths.get(startpath.paths.size()-1);
        if (length == 1) {
            if (lastnode == endnode) {return startpath;}
            return null;
        }
        ArrayList<Connectors> cArray = getConnected(lastnode);
        ArrayList<Path> pArray = new ArrayList<Path>();

        double rand = Math.random();
        for (Connectors connect:cArray) {
            Path nextpath = randomPathTo(length-1,startpath.thenTo(connect),endnode);
            if (nextpath != null) {
                pArray.add(nextpath);
            }
        }
        Path co = null;
        double totalWeight = 0;
        for(Path path : pArray) {
            totalWeight += path.weight;
        }
        for (Path path : pArray) {
            rand -= path.weight/totalWeight; //e.g. rand = .39, .39 - .4 <0, co = this one
            if (rand < 0) {
                co = path;
                break;
            }
        }
        return co;
    }
    void normalize() {

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

    static Graph Majorgraph() {
        Graph graph = new Graph();
        graph.updateNodes(new String[]{"C","D","E","F","G","A","B"});
                                    ////0,, 1   2   3   4   5   6
        graph.updateConnections(new String[][]{
                //root chords
                {"i" , "ii" , ".18"},
                {"i" , "iii" , ".16"},
                {"i" , "iv" , ".18"},
                {"i" , "v" , ".16"},
                {"i" , "vi" , ".16"},
                {"i" , "vii" , ".16"},


                //"dissonant" chords
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
        return graph;
    }
    static Graph Minorgraph() {
        Graph graph = new Graph();
        graph.updateNodes(new String[]{"c","d","e","f","g","a", "b"});

        graph.updateConnections(new String[][]{
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
        return graph;
    }
}// end graph

public class MusicPlayer {
    public void createNote(int note, int tick, Track track) throws InvalidMidiDataException {
        ShortMessage a = new ShortMessage();
        a.setMessage(144,1,note,100); //144 = on, 1 = keyboard, 44 = note, 100 = how loud and hard
        MidiEvent noteOn = new MidiEvent(a, tick); // start at tick 1
        track.add(noteOn);
        ShortMessage b = new ShortMessage();
        b.setMessage(128, 1, note, 100); //note off
        MidiEvent noteOff = new MidiEvent(b, tick+15); // stop at tick 16
        track.add(noteOff);
    }
    public void GUI(){
        JFrame frame = new JFrame();
        JButton button = new JButton("Click this");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(button);
        frame.setSize(400,400);
        frame.setVisible(true);
    }
    public void play() throws MidiUnavailableException, InvalidMidiDataException {
//            GUI();
        Sequencer player = MidiSystem.getSequencer();
        player.open();
        Sequence seq = new Sequence(Sequence.PPQ, 4);
        Track track = seq.createTrack();
        Scanner s = new Scanner(System.in);
        System.out.println("How long do you want the chord progression to be?");
        int progresLen = s.nextInt();
        s.nextLine();
        System.out.println("Major, or minor?");
        String type = s.nextLine().toLowerCase();
        Graph g = new Graph();
        Graph graph = new Graph();
        if(type.equals("minor")) {
            graph = g.Minorgraph();
        } else if(type.equals("major")) {
            graph = g.Majorgraph();
        }

        Integer[] major_scale = new Integer[]{1, 3, 5, 6, 8, 10, 12, 13};
        Integer[] minor_scale = new Integer[]{1, 3, 4, 6, 8, 9, 11, 13};
        Integer[] blues_scale = new Integer[]{1, 4, 6, 7, 8, 11, 13};

        Path path = new Path(0);
        ArrayList<Notes> notes = graph.toMusical(graph.randomPathTo(progresLen, path, 0));

        for(Notes not: notes){
            System.out.println(Arrays.asList(major_scale).indexOf(not.root) + 1);
        }
            int tick = 1;
            int octave = 52;
            //tick, octave, array list of Notes,
            for (Notes note : notes){
                createNote(note.root  + octave, tick, track);
                createNote(note.third + octave, tick, track);
                createNote(note.fifth + octave, tick, track);
                if(note.seventh != 0){
                    createNote(note.seventh + octave, tick, track);
                }
                if(note.ninth != 0){
                    createNote(note.ninth + octave, tick, track);
                }
                tick += 16;

            }
            player.setSequence(seq);
            player.setTempoInBPM(180);
            player.start();


    }
    public static Random rand = new Random();
    public static void main(String[] args) throws InvalidMidiDataException, MidiUnavailableException {
        Scanner s = new Scanner(System.in);
        MusicPlayer mp = new MusicPlayer();
        System.out.println("Welcome to the music generator.");
        boolean run = false;
        do{
            mp.play();
            System.out.println("Would you like to rerun the program?");
            run = s.nextLine().equals("yes");
        }while(run);
    }

    private static Integer[] blues(int length) {
        Integer[] chords = new Integer[]{1, 4, 1, 1,
                                         4, 4, 1, 1,
                                         2, 5, 1, 1};
        return chords;
    }

}