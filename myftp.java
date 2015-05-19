import java.util.Scanner;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates ftp client that can connects with an ftp server that it can send data
 * to, and receive data from
 */
public class myftp {
	// Socket that will act as connection between client and server
	protected Socket connector;
	private static String directoryConnector = null;
	protected ArrayList<Thread> threadList;
	protected HashMap<String, Thread> terminateID = new HashMap<String,Thread>();

	/**
	 * Initializes connector with address and nPort
	 *
	 * @param address
	 *            Server's ip address
	 * @param nPort
	 *            port number established by the server
	 */
	public myftp(String address, int nPort) throws Exception {
		connector = new Socket(address, nPort);
		threadList = new ArrayList<Thread>();
	}

	/**
	 * Uses the command line arguments to check whether a connection can be made
	 * to a server
	 *
	 * @param args
	 *            - ip address and port number
	 */
	public static void main(String[] args)
	{
		// Initializes IPAddress and nPort variables
		String IPAddress = "";
		int nPort = 0;
		int tPort = 0;
		
		System.out.println("OS: " + System.getProperty("os.name"));
		if (System.getProperty("os.name").indexOf("Windows") != -1)
			directoryConnector = "\\";			// Windows
		else
			directoryConnector = "/";

		// Checks if two command line args are given. If not the program ends
		if (args.length != 3)
		{
			System.out.println("Requires two args. IP Address, normal port number, and terminate port number. Exiting");
			System.exit(0);
		}

		// Checks if args given are of correct type. If not the program ends
		try
		{
			InetAddress.getByName(args[0]);
			nPort = Integer.parseInt(args[1]);
			tPort = Integer.parseInt(args[2]);
			IPAddress = args[0];
		}
		catch (Exception e)
		{
			System.out.println("Invalid arg values. Exitting");
			System.exit(0);
		}

		// Attempts to establish connection to server and take user input.
		try
		{
			myftp client = new myftp(IPAddress, nPort);
			System.out.println(client.connector.getPort());
			System.out.println(client.connector.getInetAddress());
			client.userIO(nPort, tPort, IPAddress, directoryConnector, client);
		}
		catch (Exception e)
		{
			System.out.println("Connection could not be made. Exiting");
			System.exit(0);
		}
	}

