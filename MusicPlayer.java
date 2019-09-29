import com.sun.jdi.connect.Connector;
import org.w3c.dom.ls.LSOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import javax.crypto.spec.PSource;
import javax.sound.midi.*;
import javax.swing.*;

//TODO: rhythm generator
//TODO: chord inversions
//TODO: undirected graph nodes

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
        root = major_scale["cdefgab".indexOf(chord.toLowerCase().charAt(0))];
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

    static String[] roman = new String[]{"i","ii","iii","iv","v","vi","vii"};
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
    ArrayList<Nodes> nodes = new ArrayList<Nodes>(); // array [Nodes, Nodes, Nodes] >> ["C", "g", "a7"] >> "C".getNote() >> [1,4,7,,,] // contains the music theory in weights
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
        if (length == 0) {
            if (lastnode == endnode) {return startpath;}
            return null;
        }
        double rand = Math.random();
        ArrayList<Connectors> cArray = getConnected(lastnode);

        ArrayList<Path> pArray = new ArrayList<Path>();
        for (Connectors connect:cArray) {
            Path nextpath = randomPathTo(length-1,startpath.thenTo(connect),endnode);
            if (nextpath != null) {pArray.add(nextpath);}
        }
        Path co = null;
        for (Path path : pArray) {
            rand -= path.weight; //e.g. rand = .39, .39 - .4 <0, co = this one
            if (rand < 0) {
                co = path;
                break;
            }
        }
        return co;
    }
    void normalize() {

    }
//    ArrayList<4Notes> toMusical(Path p) {
//
//    }
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
                {"iv", "vii", "0.5"},

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
                {"iv", "v", "0.5"},
                {"iv", "vii", "0.3"},
                {"iv", "iii", "0.2"},

                //dominants
                {"v", "i", "1.0"},
                {"vii", "i", "1.0"}
        });
        return graph;
    }


}

public class MusicPlayer {
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
            System.out.println("Major, minor, or blues?");
            String type = s.nextLine().toLowerCase();
            Integer[] chords = new Integer[progresLen];

            //chord progressions




