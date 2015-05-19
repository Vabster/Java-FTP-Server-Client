import java.io.*;
import java.nio.*;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;


/**
 * Creates an ftp server that can connect to multiple
 * ftp clients
 */
public class myftpserver //implements Runnable
{
	// Sets directory path to the server's home directory
	private Path dirPath = Paths.get(System.getProperty("user.dir"));
	// connector is the socket that will be used to connect to clients
	private Socket connector;
	// buffered reader for scanning input.
	private BufferedReader inputScan = null;
	// diverts control if we're transferring a file.
	private boolean fileTransfer = false;
	private static String directoryConnector = null;
	// Map of all the threads and terminateIDs currently active
	protected static HashMap<String, Thread> terminateID;
	// Map of all the current files being put on to the server or received from the server
	protected static HashMap<String, String> getPutList;

	/**
	 * Constructor initializes the connector socket
	 * @param clientSocket port number received from command line arguments
	 */
	public myftpserver()
	{
		terminateID =  new HashMap<String,Thread>();
		getPutList = new HashMap<String,String>();
		System.out.println("Server Started");
	}

	/**
	 * Checks for proper command line args and creates
	 * a socket clients can connect to.
	 * @param args
	 */
	public static void main(String[] args)
	{
		
		// detect what OS we're on.  If we're in Windows, directoryConnector = "\\", else "/"
		System.out.println("OS: " + System.getProperty("os.name"));
		if (System.getProperty("os.name").indexOf("Windows") != -1)
			directoryConnector = "\\";			// Windows
		else
			directoryConnector = "/";
		
		// Initializes port numbers
		int nPort = 0;
		int tPort = 0;
		// Checks for the correct amount of command line args. Exits program otherwise
		if (args.length != 2)
		{
			System.out.println("Two arguements required: Normal Port Number and Terminate Port Number");
			System.exit(0);
		}

		// Checks that the port number is a valid integer. Exits program if this is not true
		try
		{
			nPort = Integer.parseInt(args[0]);
			tPort = Integer.parseInt(args[1]);
		}
		catch(Exception e)
		{
			System.out.println("Port number must be an integer. Exiting");
			System.exit(0);
		}

		// Creates a server socket and waits for a client to connect and respond
		try
		{
			// Initializes the server sockets
			ServerSocket normalSocket = new ServerSocket(nPort);;
			ServerSocket terminateSocket = new ServerSocket(tPort);
			
			
			System.out.println("Normal port: " + nPort);
			System.out.println("Terminate port: " + tPort);
			
			myftpserver server = new myftpserver();

			
			// Waits for a connection to be made and then accepts to normal and terminate port
			(new Thread(new NormalPortTransferThread(server, normalSocket))).start();
			(new Thread(new TerminatePortTransferThread(server, terminateSocket))).start();
		}
		catch(Exception e)
		{
			System.out.println("Server unable to start. Exitting program.");
		}
	}

	
}

/**
 * Transfers NormalThread class
 */
class NormalPortTransferThread implements Runnable 
{
	private ServerSocket nSocket;
	private myftpserver server;
	
	public NormalPortTransferThread(myftpserver serverParam, ServerSocket normalSocket) 
	{
		server = serverParam;
		nSocket = normalSocket;
	}
	
	public void run() 
	{
		System.out.println(Thread.currentThread().getName() + " NormalPortTransfer Entered");
		while (true) 
		{
			try 
			{
				(new Thread(new NormalThread(server, nSocket.accept()))).start();
			} 
			catch (Exception e) 
			{
				System.out.println(Thread.currentThread().getName() + " Error in NormalPortTransfer");
			}
		}
	}
}

/**
 * Connects to Terminate Thread in order to receive terminate commands from the client
 *
 */
class TerminatePortTransferThread implements Runnable 
{
	private ServerSocket tSocket;
	private myftpserver server;
	
	public TerminatePortTransferThread(myftpserver serverParam, ServerSocket tSocket) 
	{
		server = serverParam;
		this.tSocket = tSocket;
	}
	
	public void run() 
	{
		System.out.println(Thread.currentThread().getName() + " TerminatePortTransfer Entered");
		while (true) 
		{
			try 
			{
				(new Thread(new TerminateThread(server, tSocket.accept()))).start();
			} 
			catch (Exception e) 
			{
				System.out.println(Thread.currentThread().getName() + " Error in TerminatePortTransfer");
			}
		}
	}
}

/**
 * Terminates a thread if client sends currently used terminate ID
 *
 */
