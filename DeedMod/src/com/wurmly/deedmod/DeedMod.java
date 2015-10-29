package com.wurmly.deedmod;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import com.wurmonline.server.LoginHandler;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.questions.VillageFoundationQuestion;

public class DeedMod implements WurmMod, Configurable, ServerStartedListener {

	private Integer settlementsPerSteamID = -1;
	private Integer maxDeedTiles = -1;
	private Integer maxDeedX = -1;
	private Integer maxDeedY = -1;
	private Integer powerOverride = 1;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Override
	public void onServerStarted() {
		logger.log(Level.INFO, "Initializing Deed modifications");
	}

	@Override
	public void configure(Properties properties) {
		settlementsPerSteamID = Integer.parseInt(properties.getProperty("settlementsPerSteamID", Integer.toString(settlementsPerSteamID)));
		maxDeedTiles = Integer.parseInt(properties.getProperty("maxDeedTiles", Integer.toString(maxDeedTiles)));
		maxDeedX = Integer.parseInt(properties.getProperty("maxDeedX", Integer.toString(maxDeedX)));
		maxDeedY = Integer.parseInt(properties.getProperty("maxDeedY", Integer.toString(maxDeedY)));
		powerOverride = Integer.parseInt(properties.getProperty("powerOverride", Integer.toString(powerOverride)));

		logger.log(Level.INFO, "powerOverride: " + powerOverride);
		logger.log(Level.INFO, "settlementsPerSteamID: " + settlementsPerSteamID);
		logger.log(Level.INFO, "maxDeedTiles: " + maxDeedTiles);
		logger.log(Level.INFO, "maxDeedX: " + maxDeedX);
		logger.log(Level.INFO, "maxDeedY: " + maxDeedY);
		

		HookManager.getInstance().registerHook("com.wurmonline.server.questions.VillageFoundationQuestion", "sendIntro", "()V", new InvocationHandler() {
			@Override
			public Object invoke(Object object, Method method, Object[] args) throws Throwable {
				VillageFoundationQuestion vq = (VillageFoundationQuestion) object;
				Creature c = vq.getResponder();
				int deedCount = 0;
				
				if(settlementsPerSteamID == 0 && c.getPower() < powerOverride) {
					c.getCommunicator().sendNormalServerMessage("Sorry, player settlements have been disabled on this server.");
					return null;
				}
				
				if(settlementsPerSteamID > -1 && c.getPower() < powerOverride) {
					for(Village v : Villages.getVillages()) {
						try {
							Player p = Players.getInstance().getPlayer(v.mayorName);
							if(p.SteamId.equals(c.SteamId))
								deedCount++;
						} catch(NoSuchPlayerException nsp) {
							String hashCheck = LoginHandler.hashPassword(c.SteamId, LoginHandler.encrypt(LoginHandler.raiseFirstLetter(v.mayorName)));
							PlayerInfo file = PlayerInfoFactory.createPlayerInfo(v.mayorName);
							if(hashCheck.equals(file.getPassword())) {
								deedCount++;
							}
						}
						if(deedCount >= settlementsPerSteamID) {
							c.getCommunicator().sendAlertServerMessage("Sorry, only " + String.valueOf(settlementsPerSteamID) + " villages per your SteamID '" + c.SteamId +"'");
							return null;
						} 						
					}					
				}
				return method.invoke(object, args);
			}
		});
		
		HookManager.getInstance().registerHook("com.wurmonline.server.questions.VillageFoundationQuestion", "setSize", "()V", new InvocationHandler() {
			@Override
			public Object invoke(Object object, Method method, Object[] args) throws Throwable {
				VillageFoundationQuestion vq = (VillageFoundationQuestion) object;
				Creature c = vq.getResponder();
				int diameterX = (vq.selectedWest + vq.selectedEast + 1);
				if(maxDeedX > -1 && diameterX > maxDeedX && c.getPower() < powerOverride) {
					vq.selectedEast = (maxDeedX-1)/2;
					vq.selectedWest = (maxDeedX-1)/2;
					c.getCommunicator().sendAlertServerMessage("Max total east/west size is: " +maxDeedX);
					c.getCommunicator().sendNormalServerMessage("Setting east and west sizes to: " +(maxDeedX/2));
				}
				int diameterY = (vq.selectedNorth + vq.selectedSouth + 1);
				if(maxDeedY > -1 && diameterY > maxDeedY && c.getPower() < powerOverride) {
					vq.selectedNorth = (maxDeedY-1)/2;
					vq.selectedSouth = (maxDeedY-1)/2;
					c.getCommunicator().sendAlertServerMessage("Max total north/south size is: " +maxDeedY);
					c.getCommunicator().sendNormalServerMessage("Setting north and south sizes to: " +(maxDeedY/2));
				}
				if(maxDeedTiles > -1 && (diameterX * diameterY) > maxDeedTiles && c.getPower() < powerOverride) {
					c.getCommunicator().sendAlertServerMessage("Max total tiles is: " +maxDeedTiles);
					int maxEW = ((maxDeedTiles/2)-1 < maxDeedX) ? ((maxDeedTiles/2)-1)/2 : (maxDeedX-1)/2;
					int maxNS = ((maxDeedTiles/2)-1 < maxDeedY) ? ((maxDeedTiles/2)-1)/2 : (maxDeedY-1)/2;
					vq.selectedEast = vq.selectedWest = maxEW;
					c.getCommunicator().sendNormalServerMessage("Setting east and west sizes to: " + maxEW);
					vq.selectedNorth = vq.selectedSouth = maxNS;
					c.getCommunicator().sendNormalServerMessage("Setting north and south sizes to: " + maxNS);
				}
				return method.invoke(object,  args);
			}
		});
	}
}
