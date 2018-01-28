package wlcp.gameserver.tasks;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import wlcp.gameserver.model.ClientData;
import wlcp.gameserver.module.ModuleManager;
import wlcp.gameserver.module.Modules;
import wlcp.gameserver.modules.TaskManagerModule;
import wlcp.gameserver.task.ITask;
import wlcp.gameserver.task.Task;
import wlcp.shared.packet.IPacket;
import wlcp.shared.packet.PacketTypes;
import wlcp.shared.packets.ConnectPacket;
import wlcp.shared.packets.GameLobbiesPacket;
import wlcp.shared.packets.GamePacket;
import wlcp.shared.packets.ServerPacket;
import wlcp.shared.packets.SingleButtonPressPacket;
import wlcp.shared.packets.StartGameInstancePacket;

class PacketClientData {
	public IPacket packet;
	public ClientData clientData;
	public PacketClientData(IPacket packet, ClientData clientData) {
		this.packet = packet;
		this.clientData = clientData;
	}
}

public class PacketDistributorTask extends Task implements ITask {
	
	private ConcurrentLinkedQueue<PacketClientData> recievedPackets;
	private ConcurrentLinkedQueue<PacketClientData> packetsToSend;

	public PacketDistributorTask() {
		super("Packet Distributor");
		recievedPackets = new ConcurrentLinkedQueue<PacketClientData>();
		packetsToSend = new ConcurrentLinkedQueue<PacketClientData>();
	}
	
	public void DataRecieved(ClientData clientData) {
		ByteBuffer byteBuffer = clientData.getBuffer();
		byteBuffer.flip();
		PacketTypes packetType = null;
		try {
			packetType = PacketTypes.values()[byteBuffer.get(0)];
		} catch (Exception e) {
			HandleWebSocket(clientData);
			return;
		}
		switch(packetType) {
		case START_GAME_INSTANCE:
			AddPacket(new StartGameInstancePacket(), clientData);
			break;
		case GAME_LOBBIES:
			AddPacket(new GameLobbiesPacket(), clientData);
			break;
		case CONNECT:
			AddPacket(new ConnectPacket(), clientData);
			break;
		case SINGLE_BUTTON_PRESS:
			AddPacket(new SingleButtonPressPacket(), clientData);
			break;
		default:
			break;
		}
	}
	
