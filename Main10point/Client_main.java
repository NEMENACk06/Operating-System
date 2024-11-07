package Main10point;


import java.util.Scanner;
import java.io.*;
import java.net.*;


public class Client_main {
    public static void main(String[] args) {
        String serverDistination = "localhost"; // close firewall, connect to another device.
        String clientFilePath = "C:\\Users\\asus\\Desktop\\22";
        int port = 8080;
        try {
            Socket socket = new Socket(serverDistination, port);
            System.out.println("server connected");
            new Server_handler(socket, clientFilePath).start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}

class Server_handler extends Thread{
    private Socket socket;
    private String clientFilePath;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader reader;
    private Scanner scan;

    public Server_handler(Socket socket, String clientFilePath) throws Exception{
        this.socket = socket;
        this.clientFilePath =clientFilePath;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.scan = new Scanner(System.in);
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
                        System.out.println("client start to recieve");
                        String fileNameFromServer = reader.readLine();
                        System.out.println("download : "+ fileNameFromServer);
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

    public void receiveFile(String saveFileName) throws IOException{ // not work yet !!!!!!!!!!!!
        File file = new File(saveFileName);
        file.createNewFile();
        try(FileOutputStream fileOutputStream = new FileOutputStream(saveFileName)){
            

            byte[] buffer = new byte[4096];
            int bytesRead;

            // Read file data from the server
            int length = Integer.parseInt(reader.readLine().trim());
            System.out.println("len = " + length);

            int currentLength = 0;
            while (true) {
                if(currentLength == length){
                    break;
                }  
                bytesRead = inputStream.read(buffer);
                currentLength += bytesRead;
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("File received and saved at : " + saveFileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
