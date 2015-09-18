package chatRoom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
	private Selector selector = null;
	private String serverIp;
	static final int port = 8888;
	private boolean flag = false;
	private Charset charset = Charset.forName("UTF-8");
	private SocketChannel sc = null;
	private User user = null;
	private static String USER_EXIST = "Sorry, name Taken.\nLogin Name?";
	private static String USER_CONTENT_SPILIT = "#@#";

	public void init() throws IOException {

		while (true) {
			System.out
					.println("please provide server IP and port using this format:  *.*.*.*");
			Scanner scan = new Scanner(System.in);
			String address = scan.nextLine();
			String[] adds = address.split(":");
			if (adds.length > 0 && adds[0].length() > 0) {
				serverIp = adds[0];
				serverIp = serverIp.trim();
			}
			if (isValidIp(serverIp))
				break;
			else
				System.out.println("invalid IP!");
		}
		selector = Selector.open();

		// String ip = scan.nextLine();
		sc = SocketChannel.open(new InetSocketAddress(serverIp, port));
		System.out.println("Trying " + serverIp + ".....");
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ);
		// read from server
		new Thread(new ClinetThread()).start();
		Scanner scan = new Scanner(System.in);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if ("".equals(line))
				continue;
			if (user == null || !flag) {
				User u = new User(line, sc);
				user = u;
				flag = true;
				line = user.getUserName() + USER_CONTENT_SPILIT;
			} else {
				line = user.getUserName() + USER_CONTENT_SPILIT + line;
			}
			sc.write(charset.encode(line));// write to server
		}
	}
	// open a new thread to read from server
	private class ClinetThread implements Runnable {
		public void run() {
			try {
				while (true) {
					int readyChannels = selector.select();
					if (readyChannels == 0)
						continue;
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> keyIterator = selectedKeys
							.iterator();
					while (keyIterator.hasNext()) {
						SelectionKey sk = (SelectionKey) keyIterator.next();
						keyIterator.remove();
						dealWWithSelectionKey(sk);
					}
				}
			} catch (Exception io) {
				io.printStackTrace();
			}
		}
	}

	private void dealWWithSelectionKey(SelectionKey sk) throws IOException {
		if (sk.isReadable()) {
			// use  NIO read Channel just regist 1 SocketChannel
			// sc read from server
			SocketChannel sc = (SocketChannel) sk.channel();
			ByteBuffer buff = ByteBuffer.allocate(1024);
			String content = "";
			while (sc.read(buff) > 0) {
				buff.flip();
				content += charset.decode(buff);
			}
			if (USER_EXIST.equals(content)) {
				user = null;
				flag = false;
			}
			System.out.println(content);
			sk.interestOps(SelectionKey.OP_READ);
		}
	}
	//IP validation
	private boolean isValidIp(String ip) {
		
		Pattern pattern = Pattern
				.compile("\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");
		Matcher matcher = pattern.matcher(ip);
		return matcher.matches();
	}

	public static void main(String[] args) throws IOException {
		new Client().init();
	}
}
