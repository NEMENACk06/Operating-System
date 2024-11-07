package Main15point;


import java.io.*;
import java.net.*;


public class Server_main {
    public static void main(String[] args) {

        final int port = 20000;
        String serverFolderPath = "C:\\Users\\asus\\Desktop\\11";

        System.out.println("Server is Starting. . . . .");
        try (ServerSocket serverSocket = new ServerSocket(port);) {
            System.out.println("Waiting for port :" + port);
            while(true){
                if(serverSocket.isClosed()){
                    break;
                }
                Socket clientSocket = serverSocket.accept();
                System.out.println("new Client is Connected :" + clientSocket);
                new Client_handler(clientSocket, serverFolderPath, port).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

class Client_handler extends Thread{
    private Socket clientSocket;
    private String serverFolderPath;
    private int port;
    private File folder;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader reader;

    public Client_handler(Socket clientSocket, String serverFolderPath, int port) throws IOException{
        this.clientSocket = clientSocket;
        this.serverFolderPath = serverFolderPath;
        folder = new File(serverFolderPath);
        this.inputStream = this.clientSocket.getInputStream();
        this.outputStream = this.clientSocket.getOutputStream();
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.port = port;
    }

    @Override
    public void run(){
        try{
            sendMessageToClient("saf - See all files\nlf - load file\nexit - disconnect\n");
            while (!clientSocket.isClosed()) {
                String clientOption = reader.readLine();
                System.out.println("server recieve : " + clientOption);
                if(clientOption == null){
                    clientSocket.close();
                    break;
                }
                else if(clientOption.equalsIgnoreCase("saf")){
                    sendMessageToClient(getFilesName());
                }
                else if(clientOption.equalsIgnoreCase("lf")){
                    sendMessageToClient("please enter your file name : \n");

                    String fileName = reader.readLine();
                    if(!isHaveFile(fileName)){
                        sendMessageToClient("file invalid.\n");
                    }
                    else{
                        sendMessageToClient("server ready to send file\n" + fileName + "\n");
                        send(fileName);
                    }
                }
                else{
                    sendMessageToClient("wrong command.\n");
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageToClient(String message) throws IOException{
        outputStream.write(message.getBytes());
        outputStream.flush();
    }

    public String getFilesName(){
        File[] allFiles = folder.listFiles();
        if(allFiles == null || allFiles.length == 0){
            return "Folder is empty.\n";
        }
        String allFilesName = "---File list---\n";
        for(int i=0;i<allFiles.length;i++){
            double fileSize = allFiles[i].length();
            String prefix = "B";
            if(fileSize > 1000000){
                prefix = "MB";
                fileSize = fileSize / (1024 * 1024);
            }
            else if(fileSize > 1000){
                prefix = "KB";
                fileSize = fileSize / 1024;
            }
            allFilesName += allFiles[i].getName() + "  [" + String.format("%.1f", fileSize) + " " + prefix + "]\n";
        }
        allFilesName += "---------------\n";
        return allFilesName;
    }

    public boolean isHaveFile(String filename){
        File[] allFiles = folder.listFiles();
        for(int i=0;i<allFiles.length;i++){
            if(filename.equals(allFiles[i].getName())){
                return true;
            }
        }
        return false;
    }

    public void send(String filename) throws IOException{
        String filePath = serverFolderPath+"\\"+filename;

        int threadCount = 10;
        File file = new File(filePath);

        sendMessageToClient(file.length() + "\n");
        int chunkSize = (int)file.length() / threadCount;
        TransferThreads[] transferThreads = new TransferThreads[threadCount];

        for(int i=0;i<threadCount;i++){
            int start = i * chunkSize;
            int end;
            if(i == threadCount -1){ // last thread
                end = (int)file.length();
            }
            else{
                end = start + chunkSize;
            }
            transferThreads[i] = new TransferThreads(port + i + 1, start, end, filePath);
            transferThreads[i].start();
        }
        sendMessageToClient("all thread is ready to connect.\n");
        while(true){
            int threadClosed = 0;
            for(int i=0;i<threadCount;i++){
                if(!transferThreads[i].isAlive()){
                    threadClosed ++;
                }
            }
            if(threadClosed == threadCount){
                break;
            }
        }
        System.out.println("\n File sent to client from " + filePath);
    }
}

class TransferThreads extends Thread{
    private int port, start, end;
    private String filePath;
    private ServerSocket serverSocket;
    private Socket socket;
    private OutputStream outputStream;
    public TransferThreads(int port,int start, int end, String filePath){
        this.port = port;
        this.start = start;
        this.end = end;
        this.filePath = filePath;
    }

    @Override
    public void run(){
        try (FileInputStream fileInputStream = new FileInputStream(filePath)){
            serverSocket = new ServerSocket(port);
            socket = serverSocket.accept();
            System.out.println("Server: Thread in port " + this.port  + " is accepted.");
            outputStream = socket.getOutputStream();

            fileInputStream.skip(start);

            byte[] buffer = new byte[4096];
            int totalRead = end - start;
            int byteRead;
            while (true) {
                if(totalRead > buffer.length){
                    byteRead = fileInputStream.read(buffer, 0, buffer.length);
                }
                else{
                    byteRead = fileInputStream.read(buffer, 0, totalRead);
                }
                if(totalRead == 0){
                    break;
                }
                outputStream.write(buffer);
                outputStream.flush();
                totalRead -= byteRead;
            }
            System.out.println("Server: thread in port " + this.port + " transfer complete.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            try{
                socket.close();
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}