class TerminateThread implements Runnable 
{	
	private Socket terminateSocket;
	private myftpserver server;
	public TerminateThread(myftpserver serverParam, Socket terminateSocketParam) 
	{
		server = serverParam;
		terminateSocket = terminateSocketParam;
	}
	public void run() 
	{
		
		BufferedReader inputScan;
		try 
		{
			System.out.println(Thread.currentThread().getName() + " TerminateThread Entered");
			inputScan = new BufferedReader(new InputStreamReader(terminateSocket.getInputStream()));
			String id = inputScan.readLine();
			System.out.println("From terminate command" + id);
			if(server.terminateID.containsKey(id))
			{
				System.out.println("Terminating thread id " + id);
				server.terminateID.get(id).interrupt();
				server.terminateID.remove(id);
				System.out.println("Terminate Id map size: " + server.terminateID.size());
				System.out.println(server.terminateID.values());
			}
			else
			{
				System.out.println("Id does not exist");
			}
		} catch (IOException e) 
		{
			System.out.println("Problem in Terminate Thread");
			e.printStackTrace();
		}
		
			
	}
}

/**
 * Performs all commands other than terminate sent by the client
 *
 */
class NormalThread implements Runnable {
	/**
	 * Makes an action based on data received from client
	 * @param inputList - List of 1 or 2 strings
	 */
	
	// Sets directory path to the server's home directory
	private Path dirPath = Paths.get(System.getProperty("user.dir"));
	// connector is the socket that will be used to connect to clients
	// buffered reader for scanning input.
	private BufferedReader inputScan = null;
	// diverts control if we're transferring a file.
	private boolean fileTransfer = false;
	private static String directoryConnector = null;
	private Socket nSocket;
	private myftpserver server;
	
	public NormalThread(myftpserver serverParam, Socket nSocket)
	{
		if (System.getProperty("os.name").indexOf("Windows") != -1)
			directoryConnector = "\\";			// Windows
		else
			directoryConnector = "/";
		server = serverParam;
		this.nSocket = nSocket;
	}
	