            switch (type){
                case "minor":
                    chords= major_minor(progresLen);
                    break;
                case "major":
                    chords = major_minor(progresLen);
                    break;
                case "blues":
                    chords = blues(progresLen);
                    break;
                default:
                    System.out.println("Error, no type selected");
            }
            int tick = 1;
            int octave = 52;
            Integer[] major_scale = new Integer[]{1, 3, 5, 6, 8, 10, 12, 13};
            Integer[] minor_scale = new Integer[]{1, 3, 4, 6, 8, 9, 11, 13};
            Integer[] blues_scale = new Integer[]{1, 4, 6, 7, 8, 11, 13};
            Integer[] scale = new Integer[chords.length];
            for(int i = 0; i < chords.length; i++){
                System.out.println(chords[i]);
                if(type.equals("major")){
                    scale[i] = major_scale[chords[i]-1];
                }
                else if (type.equals("minor")){
                    scale[i] = minor_scale[chords[i]-1];
                }
                else if (type.equals("blues")){
                    scale[i] = major_scale[chords[i]-1];
                }
            }
            for (int halfstep : scale){
//                System.out.println(halfstep);
                int root = halfstep + octave;
                int third = type.equals("major")? halfstep + octave + 4 : halfstep + octave + 3;
                int fifth = halfstep + octave + 7;
                int seventh = halfstep + octave + 10;
                //root
                ShortMessage a = new ShortMessage();
                a.setMessage(144,1,root,100); //144 = on, 1 = keyboard, 44 = note, 100 = how loud and hard
                MidiEvent noteOn = new MidiEvent(a, tick); // start at tick 1
                track.add(noteOn);
                ShortMessage b = new ShortMessage();
                b.setMessage(128, 1, root, 100); //note off
                MidiEvent noteOff = new MidiEvent(b, tick+15); // stop at tick 16
                track.add(noteOff);

                //third
                a = new ShortMessage();
                a.setMessage(144,1,third,100); //144 = on, 1 = keyboard, 44 = note, 100 = how loud and hard
                noteOn = new MidiEvent(a, tick); // start at tick 1
                track.add(noteOn);
                b = new ShortMessage();
                b.setMessage(128, 1, third, 100); //note off
                noteOff = new MidiEvent(b, tick+15); // stop at tick 16
                track.add(noteOff);

                //fifth
                a = new ShortMessage();
                a.setMessage(144,1,fifth,100); //144 = on, 1 = keyboard, 44 = note, 100 = how loud and hard
                noteOn = new MidiEvent(a, tick); // start at tick 1
                track.add(noteOn);
                b = new ShortMessage();
                b.setMessage(128, 1, fifth, 100); //note off
                noteOff = new MidiEvent(b, tick+15); // stop at tick 16
                track.add(noteOff);

                //seventh
                if(type.equals("blues")){
                    a = new ShortMessage();
                    a.setMessage(144,1,seventh,100); //144 = on, 1 = keyboard, 44 = note, 100 = how loud and hard
                    noteOn = new MidiEvent(a, tick); // start at tick 1
                    track.add(noteOn);
                    b = new ShortMessage();
                    b.setMessage(128, 1, seventh, 100); //note off
                    noteOff = new MidiEvent(b, tick+15); // stop at tick 16
                    track.add(noteOff);
                }

                tick +=16;
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

//    private static Integer[] minor(int length) {
////        Integer[] major_scale = new Integer[]{1, 3, 5, 6, 8, 10, 12, 13};
////        Integer[] blues_scale = new Integer[]{1, 4, 6, 7, 8, 11, 13};
//        Integer[] minor_scale = new Integer[]{1, 3, 4, 6, 8, 9, 11, 13};
//
//        Integer[] chords = new Integer[length];
//        for(int i = 0; i < length; i++){
//            if(i == 0 || i == length-1){
//                chords[i] = 1;
//            }else {
//                chords[i] = (minor_scale[rand.nextInt(6) + 1]);
//            }
//        }
//        return chords;
//    }
    private static Integer[] blues(int length) {
        Integer[] chords = new Integer[]{1, 4, 1, 1,
                                         4, 4, 1, 1,
                                         2, 5, 1, 1};
        return chords;


    }

    public static Integer[] major_minor(int length) {
        Integer[] chords = new Integer[length];
        for(int i = 0; i < length; i++){
            if(i == 0 || (( chords[i-1] !=7 || chords[i-1] !=5 )&& i == length-1)){
                chords[i] = 1;
            }else if(i == length-2){
                int num = rand.nextInt(3)+ 1;
                if (num ==1){
                    chords[i] = 5;
                }
                else if (num == 2){
                    chords[i] = 4;
                } else if(num == 3){
                    chords[i] = 7;
                }
            } else {
                switch(chords[i-1]){
                    case 8:
                    case 1:
                        switch(rand.nextInt(4)+1) {
                            case 1:
                                chords[i] = 3;
                                break;
                            case 2:
                                chords[i] = 6;
                                break;
                            case 3:
                                chords[i] = rand.nextInt(2)+1 == 1? 2 : 4;
                                break;
                            case 4:
                                chords[i] = rand.nextInt(2)+1 == 1? 5 : 7;
                                break;
                        }//end 1 chord switch
                        break;
                    case 4:
                    case 2:
                        switch(rand.nextInt(3)+1){
                            case 1:
                                chords[i] = rand.nextInt(2)+1 == 1? 2 : 4;
                                break;
                            case 2:
                                chords[i] = rand.nextInt(2)+1 == 1? 5 : 7;
                            case 3:
                                chords[i] = 1;
                        }
                        break;
                    case 3:
                        chords[i] = rand.nextInt(2)+1 == 1? 6 : (rand.nextInt(2)+1 ==1?  2 : 4);
                        break;
                    case 5:
                        chords[i] = 8;
                        break;
                    case 6:
                        chords[i] = rand.nextInt(2)+1 == 1? 2 : 4;
                        break;
                    case 7:
                        chords[i] = rand.nextInt(2)+1 == 1? 3 : 8;
                        break;
                    default:
                        chords[i] = 1;
                    }
            }
        }
        return chords;
    }
}