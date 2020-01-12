import java.io.*;
import java.util.*;
import java.nio.file.Files;




class FileSplit {

    /*public static void main(String[] args) throws IOException {
        splitFile("test.pdf");
            
        mergeFiles("output.pdf");
    }
*/
    public static int splitFile(String testFile) throws IOException {
        File f = new File(System.getProperty("user.dir")+File.separator+testFile);
        //File f = new File(System.getProperty("user.dir")+File.separator+testFile);
        double fileSize = f.length()/1024;
        StringBuffer sb = new StringBuffer();
        int partCounter = 0;//I like to name parts from 001, 002, 003, ...
                            //you can change it to 0 if you want 000, 001, ...

        double sizeOfFiles = 1024 * 100;// 100KB
        if(fileSize < 500){
            sizeOfFiles = 1024 * fileSize; 
            sizeOfFiles = sizeOfFiles/5;
        }
        int bufferSize = (int)sizeOfFiles;
        byte[] buffer = new byte[bufferSize];

        String fileName = f.getName();

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(f);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            File oneMoreDirectory = new File(f.getParent() + File.separator + "chunks");
            oneMoreDirectory.mkdir();
            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
				System.out.println("bytesAmount = "+bytesAmount);
                //write each chunk of data into separate file with different number in name
                File newFile = new File(oneMoreDirectory + File.separator + Integer.toString(++partCounter));
                newFile.createNewFile();
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
                sb.append(partCounter);
                sb.append(",");
            }
        }
        writeOutputToFile(sb.toString().substring(0, sb.length()-1));
        return partCounter;
    }

    
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
    Arrays.sort(files, (a,b)->Integer.parseInt(a.getName())-Integer.parseInt(b.getName()));//ensuring order 001, 002, ..., 010, ...
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