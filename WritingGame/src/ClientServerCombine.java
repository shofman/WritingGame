import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


public class ClientServerCombine {
	ServerSocket serverSocket;	
	Socket connection = null;
	ObjectOutputStream serverOut;
	ObjectInputStream serverIn;
	String serverMessage;
	static boolean hasReceivedMessage;
	
	Socket clientSocket;
	ObjectOutputStream clientOut;
	ObjectInputStream clientIn;
	String clientMessage;

	private static void setupGUI() {
		 try {
				UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException
					| IllegalAccessException | UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
         UIManager.put("swing.boldMetal", Boolean.FALSE);
	}
	
	private interface AddComponentsToWindow {
		void addComponentsToPane();
		void setupCloseOperation();
	}
	
	abstract class GenericWindow extends JFrame implements AddComponentsToWindow {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		void createWindow() {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
		}
		private void createAndShowGUI() {
			this.setupCloseOperation();
			this.addComponentsToPane();
			
			this.pack();
			this.setVisible(true);
		}
	}
	
	public class RunWindow extends GenericWindow {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public RunWindow() {
			super.createWindow();
		}
		
		public void addComponentsToPane() {
			JButton hostButton = new JButton("Setup Host Writer");
			hostButton.addActionListener(getHostButtonListener());
			
			JButton clientButton = new JButton("Setup Client Writer");
			clientButton.addActionListener(getClientButtonListener());
			
			getContentPane().add(hostButton, BorderLayout.PAGE_START);
			getContentPane().add(clientButton, BorderLayout.PAGE_END);
		}
				
		public ActionListener getHostButtonListener() {
			return new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(new HostRunner()).start();
				}
			};
		}
		
		public ActionListener getClientButtonListener() {
			return new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					new ClientWriterWindow();
				}
			};
		}

		@Override
		public void setupCloseOperation() {
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}

	}
	
	public class HostWriterWindow extends GenericWindow {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean isConnecting = false;
		private JButton hostButton;
		public HostWriterWindow() {
			super.createWindow();
		}
		
		public void addComponentsToPane() {
			hostButton = new JButton("Connect");
			hostButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (isConnecting) {
						new Thread(new ServerRunner()).start();
						hostButton.setText("Cancel connection");
					} else {
						hostButton.setText("Connect");
					}
					isConnecting = !isConnecting;
				}
			});
			
			getContentPane().add(hostButton, BorderLayout.PAGE_START);
		}

		@Override
		public void setupCloseOperation() {
			this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);			
		}
	}
	
	public class ClientWriterWindow extends GenericWindow {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		ClientWriterWindow() {
			super.createWindow();			
		}
		@Override
		public void addComponentsToPane() {
			JButton hostButton = new JButton("Setup Client Writer");
			hostButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
			
			getContentPane().add(hostButton, BorderLayout.PAGE_START);
		}

		@Override
		public void setupCloseOperation() {
			this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);			
		}
		
	}
	
	void runClient() {
		try {
			clientSocket = new Socket("localhost", 2004);
			System.out.println("Connected to localhost in port 2004");
			clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
			clientOut.flush();
			clientIn = new ObjectInputStream(clientSocket.getInputStream());
			
			do {
				try {
					clientMessage = (String) clientIn.readObject();
					System.out.println("server> " + clientMessage);
					sendClientMessage("Hi my server");
					clientMessage = "bye";
					sendClientMessage(clientMessage);
				} catch (ClassNotFoundException classError) {
					System.err.println("Data received in unknown format");
				}
			} while(!clientMessage.equals("bye"));
		} catch (UnknownHostException e) {
			System.err.println("You are trying to connect to an unknown host");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				clientIn.close();
				clientOut.close();
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	void sendClientMessage(String message) {
		try {
			clientOut.writeObject(message);
			clientOut.flush();
			System.out.println("client> " + message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void runServer() {
		try {
			//TODO: Try reducing this number to 1
			serverSocket = new ServerSocket(2004, 10);
			System.out.println("Waiting for connection");
			connection = serverSocket.accept();
			System.out.println("Connection received from " + connection.getInetAddress().getHostName());
			serverOut = new ObjectOutputStream(connection.getOutputStream());
			serverOut.flush();
			serverIn = new ObjectInputStream(connection.getInputStream());
			sendServerMessage("Connection successful");
			
			do {
				try {
					serverMessage = (String) serverIn.readObject();
					System.out.println("client>" + serverMessage);
					if (serverMessage.equals("bye")) {
						hasReceivedMessage = true;
						sendServerMessage("bye");
					}
				} catch(ClassNotFoundException classException) {
					System.err.println("Data received in unknown format");
				}
			} while(!serverMessage.equals("bye"));
			
		} catch(IOException e){
			e.printStackTrace();
		} finally {
			try {
				serverIn.close();
				serverOut.close();
				serverSocket.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	void sendServerMessage(String message) {
		try { 
			serverOut.writeObject(message);
			serverOut.flush();
			if (message.equals("bye")) {
				System.out.println("Send bye");
			}
			System.out.println("server> " + message);
		} catch(IOException ioException) {
			ioException.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		setupGUI();
		ClientServerCombine csc = new ClientServerCombine();
		csc.new RunWindow();
	}
	
	public class HostRunner implements Runnable {
		public void run() {
			new HostWriterWindow();
		}
	}
	
	public class ServerRunner implements Runnable {
		Server server;
		@Override
		public void run() {
			server = new Server();
			while(!hasReceivedMessage) {
				server.run();
			}
		}
		
		public Server getServer() {
			return server;
		}
	}	
	
}