	/**
	 * Decides which action to take based on user input
	 * @param inputList Command typed in by user separated into two parts
	 */
	public void inputChecker(ArrayList inputList, int nPort, int tPort,
			String IPAddress, String directoryConnector, myftp client)
	{
		try
		{
			// Reads data received from the server
			BufferedReader inputScan = new BufferedReader(new InputStreamReader(connector.getInputStream()));

			// Outputs data to the server
			DataOutputStream dataOutputStream = new DataOutputStream(connector.getOutputStream());

			// Evaluates user input and decides what information to send to
			// server based on the data
			if (inputList.get(0).equals("ls"))
			{
				dataOutputStream.writeBytes("ls" + "\n");

				String list;
				// Loops through data received from the server and outputs the data
				while (!(list = inputScan.readLine()).equals(""))
				{
					System.out.println(list);
				}
			}
			else if (inputList.get(0).equals("get")) 
			{
				if (inputList.size() == 1) 
				{
					System.out.println("Improper get format: get fileName");
					return;
				}
				
				String getParam = inputList.get(1).toString();
				if (getParam.endsWith(" &")) 
				{
					System.out.println("Ends with &");
					// Spawns off new thread to perform Get
					(new Thread(new GetWorker(connector, nPort, inputList, IPAddress, client))).start();
					
					Thread.sleep(50);
					
					return;
				}
				
				dataOutputStream.writeBytes("get " + inputList.get(1) + "\n");
				
				long fileLength = Long.parseLong(inputScan.readLine());
				byte[] buffer = new byte[8196];
				
				FileOutputStream fileOutput = new FileOutputStream(new File((String) inputList.get(1)));
				DataInputStream inputStream = new DataInputStream(connector.getInputStream());
				
				for(long fileInput = 0, area; fileInput < fileLength; fileInput += area)
				{
					area = inputStream.read(buffer);
					fileOutput.write(buffer, 0, (int)area);
				}
				fileOutput.close();
			   	
			    System.out.println("File received");
			}
			else if (inputList.get(0).equals("put"))
			{
				if (inputList.size() == 1) 
				{
					System.out.println("Improper put format: put fileName");
					return;
				}
				String putParam = inputList.get(1).toString();
				if (putParam.endsWith(" &")) 
				{
					System.out.println("Ends with &");
					
					(new Thread(new PutWorker(connector, nPort, inputList, IPAddress, directoryConnector, client))).start();
					
					Thread.sleep(50);
					
					return;
				}
				
				Path dirPath = Paths.get(System.getProperty("user.dir"));
				String filePath = dirPath.toString() + directoryConnector + inputList.get(1);
				
				dataOutputStream.writeBytes("put " + inputList.get(1) + "\n");
					
				File file = new File(filePath);
				byte[] fileArray = new byte[8196];
				dataOutputStream.writeBytes(Long.toString(file.length()) + "\n");
					
					
				// Stops file input from leaking through
				Thread.sleep(500);
						
				BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
				for(int amount = inputStream.read(fileArray); amount > 0; amount = inputStream.read(fileArray))
				{
					dataOutputStream.write(fileArray, 0, amount);
				}
						
				inputStream.close();
				System.out.println("File Sent");
				
			}
			else if (inputList.get(0).equals("cd"))
			{
				// Sends data to the server
				dataOutputStream.writeBytes("cd " + inputList.get(1) + "\n");
			}
			else if (inputList.get(0).equals("pwd"))
			{
				// Sends data to the server
				dataOutputStream.writeBytes("pwd" + "\n");

				// Prints data received from the server
				System.out.println(inputScan.readLine());
			}
			else if (inputList.get(0).equals("mkdir"))
			{
				if (inputList.size() == 1)
				{
					System.out.println("Improper mkdir format: mkdir fileName");
					return;
				}
				// Sends data to the server
				dataOutputStream.writeBytes("mkdir " + inputList.get(1) + "\n");
				
				// Prints data received from the server
				String read;
				if (!(read = inputScan.readLine()).equals(""))
				{
					System.out.println(read);
				}
			}
			else if (inputList.get(0).equals("delete"))
			{
				// Sends delete command to server
				if (inputList.size() == 1)
				{
					System.out
							.println("Improper delete format: delete fileName");
					return;
				}
				dataOutputStream
						.writeBytes("delete " + inputList.get(1) + "\n");
				System.out.println(inputScan.readLine());
			}
			else if (inputList.get(0).equals("terminate"))
			{
				// Terminates thread if it exists
				if (inputList.size() != 2)
				{
					System.out.println("Improper terminate format: terminate terminateID");
					return;
				}
				if(client.terminateID.containsKey(inputList.get(1)))
				{
					DataOutputStream terminateDataStream;
					client.terminateID.get(inputList.get(1)).interrupt();
					System.out.println("Terminating Process");
					System.out.println("Terminate map size: " + client.terminateID.size());
					Socket terminateSocket = new Socket(IPAddress, tPort);
					terminateDataStream = new DataOutputStream(terminateSocket.getOutputStream());
					terminateDataStream.writeBytes(inputList.get(1) + "\n");
				}
				else
				{
					System.out.println("Terminate ID does not exist");
				}
				//System.out.println(client.threadList.size());
				//client.threadList.get(0).interrupt();
			}
			else if (inputList.get(0).equals("quit"))
			{
				// Sends data to the server
				// Breaks out of loop. Ends client
				dataOutputStream.writeBytes("quit" + "\n");
			}
			else
			{
				// Reaches if user sends invalid input
				System.out.println("Invalid command");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Disconnected from Server.");
		}
	}

	/**
	 * Takes user input and if input is valid, sends information to server and
	 * receives information back from server
	 */
	public void userIO(int nPort, int tPort, String IPAddress, String directoryConnector, myftp client)
	{
		try
		{
			// Takes user input from console
			Scanner userInputReader = new Scanner(System.in);

			String userInput = "";

			// Runs until user uses the quit command
			while (!userInput.equals("quit"))
			{
				// Prompts user
				System.out.print("mytftp>");

				// Reads input from user
				userInput = userInputReader.nextLine().trim();
				// If user input is => 2 words, separates command into 2
				// parts: the first word and then the rest of the line
				ArrayList<String> inputList = new ArrayList<String>(Arrays.asList(userInput.split(" ", 2)));

				System.out.println(inputList);

				// Filters users input and does an action based on the command
				inputChecker(inputList, nPort, tPort, IPAddress, directoryConnector, client);
			}
			// Closes input scanner
			userInputReader.close();
		}
		catch (Exception e)
		{
			System.out.println("Disconnected from Server");
		}
	}
}

/**
 * Performs concurrent get command
 *
 */
class GetWorker implements Runnable {
	// Socket that will act as connection between client and server
	private Socket connector;
	private BufferedReader inputScan;
	private DataOutputStream dataOutputStream;
	private ArrayList inputList;
	private myftp client;
	private String secondArg;
	private String terminate;
	
	
	public GetWorker(Socket connectorParam, int nPortParam, ArrayList inputListParam, String IPAddress, myftp clientParam)
	{
		try 
		{
			connector = new Socket(IPAddress, nPortParam);
		} 
		catch 
		(Exception e) 
		{
			System.out.println("Problem creating GetWorker socket");
		}
	
		inputList = inputListParam;
		client = clientParam;
		
		secondArg = inputList.get(1).toString();
		secondArg = secondArg.substring(0, secondArg.length() - 1).trim();
		// Prints data received from the server
		
					//client.threadList.add(Thread.currentThread());
					//System.out.println(client.threadList.size());
					// Reads data received from the server
					try {
						inputScan = new BufferedReader(new InputStreamReader(connector.getInputStream()));
						// Outputs data to the server
						dataOutputStream = new DataOutputStream(connector.getOutputStream());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
	}
	
	public void run() 
	{
		
		//System.out.println("In get worker");
		//System.out.println("1");
		//System.out.println(secondArg);
		
		try 
		{
			dataOutputStream.writeBytes("get " + inputList.get(1) + "\n");
			terminate = inputScan.readLine();
		} 
		catch (IOException e5) 
		{
			// TODO Auto-generated catch block
			e5.printStackTrace();
		}
		
		client.terminateID.put(terminate, Thread.currentThread());
		//System.out.println(client.terminateID.size());
		//System.out.println(client.terminateID.values());
		System.out.println("Terminate ID: " + terminate);
	
		//System.out.println("2");
		long fileLength = 0;
		try 
		{
			fileLength = Long.parseLong(inputScan.readLine());
		} 
		catch (NumberFormatException e4) 
		{
			// TODO Auto-generated catch block
			e4.printStackTrace();
		} 
		catch (IOException e4) 
		{
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}
		byte[] buffer = new byte[8196];
		DataInputStream inputStream = null;
		//System.out.println("3");
		FileOutputStream fileOutput = null;
		try 
		{
			fileOutput = new FileOutputStream(new File(secondArg));
			inputStream = new DataInputStream(connector.getInputStream());
		} 
		catch (FileNotFoundException e3) 
		{
			// TODO Auto-generated catch block
			e3.printStackTrace();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//System.out.println("4");
		//client.terminateID.put("100", Thread.currentThread());
		try
		{
			
			for(long fileInput = 0, area; fileInput < fileLength; fileInput += area)
			{
				Thread.sleep(1);
				area = inputStream.read(buffer);
				fileOutput.write(buffer, 0, (int)area);
			}
			
	   	
			System.out.println("File received");
			fileOutput.close();
			Thread.sleep(100);
			
			//dataOutputStream.writeBytes("quit" + "\n");
			client.terminateID.remove(terminate);
			//System.out.println(client.terminateID.size());
			//client.threadList.remove(Thread.currentThread());
			//System.out.println(client.threadList.size());
		}
		catch (InterruptedException consumed)
		{
			System.out.println("Get thread interrupted.");
			try {
				inputScan.close();
				inputScan = null;
				dataOutputStream.close();
				dataOutputStream = null;
				fileOutput.close();
				fileOutput = null;
				inputStream.close();
				inputStream = null;
			} catch (IOException e2) {
				System.out.println("Can't close streams.");
			}
			
			client.terminateID.remove(terminate);
			System.out.println(client.terminateID.size());
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				System.out.println("What");
			}
			//client.threadList.remove(Thread.currentThread());
			//System.out.println(client.threadList.size());
			Path dirPath = Paths.get(System.getProperty("user.dir"));
			Path deletePath = dirPath.resolve(secondArg);
			try {
				if(Files.deleteIfExists(deletePath))
				{
					System.out.println("File Deleted");
				}
				else
				{
					System.out.println("File does not exist");
				}
			} catch (IOException e) {System.out.println("Not deleting");}
			// Prompts user
			System.out.print("mytftp>");
			return;
		}
		catch(Exception e)
		{
			System.out.println("Error in get worker.");
			e.printStackTrace();
		}
		
		
	}
	
}

/**
 * Performs concurrent put command
 *
 */
class PutWorker implements Runnable 
{
	
