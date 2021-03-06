import java.net.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.Files;
import java.util.*;

public class Client5 {
	Socket requestSocket; // socket connect to the server
	ObjectOutputStream out; // stream write to the socket
	ObjectInputStream in; // stream read from the socket
	String Sub_FileName; // capitalized message read from the server
	int[] ports = new int[6];
	static int count_files;
	String folderdest;
	public static int files_recieved = 0;
	public static boolean[] check_list;
	public static int file_downloaded = 0;
	public static int file_uploaded = 0;
	public static boolean any_new = true;

	Client5() {
	}

	void mergeFiles(String Filename, int count) throws IOException {
		int i = 0;
		File into = new File(Filename);

		try (BufferedOutputStream mergingStream = new BufferedOutputStream(
				new FileOutputStream(into))) {
			while (i < count) {
				File f = new File(Filename + "." + String.format("%d", i));
				Files.copy(f.toPath(), mergingStream);
				i++;
			}
		}
	}

	void DeleteFiles(String Filename, int count) throws IOException {
		for (int i = 0; i < count; i++) {
			File myFile = new File(Filename + "." + String.format("%d", i));
			myFile.delete();

		}
	}

	void DownloadFromServer(String folder, int port) {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		;

		try {
			// create a socket to connect to the server
			requestSocket = new Socket("localhost", port);
			System.out.println("Connected to localhost in port " + port);
			// initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(
					requestSocket.getInputStream());
			Sub_FileName = (String) in.readObject();
			Sub_FileName = folder + "//" + Sub_FileName;
			count_files = in.readInt();
			int end_index = in.readInt() - 1;
			int start_index = in.readInt() - 1;
			System.out
					.println("Creating Chunk List & Updating it to received chunks from server");

			check_list = new boolean[count_files];
			int i = start_index;
			while (i < end_index + 1) {
				byte[] mybytearray = new byte[102400];
				System.out.println("Receiving  chunk: " + i + " from server");
				fos = new FileOutputStream(Sub_FileName + "."
						+ String.format("%d", i));
				bos = new BufferedOutputStream(fos);
				int bytesRead = in.read(mybytearray, 0, mybytearray.length);
				int current = bytesRead;

				do {
					bytesRead = in.read(mybytearray, current,
							(mybytearray.length - current));
					if (bytesRead >= 0)
						current += bytesRead;
				} while (bytesRead > 0);
				bos.write(mybytearray, 0, current);
				bos.flush();
				fos.close();
				check_list[i] = true;

				file_downloaded++;

				i++;
			}
			try {
				ports = (int[]) in.readObject();
				// out.writeUTF("done");
				Thread.sleep(2000);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

		} catch (ConnectException e) {
			System.err
					.println("Connection refused. You need to initiate a server first.");
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");

		}

		catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			// Close connections
			try {
				if (fos != null)
					fos.close();
				if (bos != null)
					bos.close();
				if (requestSocket != null)
					requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}

		}

	}

	// send a message to the output stream
	public static void main(String[] args) {
		int sPort = Integer.parseInt(args[1]);

		Client5 client = new Client5();
		String folder = args[0];
		client.DownloadFromServer(folder, sPort);
		System.out.println("Waiting for Chunks from Peers");
		File Filename = new File(client.Sub_FileName);
		DownloadPeer thrd = new DownloadPeer(client.ports[4],
				Client5.count_files, Filename);
		thrd.start();
		UploadPeer thrd1 = new UploadPeer(client.ports[5],
				Client5.count_files, Filename);
		try {
			thrd.join();
			thrd1.join();
		} catch (InterruptedException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			System.out.println("All Chunks Received ! Starting to merge");
			client.mergeFiles(Filename.toString(), Client5.count_files);
			client.DeleteFiles(Filename.toString(), Client5.count_files);
			System.out.println("File Recieved Completely");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}
}

class UploadPeer extends Thread {
	Socket csocket;
	int start_subfile;
	int end_subfile;
	File filename;
	int total_files;

	int port;
	boolean anynew = true;

	UploadPeer(int port, int total, File file) {

		this.port = port;
		this.start_subfile = -1;
		this.end_subfile = -1;
		this.filename = file;
		this.total_files = total;

		start();

	}

	ObjectOutputStream out;
	ObjectInputStream in;

