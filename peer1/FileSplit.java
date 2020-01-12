import java.io.*;
import java.util.*;
import java.nio.file.Files;




class FileSplit {

    public static void mergeFiles(List<File> files, File into) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(into);
            BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {
            for (File f : files) {
                Files.copy(f.toPath(), mergingStream);
            }
         }
    }

    public static List<File> listOfFilesToMerge(File oneOfFiles) {
   
    File[] files = oneOfFiles.getParentFile().listFiles();
    Arrays.sort(files, (a,b)->Integer.parseInt(a.getName())-Integer.parseInt(b.getName())); // ensuring order
    return Arrays.asList(files);
    }

    public static void mergeFiles(String into) throws IOException {
        File oneChunk = new File(System.getProperty("user.dir")+"\\chunks\\1");
        File outputFile = new File(System.getProperty("user.dir") + File.separator + into);
        outputFile.createNewFile();
        mergeFiles(listOfFilesToMerge(oneChunk), outputFile);
    }

    public static void writeOutputToFile(String output) {
        File fout = new File("summary.txt");
        FileWriter fr = null;
        try {
            fr = new FileWriter(fout, true);
            BufferedWriter bw = new BufferedWriter(fr);

            bw.write(output);
            bw.newLine();

            bw.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}