	private void HandleWebSocket(ClientData clientData) {
		
		//Check if the first 3 bytes are GET
		if(clientData.getBuffer().get(0) == 'G' && clientData.getBuffer().get(1) == 'E' && clientData.getBuffer().get(2) == 'T') {
			
			//If so its a connect request
			String handshake = StandardCharsets.UTF_8.decode(clientData.getBuffer()).toString();
			Matcher get = Pattern.compile("^GET").matcher(handshake);
			if (get.find()) {
			    Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(handshake);
			    match.find();
			    byte[] response;
				try {
					response = ("HTTP/1.1 101 Switching Protocols\r\n"
					        + "Connection: Upgrade\r\n"
					        + "Upgrade: websocket\r\n"
					        + "Sec-WebSocket-Accept: "
					        + DatatypeConverter
					        .printBase64Binary(
					                MessageDigest
					                .getInstance("SHA-1")
					                .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
					                        .getBytes("UTF-8")))
					        + "\r\n\r\n")
					        .getBytes("UTF-8");

				    clientData.getClientSocket().write(ByteBuffer.wrap(response));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			//Actual Packet
			if(clientData.getBuffer().get() == -126) {
				byte lengthByte = (byte) (clientData.getBuffer().get() & 127);
				System.out.println("Packet length: " + lengthByte);
				byte[] masks = new byte[4];
				ByteBuffer byteBuffer = ByteBuffer.allocate(65535);
				for(int i = 0; i < 4; i++) {
					masks[i] = clientData.getBuffer().get();
				}
				for(int i = 0; i < (int)lengthByte; i++) {
					byte data = (byte) (clientData.getBuffer().get() ^ masks[i % 4]);
					byteBuffer.put(data);
				}
				
				clientData.setBuffer(byteBuffer);
				clientData.setWebSocket(true);
				DataRecieved(clientData);
			}
		}
	}
	
	private void WebSocketHandleShake(ClientData clientData) {
		
		String handshake = "";
		
		handshake = StandardCharsets.UTF_8.decode(clientData.getBuffer()).toString();
		
		//translate bytes of request to string
//		for(int i = 0; i < clientData.getBuffer().remaining(); i++) {
//			handshake += (char) clientData.getBuffer().get();
//		}
		
		//clientData.getClientSocket().write(ByteBuffer.wrap(new String("HTTP/1.1 200 OK\r\nConnection: keep-alive\r\n\r\n").getBytes()));
		


		Matcher get = Pattern.compile("^GET").matcher(handshake);
		
		if (get.find()) {
		    Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(handshake);
		    match.find();
		    byte[] response;
			try {
				response = ("HTTP/1.1 101 Switching Protocols\r\n"
				        + "Connection: Upgrade\r\n"
				        + "Upgrade: websocket\r\n"
				        + "Sec-WebSocket-Accept: "
				        + DatatypeConverter
				        .printBase64Binary(
				                MessageDigest
				                .getInstance("SHA-1")
				                .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
				                        .getBytes("UTF-8")))
				        + "\r\n\r\n")
				        .getBytes("UTF-8");

			    clientData.getClientSocket().write(ByteBuffer.wrap(response));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	public ByteBuffer HandleSocket(ByteBuffer byteBuffer) {
		ByteBuffer bb = ByteBuffer.allocate(65535);
		bb.put((byte) -126);
		
		byte[] mask = {5, 28, -23, -67};
		
		bb.put((byte) ((byte)byteBuffer.limit() + (1 << 7)));
		bb.put(mask[0]);
		bb.put(mask[1]);
		bb.put(mask[2]);
		bb.put(mask[3]);
		
		for(int i = 0; i < byteBuffer.limit(); i++) {
			byte data = (byte) (byteBuffer.get() ^ mask[i % 4]);
			bb.put(data);
		}
		
		bb.flip();
		return bb;
	}
	
	public void AddPacket(IPacket packet, ClientData clientData) {
		try {
			accquire();
			packet.populateData(clientData.getBuffer());
			recievedPackets.add(new PacketClientData(packet, clientData));
			release();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void AddPacketToSend(IPacket packet, ClientData clientData) {
		try {
			accquire();
			packetsToSend.add(new PacketClientData(packet, clientData));
			release();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void Update() {
	
		//Loop through all of packets recieved
		try {
			accquire();
			for(PacketClientData data : recievedPackets) {
				if(data.packet instanceof GamePacket) {
					for(Task task : ((TaskManagerModule) ModuleManager.getInstance().getModule(Modules.TASK_MANAGER)).getTasks()) {
						if(task instanceof GameInstanceTask) {
							if(((GameInstanceTask) task).getGameInstanceId() == ((GamePacket) data.packet).getGameInstanceId()) {
								((GameInstanceTask) task).DistributePacket(data);
								recievedPackets.remove(data);
							}
						}
					}
				}
				if(data.packet instanceof ServerPacket) {
					for(Task task : ((TaskManagerModule) ModuleManager.getInstance().getModule(Modules.TASK_MANAGER)).getTasks()) {
						if(task instanceof ServerPacketHandlerTask) {
							((ServerPacketHandlerTask) task).DistributePacket(data);
							recievedPackets.remove(data);
						}
					}
				}
			}
			release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Loop through packet to send
		try {
			accquire();
			for(PacketClientData data : packetsToSend) {
				ByteBuffer buffer = data.packet.assemblePacket();
				if(data.clientData.isWebSocket()) { 
					buffer = HandleSocket(buffer);
				}
				Future<Integer> status = data.clientData.getClientSocket().write(buffer);
				while(!status.isDone()) {}
				packetsToSend.remove(data);
			}
			release();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void CleanUp() {
		
	}
	
	@Override 
	public void ShutDown() {
		
	}
}
