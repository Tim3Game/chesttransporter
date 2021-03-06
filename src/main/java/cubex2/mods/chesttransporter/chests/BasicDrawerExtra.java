package cubex2.mods.chesttransporter.chests;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;

public class BasicDrawerExtra extends TransportableChestImpl
{
    private final String[] variants;

    public BasicDrawerExtra(Block chestBlock, int chestMeta, String name, String[] variants)
    {
        super(chestBlock, chestMeta, name);
        this.variants = variants;
    }

    @Override
    public boolean copyTileEntity()
    {
        return true;
    }

    public NBTTagCompound modifyTileCompound(NBTTagCompound nbt, World world, BlockPos pos, EntityPlayer player, ItemStack transporter)
    {
        nbt.setByte("Dir", (byte) player.getHorizontalFacing().getOpposite().ordinal());

        return nbt;
    }

    @Override
    public Collection<ResourceLocation> getChestModels()
    {
        List<ResourceLocation> models = Lists.newArrayList();

        for (String variant : variants)
        {
            models.add(locationFromName(name + "_" + variant));
        }

        return models;
    }

    @Override
    public ResourceLocation getChestModel(ItemStack stack)
    {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("ChestTile"))
        {
            NBTTagCompound nbt = stack.getTagCompound().getCompoundTag("ChestTile");
            String mat = nbt.getString("Mat");
            if (mat == null || mat.length() == 0)
                mat = "oak";
            return locationFromName(name + "_" + mat.replace(":","_"));
        }
        return locationFromName(name + "_oak");
    }


}
