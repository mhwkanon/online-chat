package chatRoom;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatServer {
	private Selector selector = null; //Selector to certain channel
	static final int port = 8888;
	private Charset charset = Charset.forName("UTF-8");
	private static Map<String, User> users = new HashMap<>(); // users set, temp store users
	private static Map<String, ChatRoom> rooms = new HashMap<>();  //room set, temp store rooms
	////if user exist, post this info to clients, note that this info must excely same with client info
	private static String USER_EXIST = "Sorry, name Taken.\nLogin Name?"; 
	//use this note to split content and username 
	private static String USER_CONTENT_SPILIT = "#@#";
	//input or get serverIp
	private String serverIp;

	public void init() throws IOException {
		selector = Selector.open();
		ServerSocketChannel server = ServerSocketChannel.open();// open a new server
		// connect to TCP socket
		server.bind(new InetSocketAddress(port));
		server.configureBlocking(false);// NIO
		// register
		server.register(selector, SelectionKey.OP_ACCEPT);
		serverIp = InetAddress.getLocalHost().toString();
		System.out.println("start Listenning.....");
		while (true) {  //keep running to check every channel state
			int readyChannels = selector.select();
			if (readyChannels == 0)
				continue;
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
			while (keyIterator.hasNext()) {
				SelectionKey sk = (SelectionKey) keyIterator.next();
				keyIterator.remove();
				dealWIthSelectionKey(server, sk);
			}
		}
	}
	// if current channel has new state, deal it, read or write 
	public void dealWIthSelectionKey(ServerSocketChannel server, SelectionKey sk)
			throws IOException {
		if (sk.isAcceptable()) {
			SocketChannel sc = server.accept();
			sc.configureBlocking(false);
			// regist SC
			sc.register(selector, SelectionKey.OP_READ);
			// this channel use to accept from clients
			sk.interestOps(SelectionKey.OP_ACCEPT);
			sc.write(charset.encode("connected to " + serverIp + "\n"));
			// XXX: DS
//			sc.write(String.format("connected to %s\n", serverIp));
			System.out.println(sc.getRemoteAddress() + "connect to Server"
					+ serverIp + "\n");
			sc.write(charset.encode("Escape Character is '^'\n"));
			sc.write(charset.encode("Welcome to mhw chat server\n"));
			sc.write(charset.encode("Login Name?"));
		}
		if (sk.isReadable()) {
			// if it has data to read
			SocketChannel sc = (SocketChannel) sk.channel();
			
			// XXX: DS
			// Check out try-with-resource
//			try (SocketChannel sc = (SocketChannel) sk.channel()) {
//				
//			}
			
			ByteBuffer buff = ByteBuffer.allocate(1024);
			StringBuilder content = new StringBuilder();
			try {
				while (sc.read(buff) > 0) {
					buff.flip();
					content.append(charset.decode(buff));
				}
				System.out.println("Server is listenning from client"
						+ sc.getRemoteAddress() + "data rev is" + content);
				sk.interestOps(SelectionKey.OP_READ);
			} catch (IOException io) {
				sk.cancel();
				if (sk.channel() != null) {
					sk.channel().close();
				}
			}
			if (content.length() > 0) {
				dealContent(content.toString(), sc);
			}
		}

	}

	// count current number of client
	public static int onlineNum(Selector selector) {
		int res = 0;
		
		// XXX: DS
		// Check out Lambda
//		selector.keys().stream().forEach(key -> {
//			Channel targetChannel = key.channel();
//			...
//		});
		
		for (SelectionKey key : selector.keys()) {
			Channel targetchanel = key.channel();
			if (targetchanel instanceof SocketChannel) {
				res++;
			}
		}
		return res;
	}

	// broadcast to clients in certain chatRoom
	public void BroadCast(Selector selector, ChatRoom chatRoom, String content)
			throws IOException {
		if (chatRoom != null) {
			Set<SocketChannel> scs = chatRoom.getSCS();
			for (SelectionKey key : selector.keys()) {
				Channel targetchannel = key.channel();
				if (targetchannel != null
						&& targetchannel instanceof SocketChannel
						&& scs.contains(targetchannel)) {
					SocketChannel dest = (SocketChannel) targetchannel;
					dest.write(charset.encode(content));
				}
			}
		} else {
			for (SelectionKey key : selector.keys()) {
				Channel targetchannel = key.channel();
				if (targetchannel != null
						&& targetchannel instanceof SocketChannel) {
					SocketChannel dest = (SocketChannel) targetchannel;
					dest.write(charset.encode(content));
				}
			}
		}

	}

	// deal with user content, broad message or deal instruction
	private void dealContent(String content, SocketChannel sc)
			throws IOException {
		String[] arrayContent = content.toString().split(USER_CONTENT_SPILIT);
		if (arrayContent != null && arrayContent.length == 1) { // no user yet,
																// create new
																// user
			String name = arrayContent[0];
			if (users.containsKey(name)) { // user exist, ask for change a name
				sc.write(charset.encode(USER_EXIST));
			} else { // else create new user
				User user = new User(name, sc);
				users.put(name, user);
				int num = onlineNum(selector);
				String message = "welcome " + name
						+ " to chat room! Online Number is " + num;
				BroadCast(selector, null, message);
			}
		} else if (arrayContent != null && arrayContent.length > 1) { // user is
																		// not
																		// null,
																		// deal
																		// the
																		// command
			String name = arrayContent[0];
			User user = users.get(name);
			if (user == null || user.getSC() != sc) {
				sc.write(charset.encode("sorry,user not exist."));
			} else {
				String message = content.substring(name.length() // get clients
																	// input
						+ USER_CONTENT_SPILIT.length());
				if (message.trim().charAt(0) == '/') { // this is a command
					dealMessage(user, message);
				} else if (user.getChatRoom() == null) { // not in the room
					sc.write(charset
							.encode("sorry,you need join to a chat room."));
				} else { // broadcast clients chat to other clients
					message = name + ": " + message;
					BroadCast(selector, user.getChatRoom(), message);
				}
			}

		}
	}

	// get all the user's name in the chatroom
	private void getMemberInRoom(ChatRoom cr, SocketChannel sc)
			throws IOException {
		List<User> users = cr.getUsers();
		for (User u : users) {
			sc.write(charset.encode(u.getUserName() + "\n"));
		}
	}

	/*
	 * deal the command 
	 * if /rooms get all chat rooms currently open 
	 * if /leave leave current room 
	 * if /join (roomname) join to a chat room 
	 * if /create room (roomname) create a new chat room
	 */
	private void dealMessage(User user, String message) throws IOException {
		message = message.trim();
		SocketChannel sc = user.getSC();
		message = message.substring(1, message.length());
		if (message.equals("rooms")) {
			sc.write(charset.encode("Active room are: \n"));
			for (ChatRoom cr : rooms.values()) {
				sc.write(charset.encode(cr.getRoomName() + " ("
						+ cr.getMembers() + ")"));
			}
			sc.write(charset.encode("\nend of list"));
		} else if (message.equals("leave")) {
			ChatRoom cr = user.getChatRoom();
			cr.deleteUser(user);
			user.setChatRoom(null);
			sc.write(charset.encode("leave room : " + cr.getRoomName()));
			BroadCast(selector, user.getChatRoom(), "user has left chat: "
					+ user.getUserName());
		}

		else {
			String[] instrs = message.split(" ");
			if (instrs.length > 1 && instrs[0].equals("join")) {
				if (rooms.containsKey(instrs[1])) {
					ChatRoom cr = rooms.get(instrs[1]);
					if (cr.getUsers().contains(user)) {
						sc.write(charset.encode("you already in this room"
								+ cr.getRoomName() + "\n"));
					} else if (user.getChatRoom() != null) {
						sc.write(charset.encode("you already in a room \n"));
					} else {
						cr.addUser(user);
						user.setChatRoom(cr);
						sc.write(charset.encode("enterring room: "
								+ cr.getRoomName() + "\n"));
						sc.write(charset.encode("welcome to "
								+ cr.getRoomName() + "\n"));
						getMemberInRoom(cr, sc);
						BroadCast(selector, user.getChatRoom(),
								"new user joined chat: " + user.getUserName());
					}
				} else {
					sc.write(charset
							.encode("room not exist! please create room using this format: /create room ****\n"));
				}
			} else if (instrs.length > 2 && instrs[0].equals("create")
					&& instrs[1].equals("room")) {
				ChatRoom cr = new ChatRoom(instrs[2], user.getUserName());
				rooms.put(instrs[2], cr);
				cr.addUser(user);
				user.setChatRoom(cr);
				sc.write(charset.encode("enterring room: " + cr.getRoomName()
						+ "\n"));
				sc.write(charset.encode("welcome to " + cr.getRoomName() + "\n"));
				BroadCast(selector, user.getChatRoom(),
						"new user joined chat: " + user.getUserName());
			} else {
				sc.write(charset.encode("invalid instruction \n"));
			}
		}
	}

	public static void main(String[] args) throws IOException {
		new ChatServer().init();
	}
}
