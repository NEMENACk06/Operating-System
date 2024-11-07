package Main15point;


import java.util.Scanner;
import java.io.*;
import java.net.*;


public class Client_main {
    public static void main(String[] args) {
        String serverDistination = "172.27.107.115"; // connect to another device.
        String clientFilePath = "C:\\Users\\asus\\Desktop\\22";
        int port = 20000;
        try {
            Socket socket = new Socket(serverDistination, port);
            System.out.println("server is connected.");
            new Server_handler(socket, clientFilePath, port, serverDistination).start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}

class Server_handler extends Thread{
    private Socket socket;
    private String clientFilePath;
    private String serverDistination;
    private int port;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader reader;
    private Scanner scan;

    public Server_handler(Socket socket, String clientFilePath,int port, String serverDistination) throws Exception{
        this.socket = socket;
        this.clientFilePath =clientFilePath;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.scan = new Scanner(System.in);
        this.port = port;
        this.serverDistination = serverDistination;
    }

    public void run(){
        String recieveMassage;
        try{
            while (true) {
                System.out.println(reader.readLine());
                if(!reader.ready()){
                    break;
                }
            }
            while (true) {
                String message = scan.nextLine() + "\n";
                if(message.trim().equalsIgnoreCase("exit")){
                    socket.close();
                    break;
                }
                
                sendMessageTOServer(message);
        
                while (true) {
                    recieveMassage = reader.readLine();
                    System.out.println(recieveMassage);
                    
                    if(recieveMassage.trim().equals("server ready to send file")){
                        String fileNameFromServer = reader.readLine();
                        System.out.println("client start to download: " + fileNameFromServer);
                        receiveFile(clientFilePath + "\\" + fileNameFromServer);
                    }
                    
                    if(!reader.ready()){
                        break;
                    }
                }
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessageTOServer(String message) throws IOException{
        outputStream.write(message.getBytes());
        outputStream.flush();
    }

    public void receiveFile(String saveFileName) throws IOException{
        File file = new File(saveFileName);
        file.createNewFile();

        int length = Integer.parseInt(reader.readLine().trim());
        
        int threadCount = 10;
        int chunkSize = length / threadCount;
        RecieveThread[] recieveThreads = new RecieveThread[threadCount];
        for(int i=0;i<threadCount;i++){
            int start = i * chunkSize;
            int end;
            if(i == threadCount - 1){
                end = length;
            }
            else{
                end = start + chunkSize;
            }
            recieveThreads[i] = new RecieveThread(port + i + 1, start, end, saveFileName, serverDistination);
        }
        System.out.println("all thread to recieve is created.");
        String dummy = reader.readLine();
        System.out.println(dummy);
        for(int i=0;i<threadCount;i++){
            recieveThreads[i].start();
        }
        while(true){
            int threadClosed = 0;
            for(int i=0;i<threadCount;i++){
                if(!recieveThreads[i].isAlive()){
                    threadClosed ++;
                }
            }
            if(threadClosed == threadCount){
                break;
            }
        }
        System.out.println("\nFile received and saved at : " + saveFileName);
    }
}

class RecieveThread extends Thread{
    private int port, start, end;
    private String filePath, serverDistination;
    private Socket socket;
    
    public RecieveThread(int port, int start, int end, String filePath, String serverDistination) throws IOException{
        this.port = port;
        this.start = start;
        this.end = end;
        this.filePath = filePath;
        this.serverDistination = serverDistination;
    }

    public void run(){
        try(RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw")){
            socket = new Socket(this.serverDistination, this.port);
            System.out.println("Client: Thread in port " + this.port + " is connected to server.");
            InputStream inputStream = socket.getInputStream();
            
            randomAccessFile.seek(start);

            byte[] buffer = new byte[4096];
            int totalRead = end - start;
            int bytesRead;
            while(true){
                if(totalRead > buffer.length){
                    bytesRead = inputStream.read(buffer, 0, buffer.length);
                }
                else{
                    bytesRead = inputStream.read(buffer, 0, totalRead);
                }
                if(totalRead == 0){
                    break;
                }
                totalRead -= bytesRead;
                randomAccessFile.write(buffer, 0, bytesRead);
            }
            System.out.println("Client: Thread in port " + this.port + " recieve complete.");
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}