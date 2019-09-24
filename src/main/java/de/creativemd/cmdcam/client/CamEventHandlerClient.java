package de.creativemd.cmdcam.client;

import java.util.ArrayList;
import java.util.Iterator;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.realmsclient.gui.ChatFormatting;

import de.creativemd.cmdcam.CMDCam;
import de.creativemd.cmdcam.client.interpolation.CamInterpolation;
import de.creativemd.cmdcam.client.mode.CamMode;
import de.creativemd.cmdcam.client.mode.OutsideMode;
import de.creativemd.cmdcam.common.packet.GetPathPacket;
import de.creativemd.cmdcam.common.packet.SetPathPacket;
import de.creativemd.cmdcam.common.utils.CamPath;
import de.creativemd.cmdcam.common.utils.CamPoint;
import de.creativemd.cmdcam.common.utils.CamTarget;
import de.creativemd.cmdcam.common.utils.vec.Vec3;
import de.creativemd.cmdcam.server.CamCommandServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CamEventHandlerClient {
	
	public static Minecraft mc = Minecraft.getInstance();
	public static float defaultfov = 70.0F;
	public static final float amountZoom = 0.1F;
	public static final float amountroll = 0.5F;
	
	public static double fov;
	public static float roll = 0;
	
	@SubscribeEvent
	public void clientCamEvent(ClientChatEvent event) {
		String message = event.getMessage();
		if (message.startsWith("/cam")) {
			try {
				String[] tempArgs = CommandLineUtils.translateCommandline(message);
				String[] args = new String[tempArgs.length - 1];
				for (int i = 0; i < args.length; i++) {
					args[i] = tempArgs[i + 1];
				}
				PlayerEntity sender = mc.player;
				if (args.length == 0) {
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam add [number] " + ChatFormatting.RED + "register a point at the current position"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam clear " + ChatFormatting.RED + "delete all registered points"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam start [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam stop " + ChatFormatting.RED + "stops the animation"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates point to current location"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam target <none:self> " + ChatFormatting.RED + "set the camera target"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam show <all:" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "shows the path using the given interpolation"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam hide <all:" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "hides the path using the given interpolation"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam save <name> " + ChatFormatting.RED + "saves the current path (including settings) with the given name"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam load <name> " + ChatFormatting.RED + "tries to load the saved path with the given name"));
					sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam list " + ChatFormatting.RED + "lists all saved paths"));
				} else {
					String subCommand = args[0];
					if (subCommand.equals("clear")) {
						sender.sendMessage(new StringTextComponent("Cleared all registered points!"));
						CMDCamClient.points.clear();
					}
					if (subCommand.equals("add")) {
						if (args.length == 1) {
							CMDCamClient.points.add(new CamPoint());
							sender.sendMessage(new StringTextComponent("Registered " + CMDCamClient.points.size() + ". Point!"));
						} else if (args.length == 2) {
							try {
								Integer index = Integer.parseInt(args[1]) - 1;
								if (index >= 0 && index < CMDCamClient.points.size()) {
									CMDCamClient.points.add(index, new CamPoint());
									sender.sendMessage(new StringTextComponent("Inserted " + index + ". Point!"));
								} else
									sender.sendMessage(new StringTextComponent("The given index '" + args[1] + "' is too high/low!"));
							} catch (Exception e) {
								sender.sendMessage(new StringTextComponent("Invalid index '" + args[1] + "'!"));
							}
							
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam add [number] " + ChatFormatting.RED + "register a point at the current position."));
					}
					if (subCommand.equals("start")) {
						if (args.length >= 2) {
							long duration = CamCommandServer.StringToDuration(args[1]);
							if (duration > 0)
								CMDCamClient.lastDuration = duration;
							else {
								sender.sendMessage(new StringTextComponent("Invalid time '" + args[1] + "'!"));
								return;
							}
							
							if (args.length >= 3) {
								CMDCamClient.lastLoop = Integer.parseInt(args[2]);
							}
						}
						try {
							CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
						} catch (PathParseException e) {
							sender.sendMessage(new StringTextComponent(e.getMessage()));
						}
					}
					if (subCommand.equals("stop")) {
						CMDCamClient.stopPath();
					}
					if (subCommand.equals("remove")) {
						if (args.length >= 2) {
							try {
								Integer index = Integer.parseInt(args[1]) - 1;
								if (index >= 0 && index < CMDCamClient.points.size()) {
									CMDCamClient.points.remove((int) index);
									sender.sendMessage(new StringTextComponent("Removed " + (index + 1) + ". point!"));
								} else
									sender.sendMessage(new StringTextComponent("The given index '" + args[1] + "' is too high/low!"));
							} catch (Exception e) {
								sender.sendMessage(new StringTextComponent("Invalid index '" + args[1] + "'!"));
							}
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"));
					}
					if (subCommand.equals("set")) {
						if (args.length >= 2) {
							try {
								Integer index = Integer.parseInt(args[1]) - 1;
								if (index >= 0 && index < CMDCamClient.points.size()) {
									CMDCamClient.points.set(index, new CamPoint());
									sender.sendMessage(new StringTextComponent("Updated " + (index + 1) + ". point!"));
								} else
									sender.sendMessage(new StringTextComponent("The given index '" + args[1] + "' is too high/low!"));
							} catch (Exception e) {
								sender.sendMessage(new StringTextComponent("Invalid index '" + args[1] + "'!"));
							}
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates the giveng point to the current location"));
					}
					if (subCommand.equals("goto")) {
						if (args.length >= 2) {
							try {
								Integer index = Integer.parseInt(args[1]) - 1;
								if (index >= 0 && index < CMDCamClient.points.size()) {
									CamPoint point = CMDCamClient.points.get(index);
									mc.player.abilities.isFlying = true;
									
									CamEventHandlerClient.roll = (float) point.roll;
									mc.gameSettings.fov = (float) point.zoom;
									mc.player.setPositionAndRotation(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
									mc.player.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
								} else
									sender.sendMessage(new StringTextComponent("The given index '" + args[1] + "' is too high/low!"));
							} catch (Exception e) {
								sender.sendMessage(new StringTextComponent("Invalid index '" + args[1] + "'!"));
							}
						} else {
							sender.sendMessage(new StringTextComponent("Missing point!"));
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"));
						}
					}
					if (subCommand.equals("mode")) {
						if (args.length >= 2) {
							if (args[1].equals("default") || args[1].equals("outside")) {
								CMDCamClient.lastMode = args[1];
								sender.sendMessage(new StringTextComponent("Changed to " + args[1] + " path!"));
							} else
								sender.sendMessage(new StringTextComponent("Path mode '" + args[1] + "' does not exit!"));
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"));
					}
					if (subCommand.equals("target")) {
						if (args.length == 2) {
							String target = args[1];
							if (target.equals("self")) {
								CMDCamClient.target = new CamTarget.SelfTarget();
								sender.sendMessage(new StringTextComponent("The camera will point towards you!"));
							} else if (target.equals("none")) {
								CMDCamClient.target = null;
								sender.sendMessage(new StringTextComponent("Removed target!"));
							} else
								sender.sendMessage(new StringTextComponent("Target '" + target + "' not found!"));
						} else {
							CamEventHandlerClient.selectEntityMode = true;
							sender.sendMessage(new StringTextComponent("Please select a target either an entity or a block!"));
						}
						
					}
					if (subCommand.equals("interpolation")) {
						if (args.length == 2) {
							String target = args[1];
							CamInterpolation move = CamInterpolation.getInterpolation(target);
							if (move != null) {
								CMDCamClient.lastInterpolation = target;
								sender.sendMessage(new StringTextComponent("Interpolation is set to '" + target + "'!"));
							} else
								sender.sendMessage(new StringTextComponent("Interpolation '" + target + "' not found!"));
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
					}
					if (subCommand.equals("follow-speed")) {
						if (args.length == 2) {
							try {
								double followspeed = Double.parseDouble(args[1]);
								CMDCamClient.cameraFollowSpeed = followspeed;
								sender.sendMessage(new StringTextComponent("Camera follow speed is set to  '" + followspeed + "'. Default is 1.0!"));
							} catch (NumberFormatException e) {
								sender.sendMessage(new StringTextComponent("'" + args[1] + "' is an invalid number!"));
							}
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"));
					}
					if (subCommand.equals("show")) {
						if (args.length == 2) {
							String target = args[1];
							CamInterpolation move = CamInterpolation.getInterpolation(target);
							if (move != null) {
								move.isRenderingEnabled = true;
								sender.sendMessage(new StringTextComponent("Showing '" + target + "' interpolation path!"));
							} else if (target.equals("all")) {
								for (CamInterpolation movement : CamInterpolation.interpolationTypes.values()) {
									movement.isRenderingEnabled = true;
								}
								sender.sendMessage(new StringTextComponent("Showing all interpolation paths!"));
							} else
								sender.sendMessage(new StringTextComponent("Interpolation '" + target + "' not found!"));
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam show <all:" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "shows the path using the given interpolation"));
					}
					if (subCommand.equals("hide")) {
						if (args.length == 2) {
							String target = args[1];
							CamInterpolation move = CamInterpolation.getInterpolation(target);
							if (move != null) {
								move.isRenderingEnabled = false;
								sender.sendMessage(new StringTextComponent("Hiding '" + target + "' interpolation path!"));
							} else if (target.equals("all")) {
								for (CamInterpolation movement : CamInterpolation.interpolationTypes.values()) {
									movement.isRenderingEnabled = false;
								}
								sender.sendMessage(new StringTextComponent("Hiding all interpolation paths!"));
							} else
								sender.sendMessage(new StringTextComponent("Interpolation '" + target + "' not found!"));
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam hide <all:" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "hides the path using the given interpolation"));
					}
					if (subCommand.equals("save")) {
						if (args.length == 2) {
							try {
								CamPath path = CMDCamClient.createPathFromCurrentConfiguration();
								
								if (CMDCamClient.isInstalledOnSever) {
									CMDCam.NETWORK.sendToServer(new SetPathPacket(args[1], path));
								} else {
									CMDCamClient.savedPaths.put(args[1], path);
									sender.sendMessage(new StringTextComponent("Saved path '" + args[1] + "' successfully!"));
								}
							} catch (PathParseException e) {
								sender.sendMessage(new StringTextComponent(e.getMessage()));
							}
							
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam save <name> " + ChatFormatting.RED + "saves the current path (including settings) with the given name"));
					}
					if (subCommand.equals("load")) {
						if (args.length == 2) {
							if (CMDCamClient.isInstalledOnSever) {
								CMDCam.NETWORK.sendToServer(new GetPathPacket(args[1]));
							} else {
								CamPath path = CMDCamClient.savedPaths.get(args[1]);
								if (path != null) {
									path.overwriteClientConfig();
									sender.sendMessage(new StringTextComponent("Loaded path '" + args[1] + "' successfully!"));
								} else
									sender.sendMessage(new StringTextComponent("Could not find path '" + args[1] + "'!"));
							}
						} else
							sender.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam load <name> " + ChatFormatting.RED + "tries to load the saved path with the given name"));
					}
					if (subCommand.equals("list")) {
						if (CMDCamClient.isInstalledOnSever) {
							sender.sendMessage(new StringTextComponent("Use /cam-server list instead!"));
							return;
						}
						String output = "There are " + CMDCamClient.savedPaths.size() + " path(s) in total. ";
						for (String key : CMDCamClient.savedPaths.keySet()) {
							output += key + ", ";
						}
						sender.sendMessage(new StringTextComponent(output));
					}
					event.setCanceled(true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@SubscribeEvent
	public void onRenderTick(RenderTickEvent event) {
		if (mc.world == null)
			CMDCamClient.isInstalledOnSever = false;
		
		if (mc.player != null && mc.world != null) {
			if (!mc.isGamePaused()) {
				if (CMDCamClient.getCurrentPath() == null) {
					if (KeyHandler.zoomIn.isKeyDown()) {
						if (mc.player.isSneaking())
							mc.gameSettings.fov -= amountZoom * 10;
						else
							mc.gameSettings.fov -= amountZoom;
					}
					
					if (KeyHandler.zoomOut.isKeyDown()) {
						if (mc.player.isSneaking())
							mc.gameSettings.fov += amountZoom * 10;
						else
							mc.gameSettings.fov += amountZoom;
					}
					
					if (KeyHandler.zoomCenter.isKeyDown()) {
						mc.gameSettings.fov = defaultfov;
					}
					fov = mc.gameSettings.fov;
					
					if (KeyHandler.rollLeft.isKeyDown())
						roll -= amountroll;
					
					if (KeyHandler.rollRight.isKeyDown())
						roll += amountroll;
					
					if (KeyHandler.rollCenter.isKeyDown())
						roll = 0;
					
					if (KeyHandler.pointKey.isPressed()) {
						CMDCamClient.points.add(new CamPoint());
						mc.player.sendMessage(new StringTextComponent("Registered " + CMDCamClient.points.size() + ". Point!"));
					}
					
				} else {
					CMDCamClient.tickPath(mc.world, event.renderTickTime);
				}
				
				if (KeyHandler.startStop.isPressed()) {
					if (CMDCamClient.getCurrentPath() != null) {
						CMDCamClient.stopPath();
					} else
						try {
							CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
						} catch (PathParseException e) {
							mc.player.sendMessage(new StringTextComponent(e.getMessage()));
						}
				}
			}
		}
		lastRenderTime = System.nanoTime();
	}
	
	@SubscribeEvent
	public void worldRender(RenderWorldLastEvent event) {
		boolean shouldRender = false;
		for (CamInterpolation movement : CamInterpolation.interpolationTypes.values()) {
			if (movement.isRenderingEnabled) {
				shouldRender = true;
				break;
			}
		}
		if (CMDCamClient.getCurrentPath() == null && shouldRender && CMDCamClient.points.size() > 0) {
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);
			GlStateManager.disableTexture();
			GL11.glDepthMask(false);
			
			Vec3[] points = new Vec3[CMDCamClient.points.size()];
			for (int i = 0; i < points.length; i++) {
				points[i] = new Vec3(CMDCamClient.points.get(i).x, CMDCamClient.points.get(i).y, CMDCamClient.points.get(i).z);
				GlStateManager.pushMatrix();
				GlStateManager.translated(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY + mc.player.getEyeHeight() - 0.1, -TileEntityRendererDispatcher.staticPlayerZ);
				renderBlock(points[i].x, points[i].y, points[i].z, 0.1, 0.1, 0.1, 0, 0, 0, 1, 1, 1, 1);
				ActiveRenderInfo activerenderinfo = TileEntityRendererDispatcher.instance.renderInfo;
				float f = activerenderinfo.getYaw();
				float f1 = activerenderinfo.getPitch();
				GameRenderer.drawNameplate(mc.fontRenderer, (i + 1) + "", (float) points[i].x, (float) points[i].y + 0.4F, (float) points[i].z, 0, f, f1, false);
				GL11.glDepthMask(false);
				GlStateManager.disableLighting();
				GlStateManager.disableTexture();
				GlStateManager.popMatrix();
			}
			
			for (Iterator<CamInterpolation> iterator = CamInterpolation.interpolationTypes.values().iterator(); iterator.hasNext();) {
				CamInterpolation movement = iterator.next();
				if (movement.isRenderingEnabled)
					renderMovement(movement, new ArrayList<>(CMDCamClient.points));
			}
			
			GL11.glDepthMask(true);
			GlStateManager.enableTexture();
			GlStateManager.enableBlend();
			GlStateManager.clearCurrentColor();
			
		}
	}
	
	public static void renderBlock(double x, double y, double z, double width, double height, double length, double rotateX, double rotateY, double rotateZ, double red, double green, double blue, double alpha) {
		GlStateManager.pushMatrix();
		GlStateManager.translated(x, y, z);
		GlStateManager.enableRescaleNormal();
		GlStateManager.rotatef((float) rotateX, 1, 0, 0);
		GlStateManager.rotatef((float) rotateY, 0, 1, 0);
		GlStateManager.rotatef((float) rotateZ, 0, 0, 1);
		GlStateManager.scaled(width, height, length);
		GlStateManager.color4f((float) red, (float) green, (float) blue, (float) alpha);
		
		GL11.glBegin(GL11.GL_POLYGON);
		GL11.glNormal3f(0.0f, 1.0f, 0.0f);
		GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(0.5f, 0.5f, -0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(0.0f, 0.0f, 1.0f);
		GL11.glVertex3f(0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(1.0f, 0.0f, 0.0f);
		GL11.glVertex3f(0.5f, 0.5f, -0.5f);
		GL11.glVertex3f(0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(0.5f, -0.5f, -0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(-1.0f, 0.0f, 0.0f);
		GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(0.0f, -1.0f, 0.0f);
		GL11.glVertex3f(0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
		GL11.glVertex3f(0.5f, -0.5f, -0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(0.0f, 0.0f, -1.0f);
		GL11.glVertex3f(0.5f, 0.5f, -0.5f);
		GL11.glVertex3f(0.5f, -0.5f, -0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
		GL11.glEnd();
		
		GlStateManager.popMatrix();
	}
	
	public void renderMovement(CamInterpolation movement, ArrayList<CamPoint> points) {
		try {
			movement.initMovement(points, 0, CMDCamClient.target);
		} catch (PathParseException e) {
			return;
		}
		
		double steps = 20 * (points.size() - 1);
		
		GlStateManager.pushMatrix();
		Vec3 color = movement.getColor();
		GL11.glColor3d(color.x, color.y, color.z);
		GlStateManager.translated(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY + mc.player.getEyeHeight() - 0.1, -TileEntityRendererDispatcher.staticPlayerZ);
		GlStateManager.lineWidth(1.0F);
		GL11.glBegin(GL11.GL_LINE_STRIP);
		for (int i = 0; i < steps; i++) {
			CamPoint pos = CamMode.getPoint(movement, points, i / steps, 0, 0);
			GL11.glVertex3d(pos.x, pos.y, pos.z);
		}
		GL11.glVertex3d(points.get(points.size() - 1).x, points.get(points.size() - 1).y, points.get(points.size() - 1).z);
		GL11.glEnd();
		GlStateManager.popMatrix();
	}
	
	public Entity renderEntity;
	
	@SubscribeEvent
	public void renderPlayerPre(RenderPlayerEvent.Pre event) {
		if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
			renderEntity = mc.renderViewEntity;
			
			mc.renderViewEntity = mc.player;
		}
	}
	
	@SubscribeEvent
	public void renderPlayerPost(RenderPlayerEvent.Post event) {
		if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
			mc.renderViewEntity = renderEntity;
		}
	}
	
	@SubscribeEvent
	public void cameraRoll(CameraSetup event) {
		event.setRoll(roll);
	}
	
	public static long lastRenderTime;
	
	public static boolean shouldPlayerTakeInput() {
		return true;
	}
	
	public static boolean selectEntityMode = false;
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!selectEntityMode)
			return;
		
		if (event instanceof EntityInteract) {
			CMDCamClient.target = new CamTarget.EntityTarget(((EntityInteract) event).getTarget());
			event.getPlayer().sendMessage(new StringTextComponent("Target is set to " + ((EntityInteract) event).getTarget().getCachedUniqueIdString() + "."));
			selectEntityMode = false;
		}
		
		if (event instanceof RightClickBlock) {
			CMDCamClient.target = new CamTarget.BlockTarget(event.getPos());
			event.getPlayer().sendMessage(new StringTextComponent("Target is set to " + event.getPos() + "."));
			selectEntityMode = false;
		}
	}
}