	/**
	 * Waits for a response from client and makes appropriate action
	 * to the responses
	 */
	public void run()
	{
		System.out.println("NormalThread Entered");
		try
		{
			// Reads data from the client(s)
			inputScan = new BufferedReader( new InputStreamReader(nSocket.getInputStream()));

			// Enters loop and stays indefinitely
			while (true)
			{
				if (fileTransfer == false)
					try
					{
						// Continually waits for input from a client, exits from the loop until once input is received.
						while (!inputScan.ready())
						{
							Thread.sleep(8);
						}
	
						// Reads line from client
						
							String clientInput = inputScan.readLine();
							System.out.println("Input from client: " + clientInput);
							
							// If client input is => 2 words, separates command into 2
							//parts: the first word and then the rest of the line
							ArrayList<String> inputList = new ArrayList<String>(Arrays.asList(clientInput.split(" ", 2)));
		
							//Evaluates client input and makes action based on the input
							
								try
								{
									// Writes data to the client(s)
									DataOutputStream dataOutput = new DataOutputStream(nSocket.getOutputStream());

									//Evaluates client input and makes action based on the input
									if(inputList.get(0).equals("ls"))
									{
										// Sets path to server directory
										DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath);
										// Sends each file form directory to client
										for (Path index: directoryStream)
										{
											dataOutput.writeBytes(index.getFileName() + "\n");
										}
										
										// Writes a \n in order to maintain proper spacing in client
										dataOutput.writeBytes("\n");
									}
									else if(inputList.get(0).equals("get")) 
									{
										String secondArg = inputList.get(1).toString();
										// Checks if new thread should be created for the get command sent by the client
										if(secondArg.endsWith("&"))
										{
											System.out.println("Concurrent get command");
											secondArg = secondArg.substring(0, secondArg.length() - 1).trim();
											Random rand = new Random();
											int terminateID = rand.nextInt(10);
											dataOutput.writeBytes(terminateID + "\n");
											// Adds thread and terminateID to terminateID map
											server.terminateID.put(Integer.toString(terminateID), Thread.currentThread());
											
											System.out.println("Sending File: " + secondArg);
											
											File file = new File(dirPath.resolve(secondArg).toString());
											
											byte[] fileArray = new byte[8196];
											dataOutput.writeBytes(Long.toString(file.length()) + "\n");
												
											BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
											try
											{
												
												for(int amount = inputStream.read(fileArray); amount > 0; amount = inputStream.read(fileArray))
												{
													Thread.sleep(1);
													dataOutput.write(fileArray, 0, amount);
												}
														
												inputStream.close();
												Thread.sleep(100);
												System.out.println("File Sent");
												
											}
											catch (InterruptedException consumed)
											{
												// Occurs when client sends terminate ID associated with the get command
												inputStream.close();
												inputStream = null;
												System.out.println("Get method interrupted");
												return;
											}
											
										}
										else
										{
											System.out.println("Sending File: " + secondArg);
											// Checks if another user is putting same file name on to client, if so waits for that to complete
											if(server.getPutList.containsKey(secondArg) && server.getPutList.get(secondArg) == "put")
											{
												while(server.getPutList.containsKey(secondArg))
												{												
													System.out.println("Waiting to perform get " + secondArg);
													Thread.sleep(1000);
												}
											}
											
											server.getPutList.put(secondArg, "get");
											System.out.println("List " + server.getPutList.toString());
											
											File file = new File(dirPath.resolve(secondArg).toString());
											
											byte[] fileArray = new byte[8196];
											dataOutput.writeBytes(Long.toString(file.length()) + "\n");
												
											BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
											for(int amount = inputStream.read(fileArray); amount > 0; amount = inputStream.read(fileArray))
											{
												dataOutput.write(fileArray, 0, amount);
											}
													
											inputStream.close();
											System.out.println("File Sent");
											server.getPutList.remove(secondArg);
										}
										
									}
									else if (inputList.get(0).equals("put")) 
									{
										String secondArg = inputList.get(1).toString();
										// Checks if client wants concurrent put command
										if(secondArg.endsWith("&"))
										{
											System.out.println("Concurrent put command");
											secondArg = secondArg.substring(0, secondArg.length() - 1).trim();
											Random rand = new Random();
											int terminateID = rand.nextInt(10);
											// Sends terminateID to client
											dataOutput.writeBytes(terminateID + "\n");
											// Adds thread and terminateID to terminateID map
											server.terminateID.put(Integer.toString(terminateID), Thread.currentThread());
											
											long fileLength = Long.parseLong(inputScan.readLine());
											byte[] buffer = new byte[8196];
											
											Thread.sleep(500);
										
											FileOutputStream fileOutput = new FileOutputStream(new File(secondArg));
											DataInputStream inputStream = new DataInputStream(nSocket.getInputStream());
											try
											{
												for(long fileInput = 0, area; fileInput < fileLength; fileInput += area)
												{
													Thread.sleep(10);
													area = inputStream.read(buffer);
													fileOutput.write(buffer, 0, (int)area);
												}
												fileOutput.close();	
												inputStream.close();
									    		System.out.println("File received");
									    		Thread.sleep(100);
											}
											catch(InterruptedException consumed)
											{
												fileOutput.close();
												inputStream.close();
												
												System.out.println("Put Command interrupted");
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
											}
										}
										else
										{
											
											long fileLength = Long.parseLong(inputScan.readLine());
											byte[] buffer = new byte[8196];
											//if(server.getPutList.containsKey(secondArg) && server.getPutList.get(secondArg) == "put")
											//{
												while(server.getPutList.containsKey(secondArg))
												{												
													System.out.println("Waiting to perform put " + secondArg);
													Thread.sleep(1000);
												}
											//}
											
											server.getPutList.put(secondArg, "put");
											System.out.println("List " + server.getPutList.toString());
											
											FileOutputStream fileOutput = new FileOutputStream(new File((String) inputList.get(1)));
											DataInputStream inputStream = new DataInputStream(nSocket.getInputStream());
										
											for(long fileInput = 0, area; fileInput < fileLength; fileInput += area)
											{
												area = inputStream.read(buffer);
												fileOutput.write(buffer, 0, (int)area);
											}
											fileOutput.close();
									   	
									    	System.out.println("File received");
									    	
									    	server.getPutList.remove(secondArg);
									    	
										}
									}
									else if(inputList.get(0).equals("cd")) {
										// if the user wants to go up a directory, trim the last element from the current directory.
										if ( inputList.get(1).equals("..")) {
											String currDir = System.getProperty("user.dir");
											//System.out.println("Current Dir: " + currDir);
											int slashIndex = currDir.lastIndexOf(directoryConnector);
											String newDir = currDir.substring(0, slashIndex);
											//System.out.println("New Dir: " + newDir);
											System.setProperty("user.dir", newDir);
										} // if
										// otherwise, simply move down the directory structure.
										else
											System.setProperty("user.dir", System.getProperty("user.dir") + directoryConnector + inputList.get(1));

										// in either case, update dirPath appropriately, and return a response to the user.
										dirPath = Paths.get(System.getProperty("user.dir"));
										dataOutput.writeBytes("Directory changed to: " + dirPath + "\n");
										dataOutput.flush();
									}
									else if(inputList.get(0).equals("pwd"))
									{
										dataOutput.writeBytes(dirPath + "\n");
									}
									else if(inputList.get(0).equals("mkdir"))
									{
										// Creates directory
										try 
										{
											Files.createDirectory(dirPath.resolve(inputList.get(1)));
											dataOutput.writeBytes("\n");
										} 
										 catch(Exception e) 
										{
											dataOutput.writeBytes("Could not create file " + "\n");
										}
									}
									else if(inputList.get(0).equals("delete"))
									{
										// Deletes file if it exists
										Path deletePath = dirPath.resolve(inputList.get(1));
										if(Files.deleteIfExists(deletePath))
										{
											dataOutput.writeBytes("Successful deletion\n");
										}
										else
										{
											dataOutput.writeBytes("File does not exist\n");
										}
									}
									else if(inputList.get(0).equals("quit"))
									{
										nSocket.close();
									}
								}
								catch(Exception e)
								{}
							}
					
				catch (Exception e)
				{
					break;
				}
			}
		}
		catch(Exception e)
		{}
	}
