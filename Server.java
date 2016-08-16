
import java.net.*;
import java.io.*;
import java.nio.file.Files;

public class server {

	public static void main(String args[]) {
		int[] port = new int[6];
		for (int i = 1; i < args.length; i++)
			port[i - 1] = Integer.parseInt(args[i]);
		ServerSocket sSocket = null;
		try {
			sSocket = new ServerSocket(port[0], 10);
			FileSplit a = new FileSplit();
			File Filename = new File(args[0]);
			a.splitFile(Filename);
			int clients = 1;
			int chunkcount = (int) Math.ceil(a.count_files / 5);
			MultiThreadServer1[] thrd = new MultiThreadServer1[6];
			while (true && clients < 6) {
				System.out.println("Waiting for Connection");
				if (clients != 5)
					thrd[clients] = new MultiThreadServer1(sSocket.accept(),
							port, (clients - 1) * chunkcount + 1, (clients)
									* chunkcount, a.count_files, Filename);
				else
					thrd[clients] = new MultiThreadServer1(sSocket.accept(),
							port, (clients - 1) * chunkcount + 1,
							a.count_files, a.count_files, Filename);
				thrd[clients].start();
				clients++;
			}
			clients = 1;
			while (clients < 6) {
				thrd[clients].join();
				clients++;
			}
			for (int i = 0; i < a.count_files + 1; i++) {
				File myFile = new File(Filename + "." + String.format("%d", i));
				myFile.delete();
			}

			System.out.println("All threads are dead, exiting main thread");
		} catch (IOException ioException) {
			System.err
					.println("Unable to access port as it blocked by another process");
			System.exit(1);
		} catch (InterruptedException e) {
			System.err
					.println("Unable to access port as it blocked by another process");
			System.exit(1);
		} finally {
			try {
				sSocket.close();
				System.out.println("closing");
			} catch (IOException ioException) {
				System.err
						.println("Unable to access port as it blocked by another process");
				System.exit(1);
			}
		}
	}
}

class MultiThreadServer1 extends Thread {
	Socket csocket;
	int start_subfile;
	int end_subfile;
	File filename;
	int[] ports = new int[5];
	int total_files;

	MultiThreadServer1(Socket csocket, int dest_port[], int start, int end,
			int total, File file) {
		this.csocket = csocket;
		this.start_subfile = start;
		this.end_subfile = end;
		this.filename = file;
		this.ports = dest_port;
		this.total_files = total;
	}

	ObjectOutputStream out;
	ObjectInputStream in;

	public void run() {
		try {
			out = new ObjectOutputStream(csocket.getOutputStream());
			out.writeObject(filename.getName());
			// send files
			out.flush();
			in = new ObjectInputStream(csocket.getInputStream());
			out.writeInt(total_files);
			out.writeInt(end_subfile);
			out.writeInt(start_subfile);
			out.flush();
			while (start_subfile <= end_subfile) {
				File myFile = new File(filename.getParent(), filename.getName()
						+ "." + String.format("%d", start_subfile));
				byte[] mybytearray = new byte[102400];
				FileInputStream fis = new FileInputStream(myFile);
				BufferedInputStream bis = new BufferedInputStream(fis);
				int tmp = bis.read(mybytearray);
				System.out.println("Sending chunk" + start_subfile + "(" + tmp
						+ " bytes)");
				out.write(mybytearray, 0, tmp);
				System.out.println("Done.");
				start_subfile++;
				out.flush();
				fis.close();
				bis.close();
			}
			out.writeObject((Object) ports);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}
}

class FileSplit {
	int count_files = 0;

	public void splitFile(File f) throws IOException {
		int subfile_count = 1;
		int subfile_size = 10 * 10240;// 100KB
		byte[] buffer = new byte[subfile_size];
		System.out.println("Total File size " + f.length() + " Bytes");
		float a = ((float) f.length() / (long) subfile_size);
		count_files = (int) Math.ceil(a);
		try (BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(f))) {
			String name = f.getName();
			int tmp = 0;
			while ((tmp = bis.read(buffer)) > 0) {
				File newFile = new File(f.getParent(), name + "."
						+ String.format("%d", subfile_count++));
				try (FileOutputStream out = new FileOutputStream(newFile)) {
					out.write(buffer, 0, tmp);// tmp is chunk size
				}
			}
		}
		System.out.println("Count of total chunks " + count_files);
	}
}