	public void run() {
		ServerSocket sSocket = null;
		int a = 0;

		Socket csocket = null;
		while (Math.ceil(Client5.count_files / 5) + Client5.file_uploaded != Client5.count_files)

		{
			try {
				a = Client5.file_downloaded;
				sSocket = new ServerSocket(port, 10);
				csocket = sSocket.accept();
				out = new ObjectOutputStream(csocket.getOutputStream());
				out.flush();
				int i = 0;
				int change = 0;
				in = new ObjectInputStream(csocket.getInputStream());
				boolean[] peerchecklist = (boolean[]) in.readObject();
				System.out.println("Receiving Chunk List from Peer");
				while (i < peerchecklist.length) {
					if (peerchecklist[i] != Client5.check_list[i]
							&& !peerchecklist[i])
						change++;
					i++;
				}

				if (change == 0)

				{
					if (csocket != null)
						csocket.close();
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				i = 0;

				System.out.println("Chunks not in Peer are");
				while (i < peerchecklist.length)

				{
					if (peerchecklist[i] != Client5.check_list[i]
							&& !peerchecklist[i]) {
						System.out.print(" " + i);
						if (start_subfile == -1)
							start_subfile = i;
						end_subfile = i;
					}
					i++;
				}
				System.out.println();
				out.writeInt(total_files);
				out.writeInt(end_subfile);
				out.writeInt(start_subfile);
				out.flush();
				while (start_subfile <= end_subfile) {
					File myFile = new File(filename.getParent(),
							filename.getName() + "."
									+ String.format("%d", start_subfile));
					byte[] mybytearray = new byte[(int) myFile.length()];
					FileInputStream fis = new FileInputStream(myFile);
					BufferedInputStream bis = new BufferedInputStream(fis);
					int tmp = bis.read(mybytearray, 0, mybytearray.length);
					out.writeInt((int) myFile.length());
					System.out.println("Sending chunk" + start_subfile + "("
							+ mybytearray.length + " bytes)");
					out.write(mybytearray, 0, tmp);
					out.flush();

					System.out.println("Done.");
					start_subfile++;
					Client5.file_uploaded++;

					fis.close();
					bis.close();
				}
				change = 0;
				start_subfile = -1;

			} catch (IOException ioException) {
				// ioException.printStackTrace();

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block

				// e.printStackTrace();
			}

			try {

				Thread.sleep(1000);
				if (sSocket != null)
					sSocket.close();
				if (csocket != null)
					csocket.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block

				e.printStackTrace();

			}
			do {
				try {

					Thread.sleep(5000);
					a = Client5.file_downloaded;
				} catch (InterruptedException e) {

					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (a == Client5.file_uploaded);

		}
	}
}

class DownloadPeer extends Thread {

	int start_subfile;
	int end_subfile;
	File filename;
	int ports;
	int total_files;

	boolean anynew = true;

	DownloadPeer(int dest_port, int total, File filename) {
		this.start_subfile = -1;
		this.end_subfile = -1;
		this.filename = filename;
		this.ports = dest_port;
		this.total_files = total;

	}

	ObjectOutputStream out;
	ObjectInputStream in;

	public void run() {
		Socket requestSocket = null;
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;

		while (Client5.file_downloaded != Client5.count_files)

		{
			try {
				while (true)

				{
					if (Client5.file_downloaded == Client5.count_files)
						return;

					try {
						requestSocket = new Socket("localhost", ports);
						break;
					}

					catch (ConnectException e) {
						System.err
								.println("Waiting to Download Files from Peer");
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();

						}
					}
				}

				System.out.println("Connected to localhost in port " + ports);
				// initialize inputStream and outputStream
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				out.flush();
				if (!requestSocket.isClosed()) {
					if (in != null)
						in.close();

					in = new ObjectInputStream(requestSocket.getInputStream());
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// continue;
				}
				System.out.println("Sending Chunk List to Peer");

				out.writeObject(Client5.check_list);
				int count_files = in.readInt();
				int end_index = in.readInt();
				int start_index = in.readInt();

				// check_list= new boolean[count_files];
				int i = start_index;
				while (i < end_index + 1) {
					byte[] mybytearray = new byte[102400];
					fos = new FileOutputStream(filename + "."
							+ String.format("%d", i));
					bos = new BufferedOutputStream(fos);
					int count = in.readInt();
					int bytesRead = in.read(mybytearray, 0, mybytearray.length);
					int current = bytesRead;

					do {
						bytesRead = in.read(mybytearray, current,
								(mybytearray.length - current));
						if (bytesRead >= 0)
							current += bytesRead;
					} while (bytesRead > 0 & current < count);
					bos.write(mybytearray, 0, current);
					bos.flush();
					fos.close();
					System.out.println(" chunk " + i + " downloaded ("
							+ current + " bytes read)");
					Client5.check_list[i] = true;
					Client5.file_downloaded++;

					i++;
					out.flush();
				}
				if (in != null)
					in.close();
				if (out != null)
					out.close();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (ConnectException e) {
				System.err
						.println("Connection refused2. You need to initiate a server first.");
			} catch (UnknownHostException unknownHost) {
				System.err
						.println("You are trying to connect to an unknown host!");
			} catch (IOException ioException) {

				// ioException.printStackTrace();

			} finally {
				// Close connections

				try {
					if (fos != null)
						fos.close();
					if (bos != null)
						bos.close();
					if (requestSocket != null)
						requestSocket.close();

				} catch (IOException ioException) {
					ioException.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
	}
}
