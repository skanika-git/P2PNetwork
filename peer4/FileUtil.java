import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;

public class FileUtil {

    public static final File fout = new File("summary.txt");

    public static void updateSummaryFile() {
        synchronized(fout){
            File folder = new File(System.getProperty("user.dir") + File.separator + "chunks");
            File[] files = folder.listFiles();

            StringBuilder sb = new StringBuilder();

            for(File f: files){
                sb.append(f.getName());
                sb.append(" ");
            }
            writeOutputToFile(sb.toString().trim());
        }
    }

    public static void writeOutputToFile(String output) {
        if(fout.exists()){
            fout.delete();
        }
        FileWriter fr = null;
        try {
            fout.createNewFile();
            fr = new FileWriter(fout, true);
            BufferedWriter bw = new BufferedWriter(fr);

            bw.write(output);
            bw.newLine();

            bw.close();
            fr.close();
        } catch (IOException e) {
            System.out.println("summary file write error");
        }
        
    }
	
	public static String getSummaryFile(){
		String out = "";
        synchronized(fout){
            try 
            {
                BufferedReader br = new BufferedReader(new FileReader(fout));
    			out = br.readLine();
                br.close();
            } catch (IOException e) {
    			System.out.println("summary file read error");
            }
        }
		return out;
	}
}
