import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.file.*;

public class fileowner {

    // set of file names
    public static Set<String> files = new HashSet<String>(); 
    public static Set<Integer> goodClients = new HashSet<Integer>();
    public static Set<Integer> badClients = new HashSet<Integer>();

    private static int sPort;   //The server will be listening on this port number
    private static String username = "tim";
    private static String password = "123";
    private static String testFileName;
    private static Integer chunkid = 1;
    private static Integer dummyLock = 1;

    
    public static void main(String[] args) throws Exception {

        if(args.length!=1){
            System.out.println("bad syntax");
            System.out.println("run : java ftpserver <port>");
            return;
        }

        try{
            sPort = Integer.parseInt(args[0]);
        }
        catch(Exception e){
            System.out.println("Bad port");
            return;
        }

        clearOlderFiles();
        BufferedReader x= new BufferedReader(new InputStreamReader(System.in));

        int noOfChunks = Integer.MIN_VALUE;
        while(noOfChunks == Integer.MIN_VALUE){
            System.out.println("Enter test file name");
            testFileName = x.readLine();
            noOfChunks = FileSplit.splitFile(testFileName);
            if(noOfChunks == Integer.MIN_VALUE){
                System.out.println("File not found");
            }
        }
        System.out.println("Total chunks created: " + noOfChunks);

        System.out.println("Server successfully set up");

        
        ServerSocket listener = new ServerSocket(sPort);

        System.out.println("The server is running."); 
        int clientNum = 1;
        try {
            while(true) {
                new Handler(listener.accept(), clientNum, noOfChunks).start();
                System.out.println("Client "  + clientNum + " is attempting to connect!");
                clientNum++;
            }
        } finally {
            listener.close();
        }
    }

    private static void clearOlderFiles(){
        File chunkDirectory = new File(System.getProperty("user.dir") + File.separator + "chunks");
        if(chunkDirectory.exists()){
            File[] chunkList = chunkDirectory.listFiles();
            if(chunkList.length > 0){
                for(File f : chunkList){
                    f.delete();
                }
            }
            chunkDirectory.delete();
        }

        File summaryFile = new File(System.getProperty("user.dir") + File.separator + "summary.txt");
        if(summaryFile.exists()){
            summaryFile.delete();
        }
    }

    /**
    * A handler thread class.  Handlers are spawned from the listening
    * loop and are responsible for dealing with a single client's requests.
    */
    private static class Handler extends Thread {
        private String message;    //message received from the client
        private Socket connection;
        private ObjectInputStream in;   //stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;     //The index number of the client
        private int totalChunks;
        public Handler(Socket connection, int no, int totalChunks) {
            this.connection = connection;
            this.no = no;
            this.totalChunks = totalChunks;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                int chunkNo = (int)Math.ceil(totalChunks*1.0/5.0);
                int count = 0;

                    System.out.println("Client "+ no +" can talk to server");
					if(no==4){
						chunkNo = Math.min(chunkNo, totalChunks - chunkNo*3);
					}
                    if(no == 5)//&& totalChunks%5!=0)
                    {chunkNo = totalChunks - chunkNo*4;}
                    if(chunkNo<=0){
                        chunkNo = 0;
                        Response response = new Response("-1", totalChunks, testFileName, -1);
                        sendMessage(response);
                    }
                    while(count<chunkNo)
                    {
                        synchronized(dummyLock)
                        {
                            Request request = new Request(String.valueOf(chunkid));
                            Response response = new Response(String.valueOf(chunkid), totalChunks, testFileName, chunkNo);
                            response.createResponse(request);
                            
							if(response.code!=ErrorCode.OK){
								continue;
							}
                            // send response

                            chunkid++;
                            count++;

                            sendMessage(response);
                        }
                    }
                        
                }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
    }

        //send a message to the output stream
        public void sendMessage(Response response)
        {
            try{
                out.writeObject(response);
                out.flush();
                System.out.println("Send Response: " + response.toString() + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }

}
