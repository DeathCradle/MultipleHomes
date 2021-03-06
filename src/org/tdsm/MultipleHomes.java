package org.tdsm;
import java.io.File;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.tdsm.commands.CommandParser;
import org.tdsm.homes.Home;
import org.tdsm.homes.HomeManager;
import org.tdsm.homes.imports.Imports;


public class MultipleHomes extends JavaPlugin {

	private final mhPlayerListener playerListener = new mhPlayerListener(this);
	public CommandParser cmdParser;
	public Properties properties;
	public QueueT QueueThread;
	
	public int TPDelay = 0;

	public static final Logger log = Logger.getLogger("Minecraft");
	public static final String PluginFolder = "plugins/MultipleHomes/";
	public final String WorldFolder = PluginFolder + "WorldData/";

	public HashMap<String, List<Home>> WorldPlayerData;
	public HashMap<String, Timestamp> WorldPlayerDelay;
		
	@Override
	public void onDisable() {
		// TODO Auto-generated method stub
		WritetoConsole("Disabled.");
	}
	
	@Override
	public void onLoad() {
		// TODO Auto-generated method stub
		WritetoConsole("Initialzing...");
		
		SetupDirectories();
		properties = new Properties(PluginFolder + "multiplehomes.properties"); //Create/Load Properties
		
	}

	@Override
	public void onEnable() {
		// TODO Auto-generated method stub		
		LoadData(); //This uses Worlds.
		
		System.out.println("Loaded " + String.valueOf(WorldPlayerData.size()) + " Home(s)");
		
		TPDelay = properties.GetTeleportDelay();
		if(TPDelay > 0) {
			System.out.println("Running with Teleport Delay: " + String.valueOf(TPDelay));
		}
		
		HashMap<String, List<Home>> MergeData = new HashMap<String, List<Home>>();
		
		boolean Merge = false;
		File oldHomes = new File(PluginFolder + "Homes");
		if(oldHomes.exists() && oldHomes.isDirectory()) {
			WritetoConsole("Old Home Directory Found...Attempting Convert.");
			HashMap<String, List<Home>> Lists = Imports.ImportHomes(this);
			if(Lists != null) {
				for(String playerName : Lists.keySet()) {
					if(WorldPlayerData.containsKey(playerName)) {
						for(String RealPlayer : WorldPlayerData.keySet()) {
							for(Home importedHome : Lists.get(playerName)) {
								for(Home home : WorldPlayerData.get(RealPlayer)) {
									Home toSaveHome = importedHome;
									if((home.HomeNumber == importedHome.HomeNumber ||
											home.Name == importedHome.Name) && properties.GetConverterOverwrite()) {
										toSaveHome = home;
									}
									if(MergeData.containsKey(RealPlayer)) {
										MergeData.get(RealPlayer).add(toSaveHome);
									} else {
										List<Home> homes = new ArrayList<Home>();
										homes.add(toSaveHome);
										MergeData.put(RealPlayer, homes);
									}
								}
							}
						}
					} else {
						MergeData.put(playerName, Lists.get(playerName));
					}
				}
				WritetoConsole("Convert Was Successful, Saving Data.");
				Merge = true;
			} else {
				WritetoConsole("Convert Was UnSuccessful.");
			}
		}

		if(Merge) {
			boolean delete = true;
			this.WorldPlayerData = MergeData;
			for(String playerName : MergeData.keySet()) {
				if(!HomeManager.SavePlayerHomes(playerName, WorldPlayerData, null)) {
					WritetoConsole("Failed to save " + playerName +"'s New Data File");
					delete = false;
				}
			}
			if(delete) {
				try {
					//DeleteDirectoryAndContents(oldHomes);
					int i = 0;
					File renamed = new File(oldHomes.getAbsoluteFile() + "_" + String.valueOf(i));
					while(renamed.exists()) {
						i++;
						renamed = new File(oldHomes.getAbsoluteFile() + "_" + String.valueOf(i));
					}
					oldHomes.renameTo(renamed);
				} catch(Exception e) {
					
				}
			}
		}
		
		cmdParser = new CommandParser(properties);
		
		PluginManager pm = getServer().getPluginManager(); 
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Highest, this); //Hopefully it will over rule Essentials
		pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Normal, this); //Hopefully it will over rule Essentials
		
		WritetoConsole("Enabled.");
		
		QueueThread = new QueueT(this);
	}
	
	public void WritetoConsole(String Msg) {
		log.info("[MultipleHomes] " + Msg);
	}
	
	public void SetupDirectories() {
		String[] Folders = new String[] { PluginFolder, this.WorldFolder };
		for(String folder : Folders) {
			File worldDir = new File(folder);
			if(!worldDir.exists()) {
				if(!worldDir.mkdir()) {
					WritetoConsole("Issue Creating Folder (1): " + folder);
					if(!worldDir.mkdirs()) {
						WritetoConsole("Issue Creating Folder (2): " + folder);
					}
				}
			}
		}
	}
	
	public void LoadData() {
		WorldPlayerData = new HashMap<String, List<Home>>();
		
		File WorldDataFolder = new File(WorldFolder);
	    File[] PlayerHomeFileList = WorldDataFolder.listFiles();

	    for (int i = 0; i < PlayerHomeFileList.length; i++) {
	    	if (PlayerHomeFileList[i].isFile()) {
	    		if(PlayerHomeFileList[i].getName().toLowerCase().endsWith(".mhf")) {
	    			String PlayerName = PlayerHomeFileList[i].getName().substring(0, 
	    				  				PlayerHomeFileList[i].getName().length()-4).trim();

	    			//System.out.println("Loading: " + PlayerHomeFileList[i].getAbsolutePath());

	    			List<Home> playerHomes = HomeManager.LoadPlayerHomes(
	    				  						PlayerHomeFileList[i].getAbsolutePath(),
	    				  						PlayerName,
	    				  						this.getServer());
	    	  
	    			if(playerHomes != null) {
	    				WorldPlayerData.put(PlayerName, playerHomes);
	    			} else {
	    				System.out.print("Error loading Player Data");
	    			}
	    		}
	    	} 
	    }
	}
	
	public static String ArrayToString(String[] Array, String Deliminator) {
		StringBuilder StringBuilder = new StringBuilder();
		for(String str : Array) {
			StringBuilder.append(Deliminator +  str);
		}
		String ReT = StringBuilder.toString().trim();
		if(ReT.length() > 0) {
			if(ReT.startsWith(Deliminator)) {
				ReT = ReT.substring(1, ReT.length());
			}
			return ReT.trim();
		}
		return null;
	}
	
	//public static void DeleteDirectoryAndContents(File Directory) {
	//	if(Directory.exists()) {
	//		File[] fileList = Directory.listFiles();
	//		for(int i = 0; i < fileList; i++) {
	//			if(fileList[i].isDirectory()) {
	//				DeleteDirectoryAndContents(fileList[i]);
	//			}
	//			else {
	//				fileList[i].delete();
	//			}
	//		}
	//	}
	//}

}
