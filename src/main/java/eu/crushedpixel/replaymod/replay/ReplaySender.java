package eu.crushedpixel.replaymod.replay;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S36PacketSignEditorOpen;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.network.play.server.S14PacketEntity.S17PacketEntityLookMove;
import net.minecraft.util.Timer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;

import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.reflection.MCPNames;

@Sharable
public class ReplaySender extends ChannelInboundHandlerAdapter {

	private long currentTimeStamp;
	private boolean hurryToTimestamp;
	private long desiredTimeStamp;
	private long lastTimeStamp, lastPacketSent;

	private long toleratedTimeStamp;

	private File replayFile;
	private PacketDeserializer ds = new PacketDeserializer(EnumPacketDirection.SERVERBOUND);
	private boolean active = true;
	private ZipFile archive;
	private DataInputStream dis;
	private ChannelHandlerContext ctx = null;

	private boolean startFromBeginning = true;

	private NetworkManager networkManager;
	private boolean terminate = false;

	private float defaultReplaySpeed = 0.5f;

	private int packetAt = 0;
	private double replaySpeed = 1f;
	private long lastTime = 0, lastTimestamp = 0;

	private Field joinPacketEntityId, joinPacketWorldType,
	joinPacketDimension, joinPacketDifficulty, joinPacketMaxPlayers;

	private Field effectPacketEntityId;
	private Field metadataPacketEntityId, metadataPacketList;

	private Field animationPacketEntityId;
	private Field entityDataWatcher;

	private Field chatPacketPosition;

	private Minecraft mc = Minecraft.getMinecraft();
	private Field mcTimer;

	private long now = System.currentTimeMillis();

	private int replayLength = 0;

	private EffectRenderer old = mc.effectRenderer;

	private ZipArchiveEntry replayEntry;

	public boolean isHurrying() {
		return hurryToTimestamp;
	}

	public long currentTimeStamp() {
		return currentTimeStamp;
	}

	public int replayLength() {
		return replayLength;
	}

