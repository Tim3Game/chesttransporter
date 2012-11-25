package cubex2.mods.chesttransporter;

import java.util.Arrays;
import java.util.logging.Level;

import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraftforge.common.Configuration;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

@Mod(modid = "ChestTransporter", name = "Chest Transporter", version = "1.1.2")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class ChestTransporter {
	@Instance("ChestTransporter")
	public static ChestTransporter instance;
	@SidedProxy(clientSide = "cubex2.mods.chesttransporter.client.ClientProxy", serverSide = "cubex2.mods.chesttransporter.CommonProxy")
	public static CommonProxy proxy;
	public static ItemChestTransporter chestTransporter;
	private int itemId;
	public static Block ironChestBlock;

	@PreInit
	public void preInit(FMLPreInitializationEvent event) {
		Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());
		try {
			cfg.load();
			itemId = cfg.getItem("id", 17750).getInt(17750);
		} catch (Exception e) {
			FMLLog.log(Level.SEVERE, e, "ChestTransporter has a problem loading it's configuration");
		} finally {
			cfg.save();
		}
		ModMetadata md = event.getModMetadata();
		md.autogenerated = false;
		md.description = "Adds an item that allows you to transport chests.";
		md.authorList = Arrays.asList("CubeX2");
		md.url = "http://www.minecraftforum.net/topic/506109-";
	}

	@Init
	public void load(FMLInitializationEvent evt) {
		chestTransporter = new ItemChestTransporter(itemId);
		LanguageRegistry.instance().addStringLocalization("item.chesttransporter.name", "en_US", "Chest Transporter");
		GameRegistry.addRecipe(new ItemStack(chestTransporter), "S S", "SSS", " S ", Character.valueOf('S'), Item.stick);
		proxy.registerRenderInformation();
	}
	
	@PostInit
	public void postInit(FMLPostInitializationEvent evt)
	{
		try {
			Class clazz = Class.forName("cpw.mods.ironchest.IronChest");
			Object instance = clazz.getField("instance").get(null);
			ironChestBlock = (Block)clazz.getField("ironChestBlock").get(null);
		} catch (ClassNotFoundException e) {
			// IronChest is not installed
		} catch (Exception e) {
			
		}
	}

}
