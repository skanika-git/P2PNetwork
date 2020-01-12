import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.file.*;

enum ErrorCode{
    OK,
    UnknownCommand,
    CommandMalformed,
    ReadFailed,
    WriteFailed,
    BadUsername,
    BadPassword,
    FileNotFound
}

class Request implements Serializable{
    //String command;
    String filename;
    byte[] data;

    public Request(String filename){
    
        this.filename = filename;
        this.data = new byte[0];
    }

    public void processGet(String[] arr){
        try{
            this.filename = arr[1];
        }
        catch(Exception e){
        }
    }

    public void processUpload(String[] arr){
        try{
            this.filename = arr[1];
        }
        catch(Exception e){
        }

        if(!this.filename.isEmpty()){
            try{
                this.data = Files.readAllBytes(Paths.get(this.filename));
            }
            catch(NoSuchFileException exception){
                // if file did not exist, send an empty filename
                // so that server detects a malformed command
                this.data = new byte[0];
                this.filename = "";
                System.out.println("File does not exist");
            }
            catch(Exception exception){
                this.data = new byte[0];
                this.filename = "";
                System.out.println("File read failed");
            }
        }
    }


    @Override
    public String toString()
    {
        return " filename = " + this.filename;
    }
}


class Response implements Serializable{
    private static final long serialVersionUID = 6529685098267757690L;
    ErrorCode code;
    byte[] data;
    String files;
    int totalChunks;
	int initialChunks;
	String testFileName;

    public Response(String files, int totalChunks, String testFileName, int initialChunks){
        this.totalChunks = totalChunks;
        this.files = files;
        this.data = new byte[0];
		this.testFileName = testFileName;
		this.initialChunks = initialChunks;
        this.code = ErrorCode.OK;
    }
	
	public Response(String files){
		this.files = files;
		this.data = new byte[0];
        this.code = ErrorCode.OK;
	}

    void createResponse(Request request){
        readFile(request.filename);       
    }
    

    void readFile(String filename)
    {
        if(filename.isEmpty()){
            this.code = ErrorCode.CommandMalformed;
            this.data = new byte[0];
        }
        else{
            try{
                this.data = Files.readAllBytes(Paths.get(System.getProperty("user.dir") + File.separator + "chunks" + File.separator + filename));
                this.code = ErrorCode.OK;
            }
            catch(NoSuchFileException exception){
                this.data = new byte[0];
                this.code = ErrorCode.FileNotFound;
                System.out.println("File does not exist "+ filename);
            }
            catch(Exception exception){
                this.data = new byte[0];
                this.code = ErrorCode.ReadFailed;
                System.out.println("Read failed at server");
            }
        }
    }

    void uploadFile(Request request)
    {
        String filename = request.filename;
        if(filename.isEmpty()){
            this.code = ErrorCode.CommandMalformed;
            this.data = new byte[0];
        }
        else{
            try{
                Files.write(Paths.get(request.filename), request.data);
                this.code = ErrorCode.OK;
                this.data = new byte[0];
            }
            catch(Exception exception){
                this.data = new byte[0];
                this.code = ErrorCode.WriteFailed;
                System.out.println("Write failed at server");
            }
        }
    }

    @Override
    public String toString()
    {
        return "code = " + this.code.toString() + " sending file: " + this.files + " dataLen = " + this.data.length; 
    }
}