	public void terminateReplay() {
		terminate = true;
		try {
			channelInactive(ctx);
			ctx.channel().pipeline().close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void jumpToTime(int millis, boolean forceReload) {
		setReplaySpeed(replaySpeed);

		if((millis < currentTimeStamp && !isHurrying()) || forceReload) {
			if(forceReload) System.out.println("forced");
			if(ReplayHandler.isReplaying()) {
				if(forceReload) {
					startFromBeginning = true;
				}
				else {
					startFromBeginning = false;
					desiredTimeStamp = millis;
					hurryToTimestamp = false;
					return;
				}
			} else {
				startFromBeginning = true;
			}
		}

		desiredTimeStamp = millis;
		hurryToTimestamp = true;
	}

	public void setReplaySpeed(final double d) {
		if(d != 0) this.replaySpeed = d;

		try {
			Timer timer = (Timer)mcTimer.get(mc);
			timer.timerSpeed = (float)d;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public ReplaySender(final File replayFile, NetworkManager nm) {
		try {
			joinPacketEntityId = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149206_a"));
			joinPacketEntityId.setAccessible(true);

			joinPacketDifficulty = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149203_e"));
			joinPacketDifficulty.setAccessible(true);

			joinPacketDimension = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149202_d"));
			joinPacketDimension.setAccessible(true);

			joinPacketMaxPlayers = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149200_f"));
			joinPacketMaxPlayers.setAccessible(true);

			joinPacketWorldType = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149201_g"));
			joinPacketWorldType.setAccessible(true);

			mcTimer = Minecraft.class.getDeclaredField(MCPNames.field("field_71428_T"));
			mcTimer.setAccessible(true);

			effectPacketEntityId = S1DPacketEntityEffect.class.getDeclaredField(MCPNames.field("field_149434_a"));
			effectPacketEntityId.setAccessible(true);

			metadataPacketEntityId = S1CPacketEntityMetadata.class.getDeclaredField(MCPNames.field("field_149379_a"));
			metadataPacketEntityId.setAccessible(true);

			metadataPacketList = S1CPacketEntityMetadata.class.getDeclaredField(MCPNames.field("field_149378_b"));
			metadataPacketList.setAccessible(true);

			animationPacketEntityId = S0BPacketAnimation.class.getDeclaredField(MCPNames.field("field_148981_a"));
			animationPacketEntityId.setAccessible(true);

			entityDataWatcher = Entity.class.getDeclaredField(MCPNames.field("field_70180_af"));
			entityDataWatcher.setAccessible(true);

			chatPacketPosition = S02PacketChat.class.getDeclaredField(MCPNames.field("field_179842_b"));
			chatPacketPosition.setAccessible(true);

		} catch (Exception e) {
			e.printStackTrace();
		}

		this.replayFile = replayFile;
		this.networkManager = nm;
		if(("."+FilenameUtils.getExtension(replayFile.getAbsolutePath())).equals(ConnectionEventHandler.ZIP_FILE_EXTENSION)) {
			try {
				archive = new ZipFile(replayFile);
				replayEntry = archive.getEntry("recording"+ConnectionEventHandler.TEMP_FILE_EXTENSION);

				ZipArchiveEntry metadata = archive.getEntry("metaData"+ConnectionEventHandler.JSON_FILE_EXTENSION);
				InputStream is = archive.getInputStream(metadata);
				BufferedReader br = new BufferedReader(new InputStreamReader(is));

				String json = br.readLine();

				ReplayMetaData metaData = new Gson().fromJson(json, ReplayMetaData.class);

				this.replayLength = metaData.getDuration();

				sender.start();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Thread sender = new Thread(new Runnable() {

		@Override
		public void run() {

			try {
				dis = new DataInputStream(archive.getInputStream(replayEntry));
			} catch(Exception e) {
				e.printStackTrace();
			}

			try {
				while(ctx == null && !terminate) {
					Thread.sleep(10);
				}
				while(!terminate) {
					if(startFromBeginning) {
						currentTimeStamp = 0;
						dis = new DataInputStream(archive.getInputStream(replayEntry));
						startFromBeginning = false;
						ReplayHandler.restartReplay();
					}
					while(!startFromBeginning && (!paused() || FMLClientHandler.instance().isGUIOpen(GuiDownloadTerrain.class))) {
						try {

							/*
							 * LOGIC: 
							 * While behind desired timestamp, only send packets
							 * until desired timestamp is reached,
							 * then increase desired timestamp by 1/20th of a second
							 * 
							 * Desired timestamp is divided through stretch factor.
							 * 
							 * If hurrying, don't wait for correct timing.
							 */

							if(!hurryToTimestamp && ReplayHandler.isReplaying()) {
								continue;
							} else if(ReplayHandler.isReplaying()) {
								System.out.println("nocont "+currentTimeStamp+" | "+((Timer)mcTimer.get(mc)).timerSpeed);
							}

							int timestamp = dis.readInt();

							currentTimeStamp = timestamp;

							if(!ReplayHandler.isReplaying() && !hurryToTimestamp && !FMLClientHandler.instance().isGUIOpen(GuiDownloadTerrain.class)) {
								int timeWait = (int)Math.round((currentTimeStamp - lastTimeStamp)/replaySpeed);
								long timeDiff = System.currentTimeMillis() - lastPacketSent;
								lastPacketSent = System.currentTimeMillis();
								Thread.sleep(Math.max(0, timeWait-timeDiff));
							}

							int bytes = dis.readInt();
							byte[] bb = new byte[bytes];
							dis.readFully(bb);

							ReplaySender.this.channelRead(ctx, bb);

							lastTimeStamp = currentTimeStamp;

							if(hurryToTimestamp && currentTimeStamp >= desiredTimeStamp && !startFromBeginning) {
								toleratedTimeStamp = currentTimeStamp;
								hurryToTimestamp = false;
								System.out.println("stop hurrying");
							}

						} catch(EOFException eof) {
							setReplaySpeed(0);
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	});

	private List<Class> packetClasses = new ArrayList<Class>();
	
	private static Field field_149074_a; //TODO: REMOVE
	static {
		try {
			field_149074_a = S14PacketEntity.class.getDeclaredField(MCPNames.field("field_149074_a"));
			field_149074_a.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if(terminate) {
			return;
		}

		if(msg instanceof Packet) {
			super.channelRead(ctx, msg);
			return;
		}
		byte[] ba = (byte[])msg;

		try {
			ByteBuf bb = Unpooled.wrappedBuffer(ba);
			PacketBuffer pb = new PacketBuffer(bb);

			pb.writeBytes(ba);

			int i = pb.readVarIntFromBuffer();

			Packet p = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, i);

			if(hurryToTimestamp) { //If hurrying, ignore some packets
				if(p instanceof S45PacketTitle ||
						p instanceof S29PacketSoundEffect) return;
			}

			if(p instanceof S28PacketEffect) {
				return;
			}

			if(p instanceof S2BPacketChangeGameState) { //TODO: Test with rain
				return;
			}

			if(p instanceof S06PacketUpdateHealth) {
				return;
			}

			if(p instanceof S2DPacketOpenWindow) {
				return;
			}

			if(p instanceof S2EPacketCloseWindow) {
				return;
			}

			if(p instanceof S2FPacketSetSlot) {
				return;
			}

			if(p instanceof S30PacketWindowItems) {
				return;
			}

			if(p instanceof S36PacketSignEditorOpen) {
				return;
			}

			if(p instanceof S02PacketChat) {
				byte pos = (Byte)chatPacketPosition.get(p);
				if(pos == 1) { //Ignores command block output sent
					return;
				}
			}

			if(p instanceof S37PacketStatistics) {
				return;
			}

			try {
				p.readPacketData(pb);

				if(p instanceof S0CPacketSpawnPlayer) {
					S0CPacketSpawnPlayer sp = (S0CPacketSpawnPlayer)p;
					System.out.println("PACKET SPAWN PLAYER ----------");
					System.out.println("ENTITY ID: "+sp.func_148943_d());
					System.out.println("UUID: "+sp.func_179819_c());
					System.out.println("X: "+sp.func_148942_f());
					System.out.println("Y: "+sp.func_148949_g());
					System.out.println("Z: "+sp.func_148946_h());
					System.out.println("YAW: "+sp.func_148941_i());
					System.out.println("PITCH: "+sp.func_148945_j());
					System.out.println("ITEM: "+sp.func_148947_k());
					System.out.println("PACKET END -------------------");
				}
				
				if(p instanceof S01PacketJoinGame) {

					int entId = (Integer)joinPacketEntityId.get(p);
					entId = -1;
					int dimension = (Integer)joinPacketDimension.get(p);
					EnumDifficulty difficulty = (EnumDifficulty)joinPacketDifficulty.get(p);
					int maxPlayers = (Integer)joinPacketMaxPlayers.get(p);
					WorldType worldType = (WorldType)joinPacketWorldType.get(p);

					p = new S01PacketJoinGame(entId, GameType.SPECTATOR, false, dimension, 
							difficulty, maxPlayers, worldType, false);
				}

				if(p instanceof S39PacketPlayerAbilities) {
					return;
				}

				if(p instanceof S08PacketPlayerPosLook) {
					final S08PacketPlayerPosLook ppl = (S08PacketPlayerPosLook)p;

					if(ReplayHandler.isReplaying()) return;
					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							while(mc.theWorld == null) {
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}

							Entity ent = ReplayHandler.getCameraEntity();
							if(ent == null || !(ent instanceof CameraEntity)) ent = new CameraEntity(mc.theWorld);
							CameraEntity cent = (CameraEntity)ent;
							cent.moveAbsolute(ppl.func_148932_c(), ppl.func_148928_d(), ppl.func_148933_e());

							ReplayHandler.setCameraEntity(cent);
						}
					});

					t.start();
				}

				if(p instanceof S43PacketCamera) {
					return;
				}
				if(ReplayHandler.isReplaying())
					System.out.println("packet arrived");
				super.channelRead(ctx, p);
			} catch(Exception e) {
				System.out.println(p.getClass());
				e.printStackTrace();
			}

		} catch(Exception e) {
			//e.printStackTrace();
		}

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
		networkManager.channel().attr(networkManager.attrKeyConnectionState).set(EnumConnectionState.PLAY);
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		archive.close();
		super.channelInactive(ctx);
	}

	public boolean paused() {
		try {
			return ((Timer)mcTimer.get(mc)).timerSpeed == 0;
		} catch(Exception e) {}
		return true;
	}

	public double getReplaySpeed() {
		if(!paused()) return replaySpeed;
		else return 0;
		//return timeInfo.get().getSpeed();
	}

}
