package chatRoom;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class User {
	private String userName;
	private List<User> contact = new ArrayList<>();
	private ChatRoom chatRoom = null;
	private SocketChannel sc = null;

	public User(String userName, SocketChannel sc) {
		this.userName = userName;
		this.sc = sc;
	}

	public SocketChannel getSC() {
		return this.sc;
	}

	public String getUserName() {
		return this.userName;
	}

	public ChatRoom getChatRoom() {
		return this.chatRoom;
	}

	public void setChatRoom(ChatRoom room) {
		this.chatRoom = room;
	}

}
