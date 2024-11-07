package Main10point;


import java.io.*;
import java.net.*;


public class Server_main {
    public static void main(String[] args) {

        final int port = 8080;
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
                new Client_handler(clientSocket, serverFolderPath).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

class Client_handler extends Thread{
    private Socket clientSocket;
    private String serverFolderPath;
    private File folder;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader reader;

    public Client_handler(Socket clientSocket, String serverFolderPath) throws IOException{
        this.clientSocket = clientSocket;
        this.serverFolderPath = serverFolderPath;
        folder = new File(serverFolderPath);
        this.inputStream = this.clientSocket.getInputStream();
        this.outputStream = this.clientSocket.getOutputStream();
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
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
                    sendMessageToClient("file name : \n");

                    String fileName = reader.readLine();
                    if(!isHaveFile(fileName)){
                        sendMessageToClient("file invalid\n");
                    }
                    else{
                        sendMessageToClient("server ready to send file\n" + fileName + "\n");
                        send(fileName);
                    }
                }
                else{
                    sendMessageToClient("wrong command\n");
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

    public void send(String filename) throws InterruptedException{ // not work yet!!!!!!!
        String filePath = serverFolderPath+"\\"+filename;

        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            File file = new File(filePath);
            int length = (int) file.length();
            sendMessageToClient(length + "\n");

            Thread.sleep(7);
            
            while (true) {
                bytesRead = fileInputStream.read(buffer);
                if(bytesRead == -1){
                    break;
                }
                outputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("File sent to client from " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}