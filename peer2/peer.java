import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.file.*;
import java.lang.Thread;

public class peer {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String endP;
	int port;
	int uport;
	int dport;
	int totalChunks;
	String testFileName;

	public peer(String ep, int port, int uport, int dport) {
		this.endP = ep;
		this.port = port;
		this.uport = uport;
		this.dport = dport;
	}

	void run()
	{
		try{
			//create a socket to connect to the server
			requestSocket = new Socket(endP, port);
			System.out.println("Connecting to " + endP + " on Port " + port);
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			//get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			ErrorCode err = ErrorCode.UnknownCommand;

			System.out.println("Connected to server");

			int i = 0;
			int initialChunkCount = 0;

			do
			{
				//Receive the response from the server
				Response response = null;
				
				try{
					response = (Response)in.readObject();
				}
				catch(EOFException eof){
					break;
				}
				
				totalChunks = response.totalChunks;
				initialChunkCount = response.initialChunks;
				testFileName = response.testFileName;

				if(response.initialChunks==-1){
					break;
				}
				
				File oneMoreDirectory = new File(System.getProperty("user.dir") + File.separator + "chunks");
            	oneMoreDirectory.mkdir();
				
				try{
					Thread.sleep(100);
				}
				catch(Exception e){
				}

				System.out.println("Received Chunk " + response.files);

				Files.write(Paths.get(System.getProperty("user.dir") + File.separator + "chunks" + File.separator + response.files), response.data);
				i++;
				
			}while(i <= initialChunkCount);
			
			try{
				Thread.sleep(1000);
			}
			catch(Exception e){
			}

			if(i==0){
				File oneMoreDirectory = new File(System.getProperty("user.dir") + File.separator + "chunks");
            	oneMoreDirectory.mkdir();
			}
			else{
				FileUtil.updateSummaryFile();
			}

			new uploadHandler(uport).start();
			new downloadHandler(dport, totalChunks, testFileName).start();
		}
		catch (EOFException eof){
			System.out.println("chunks received from server");
		}
		catch (ConnectException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
            		System.err.println("Class not found");
        	} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(Exception ioException){
				//ioException.printStackTrace();
			}
		}	
	}
	//send a message to the output stream
	void sendMessage(Request request)
	{
		try{
			//stream write the message
			out.writeObject(request);
			out.flush();
			System.out.println("Send Response: " + request.toString());
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	//main method
	public static void main(String args[])
	{

		if(args.length!=3){
			System.out.println("bad syntax");
			System.out.println("run : java peer serverPort uploadPort downloadPort");
		}
		else{
			int port = 10;
			int uport = 15;
			int dport = 20;
			clearOlderFiles();
			try{
				port = Integer.parseInt(args[0]);
				uport = Integer.parseInt(args[1]);
				dport = Integer.parseInt(args[2]);
			}
			catch(Exception e){
				e.printStackTrace();
				System.out.println("Bad port");
				return;
			}
			peer client = new peer("localhost", port, uport, dport);
			client.run();
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
    * A handler thread to listen from upload neighbour.  Handlers are spawned from the listening
    * loop and are responsible for dealing with a single client's requests.
    */
    private static class uploadHandler extends Thread {
        private String message;    //message received from the client
        private Socket uploadConnection = null;
        private ObjectInputStream uploadIn;   //stream read from the socket
        private ObjectOutputStream uploadOut;    //stream write to the socket

        // new code
        private static int uPort;   //The peer will be listening on this port number



        public uploadHandler(int uPort) {
            this.uPort = uPort;
        }

        public void run() {

        	System.out.println("inside uploadHandler thread");
            try{
               // System.out.println("Credentials matched. Client "+ no +" can talk to server");
               ServerSocket listener = new ServerSocket(uPort);  
               while(uploadConnection == null){
               	System.out.println("waiting for connection request on port no: " +uPort);
               	uploadConnection = listener.accept();
               }
               System.out.println("Peer connected!");

               // initialize input out stream for upload neighbour
                uploadOut = new ObjectOutputStream(uploadConnection.getOutputStream());
                uploadOut.flush();
                uploadIn = new ObjectInputStream(uploadConnection.getInputStream());

                while(true){
					try{
						Request request = (Request)uploadIn.readObject();
						System.out.println("Received request for file: "+ request.filename);
						Response response = new Response(request.filename);
						response.createResponse(request);
						
						//System.out.println(response.data.length);
						sendToUploadPeer(response);
					}
					catch(Exception e){
					}
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with upload peer");
            }
            finally{
                //Close connections
                try{
                    uploadIn.close();
                    uploadOut.close();
                    uploadConnection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with upload peer ");
                }
            }
    	}

        //send a message to upload neighbour output stream
        public void sendToUploadPeer(Response response)
        {
            try{
                uploadOut.writeObject(response);
                uploadOut.flush();
                System.out.println("Send response: " + response.toString() + " to upload peer");
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }

    /**
    * A handler thread to request chunks from download neighbour.  Handlers are spawned from the listening
    * loop and are responsible for dealing with a single client's requests.
    */
    private static class downloadHandler extends Thread {
        private String message;    //message sent to the client
        private Socket requestpeerSocket;
        private ObjectInputStream uploadIn;   //stream read from the socket
        private ObjectOutputStream uploadOut;    //stream write to the socket

        // new code
        private static int dPort;   //The peer will be listening on this port number
        private static boolean isConnected = false; // flag to check connection status
		private static String[] downloadSummary;
		private static int totalChunks;
		private static String testFileName;


        public downloadHandler(int dPort, int totalChunks, String testFileName) {
            this.dPort = dPort;
			this.totalChunks = totalChunks;
			this.testFileName = testFileName;
        }

        public void run() {

        	System.out.println("inside downloadHandler thread");
        	try{
        		while(!isConnected){
				
					System.out.println("Attempting to connect to download neighbour at port: " +dPort);
					try{
						//create a socket to connect to the server
						requestpeerSocket = new Socket("localhost", dPort);
						isConnected = true;
						System.out.println("Connected to download neighbour at port no: "+ dPort);
					}catch (ConnectException connectException) {
    					System.err.println("Connection refused. No active peer running on the provided port.");
    					try{
    						Thread.sleep(3000);
    					}
    					catch (InterruptedException interruptedException){
    					interruptedException.printStackTrace();
    					}
					}catch(UnknownHostException e){
						System.err.println("You are trying to connect to an unknown host!");
					}
				}

				//initialize download neighbour inputStream and outputStream
				uploadOut = new ObjectOutputStream(requestpeerSocket.getOutputStream());
				uploadOut.flush();
				uploadIn = new ObjectInputStream(requestpeerSocket.getInputStream());
				int temp = 0;
				while(true){
					try{
						if(temp == 0){
							message = ".." + File.separator + "summary.txt"; //getmissingChunk();
						}
						else{
							Thread.sleep(3);
							message = getNextChunk();
							if(message.equals("done")){
								FileSplit.mergeFiles(testFileName);
								FileUtil.updateSummaryFile();
								System.out.println("Stopping download thread");
								break;
							}
							else if(message.equals("summary.txt")){
								message = ".." + File.separator + "summary.txt";
							}
						}
						
						Request request = new Request(message);
						sendToDownloadPeer(request);

						Response response = (Response)uploadIn.readObject();
						
						if(response.files.charAt(0) == '.'){
							updateDownloadSummary(new String(response.data));
						}
						else{	
							Files.write(Paths.get(System.getProperty("user.dir") + File.separator + "chunks" + File.separator + response.files), response.data);
							FileUtil.updateSummaryFile();
						}
						temp++;
					}
					catch(Exception e){
					}
				}
        	}
        	catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

		//send a message to upload neighbour output stream
        public void sendToDownloadPeer(Request request)
        {
            try{
                uploadOut.writeObject(request);
                uploadOut.flush();
                System.out.println("Send request for: " + request.toString() + " to download peer");
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
		
		public String getNextChunk(){
			String myChunks = FileUtil.getSummaryFile();
			String[] chunks = myChunks.trim().split(" ");

			System.out.println("Downloaded Chunks");
			for(int i=0; i<chunks.length; i++){
				System.out.print(chunks[i] + " ");
			}
			System.out.println();
			
			if(chunks.length==totalChunks){
				return "done";
			}

			System.out.println("Chunks on download neighbor");
			for(int i=0; i<downloadSummary.length; i++){
				System.out.print(downloadSummary[i] + " ");
			}
			System.out.println();

			if(downloadSummary.length==0){
				return "summary.txt";
			}
			
			ArrayList<String> notDownloaded = new ArrayList<>();
			for(String s2: downloadSummary){
				boolean found = false;
				for(String s1: chunks)	
				{
					if(s1.equals(s2)){
						found = true;
						break;
					}
				}
				if(!found){
					notDownloaded.add(s2);
				}
			}

			System.out.println("Not Downloaded");
			for(int i=0; i<notDownloaded.size(); i++){
				System.out.print(notDownloaded.get(i) + " ");
			}
			System.out.println();
			
			if(notDownloaded.size()==0){
				return "summary.txt";
			}
			
			int key = (int)(Math.random() * notDownloaded.size());
			return notDownloaded.get(key);
		}
        
		public void updateDownloadSummary(String s){
			if(s.trim().length()==0){
				downloadSummary = new String[0];
			}
			else{
				downloadSummary = s.trim().split(" ");
			}
		}
    }
}