	// Socket that will act as connection between client and server
		private Socket connector;
		private BufferedReader inputScan;
		private DataOutputStream dataOutputStream;
		private ArrayList inputList;
		private String directoryConnector;
		private String terminate;
		private myftp client;
		
		
		public PutWorker(Socket connectorParam, int nPortParam, ArrayList inputListParam, 
				String IPAddress, String directoryConnectorParam, myftp clientParam)
		{
			try 
			{
				connector = new Socket(IPAddress, nPortParam);
			} 
			catch 
			(Exception e) 
			{
				System.out.println("Problem creating Put Thread socket");
			}
		
			inputList = inputListParam;
			client = clientParam;
			directoryConnector = directoryConnectorParam;
			// Reads data received from the server
			try {
				inputScan = new BufferedReader(new InputStreamReader(connector.getInputStream()));
				// Outputs data to the server
				dataOutputStream = new DataOutputStream(connector.getOutputStream());
			} 
			catch (IOException e) 
			{
				
			}
			
		
		}
	
	public void run() 
	{
		BufferedInputStream inputStream = null;
		try
		{			
			String secondArg = inputList.get(1).toString();
			secondArg = secondArg.substring(0, secondArg.length() - 1).trim();
			Path dirPath = Paths.get(System.getProperty("user.dir"));
			String filePath = dirPath.toString() + directoryConnector + secondArg;
			
			dataOutputStream.writeBytes("put " + inputList.get(1) + "\n");
			terminate = inputScan.readLine();
			client.terminateID.put(terminate, Thread.currentThread());
			//System.out.println(client.terminateID.size());
			//System.out.println(client.terminateID.values());
			System.out.println("Terminate ID: " + terminate);
				
			File file = new File(filePath);
			byte[] fileArray = new byte[8196];
			dataOutputStream.writeBytes(Long.toString(file.length()) + "\n");
				
				
			// Stops file input from leaking through
			Thread.sleep(500);
					
			inputStream = new BufferedInputStream(new FileInputStream(file));
			for(int amount = inputStream.read(fileArray); amount > 0; amount = inputStream.read(fileArray))
			{
				Thread.sleep(10);
				dataOutputStream.write(fileArray, 0, amount);
			}
					
			inputStream.close();
			Thread.sleep(100);
			System.out.println("File Sent");
			
			client.terminateID.remove(terminate);
		}
		catch (InterruptedException e1) 
		{
			try 
			{
				dataOutputStream.close();
				//dataOutputStream = null;
				
				inputStream.close();
				//inputStream = null;
			} 
			catch (IOException e) {
				System.out.println("Promblem closing streams.");
			}
			// TODO Auto-generated catch block
			client.terminateID.remove(terminate);
			System.out.println("Put thread interrupted");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Promblem in put thread");
		}
	}
	
}

