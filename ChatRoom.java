package chatRoom;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ChatRoom {
	private static int MAX_MEMBER = 100;
	private List<User> users = new ArrayList<>();
	private String Admin = "";
	private boolean isActive = true;
	private String roomName = "";
	private Set<SocketChannel> scs = new HashSet<>();

	public ChatRoom(String roomName, String Admin) {
		this.roomName = roomName;
		this.Admin = Admin;
	}

	public Set<SocketChannel> getSCS() {
		return this.scs;
	}

	public int getMembers() {
		return users.size();
	}

	public List<User> getUsers() {
		return this.users;
	}

	public boolean isFull() {
		return users.size() >= MAX_MEMBER;
	}

	public boolean isActive() {
		return isActive;
	}

	public void closeRoom() {
		isActive = false;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public String getRoomName() {
		return this.roomName;
	}

	public String getAdmin() {
		return this.Admin;
	}

	public void setAdmin(String Admin) {
		this.Admin = Admin;
	}

	public void addUser(User u) {
		users.add(u);
		scs.add(u.getSC());
	}

	public void deleteUser(User u) {
		scs.remove(u.getSC());
		Iterator<User> it = users.iterator();
		while (it.hasNext()) {
			User temp = it.next();
			if (temp.equals(u)) {
				it.remove();
			}
		}
	}

}
