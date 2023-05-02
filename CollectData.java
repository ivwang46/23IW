import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CollectData {
    public static void main(String[] args) {
        String filename = args[0];
        SilberMcCoy sb = new SilberMcCoy(filename);
        BufferedWriter buffer;
        try {
            FileWriter file = new FileWriter("filename" + "_out.txt");
            buffer = new BufferedWriter(file);

            buffer.write(sb.getConstructionTime() + " milliseconds for construction");



            buffer.write("\nStrong Chains: ");
            buffer.write(sb.strongToString());
        } catch (IOException e){
            System.out.println("ERR: " + e);
        }
    }
}
