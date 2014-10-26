package cubex2.mods.chesttransporter;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

import java.util.List;

public class ItemChestTransporter extends Item
{
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    protected ItemChestTransporter()
    {
        super();
        setUnlocalizedName("chesttransporter");
        setMaxStackSize(1);
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(CreativeTabs.tabTools);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void OnPlayerInteract(PlayerInteractEvent event)
    {
        if (event.action == Action.RIGHT_CLICK_BLOCK)
        {
            ItemStack stack = event.entityPlayer.getCurrentEquippedItem();
            if (stack == null || stack.getItem() != this)
                return;
            EntityPlayer player = event.entityPlayer;

            World world = event.entityPlayer.worldObj;

            int x = event.x;
            int y = event.y;
            int z = event.z;
            int face = event.face;

            if (stack.getItemDamage() == 0 && isChestAt(world, x, y, z))
            {
                TileEntity tile = world.getTileEntity(x, y, z);
                IInventory chest = (IInventory) tile;
                if (chest != null)
                {
                    Block chestBlock = world.getBlock(x, y, z);
                    int metadata = world.getBlockMetadata(x, y, z);
                    int newDamage = getNewDamageFromChest(chestBlock, metadata);
                    if (newDamage == 0)
                        return;
                    stack.setItemDamage(newDamage);
                    moveItemsIntoStack(chest, stack);
                    ChestRegistry.getChest(chestBlock, metadata).preRemoveChest(stack, tile);
                    world.setBlockToAir(x, y, z);
                    world.playSoundEffect(x + 0.5F, y + 0.5F, z + 0.5F, chestBlock.stepSound.getBreakSound(), (chestBlock.stepSound.getVolume() + 1.0F) / 2.0F, chestBlock.stepSound.getPitch() * 0.5F);
                }
            } else if (stack.getItemDamage() != 0)
            {
                ItemStack chestStack = getStackFromDamage(stack.getItemDamage());
                chestStack.tryPlaceItemIntoWorld(player, world, x, y, z, face, 0.0f, 0.0f, 0.0f);

                int[] chestCoords = getChestCoords(world, x, y, z, face);
                if (chestCoords != null)
                {
                    int x1 = chestCoords[0];
                    int y1 = chestCoords[1];
                    int z1 = chestCoords[2];

                    Block block = world.getBlock(x1, y1, z1);
                    int meta = world.getBlockMetadata(x1, y1, z1);

                    TileEntity tile = world.getTileEntity(x1, y1, z1);
                    IInventory chest = (IInventory) tile;
                    moveItemsIntoChest(stack, chest);

                    ChestRegistry.getChest(block, meta).preDestroyTransporter(stack, tile);

                    if (!player.capabilities.isCreativeMode)
                    {
                        player.renderBrokenItemStack(stack);
                        stack.stackSize--;
                    } else
                    {
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(ChestTransporter.chestTransporter));
                    }
                }
            }
        }
    }

    private int[] getChestCoords(World world, int x, int y, int z, int facing)
    {
        Block block = world.getBlock(x, y, z);

        if (block == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1)
        {
            // do nothing
        } else if (block != Blocks.vine && block != Blocks.tallgrass && block != Blocks.deadbush && (block == null || !block.isReplaceable(world, x, y, z)))
        {
            if (facing == 0)
            {
                --y;
            }

            if (facing == 1)
            {
                ++y;
            }

            if (facing == 2)
            {
                --z;
            }

            if (facing == 3)
            {
                ++z;
            }

            if (facing == 4)
            {
                --x;
            }

            if (facing == 5)
            {
                ++x;
            }
        }

        return new int[]{x, y, z};
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean par5)
    {
        if (stack.getItemDamage() != 0 && entity instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.capabilities.isCreativeMode)
                return;
            if (player.getActivePotionEffect(Potion.moveSlowdown) == null || player.getActivePotionEffect(Potion.moveSlowdown).getDuration() < 20)
            {
                player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 20 * 3, 2));
            }
            if (player.getActivePotionEffect(Potion.digSlowdown) == null || player.getActivePotionEffect(Potion.digSlowdown).getDuration() < 20)
            {
                player.addPotionEffect(new PotionEffect(Potion.digSlowdown.getId(), 20 * 3, 3));
            }
            if (player.getActivePotionEffect(Potion.jump) == null || player.getActivePotionEffect(Potion.jump).getDuration() < 20)
            {
                player.addPotionEffect(new PotionEffect(Potion.jump.getId(), 20 * 3, -2));
            }
            if (player.getActivePotionEffect(Potion.hunger) == null || player.getActivePotionEffect(Potion.hunger).getDuration() < 20)
            {
                player.addPotionEffect(new PotionEffect(Potion.hunger.getId(), 20 * 3, 0));
            }
        }

    }


    @SubscribeEvent
    public void mincartInteract(MinecartInteractEvent event)
    {
        ItemStack stack = event.player.getCurrentEquippedItem();
        EntityMinecart minecart = event.minecart;
        if (stack == null || stack.getItem() != this || stack.getItemDamage() != getNewDamageFromChest(Blocks.chest, 0) && stack.getItemDamage() != 0 || minecart == null)
            return;

        if (stack.getItemDamage() == 0 && minecart instanceof EntityMinecartChest)
        {
            moveItemsIntoStack((EntityMinecartChest) minecart, stack);
            stack.setItemDamage(1);
            minecart.worldObj.playSoundEffect((float) minecart.posX + 0.5F, (float) minecart.posY + 0.5F, (float) minecart.posZ + 0.5F, Blocks.chest.stepSound.getBreakSound(), (Blocks.chest.stepSound.getVolume() + 1.0F) / 2.0F, Blocks.chest.stepSound.getPitch() * 0.5F);
            if (!event.player.worldObj.isRemote)
            {
                EntityMinecartEmpty newMinecart = new EntityMinecartEmpty(minecart.worldObj);
                newMinecart.setPosition(minecart.posX, minecart.posY, minecart.posZ);
                newMinecart.setAngles(minecart.rotationPitch, minecart.rotationYaw);
                event.player.worldObj.spawnEntityInWorld(newMinecart);
                minecart.setDead();
            }
        } else if (stack.getItemDamage() == 1 && minecart instanceof EntityMinecartEmpty && minecart.riddenByEntity == null)
        {
            EntityMinecartChest newMinecart = new EntityMinecartChest(minecart.worldObj);
            if (!event.player.worldObj.isRemote)
            {
                newMinecart.setPosition(minecart.posX, minecart.posY, minecart.posZ);
                newMinecart.setAngles(minecart.rotationPitch, minecart.rotationYaw);
                minecart.worldObj.spawnEntityInWorld(newMinecart);
                minecart.setDead();
            }
            moveItemsIntoChest(stack, newMinecart);
            minecart.worldObj.playSoundEffect((float) minecart.posX + 0.5F, (float) minecart.posY + 0.5F, (float) minecart.posZ + 0.5F, Blocks.chest.stepSound.getBreakSound(), (Blocks.chest.stepSound.getVolume() + 1.0F) / 2.0F, Blocks.chest.stepSound.getPitch() * 0.8F);
            if (!event.player.capabilities.isCreativeMode)
            {
                event.player.renderBrokenItemStack(stack);
                event.player.inventory.setInventorySlotContents(event.player.inventory.currentItem, null);
            } else
            {
                event.player.inventory.setInventorySlotContents(event.player.inventory.currentItem, new ItemStack(ChestTransporter.chestTransporter));
            }
        }
        event.setCanceled(true);
    }

    @Override
    public boolean getShareTag()
    {
        return true;
    }

    @Override
    public IIcon getIconFromDamage(int i)
    {
        return icons[0];
    }

    @Override
    public IIcon getIconIndex(ItemStack stack)
    {
        if (stack.getItemDamage() == 0) return icons[0];
        return icons[ChestRegistry.dvToChest.get(stack.getItemDamage()).getIconIndex(stack)];
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass)
    {
        return getIconIndex(stack);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean flag)
    {
        if (stack.getItemDamage() != 0)
        {
            int numItems = 0;
            NBTTagList nbtList = stack.stackTagCompound.getTagList("Items", 10);

            for (int i = 0; i < nbtList.tagCount(); ++i)
            {
                NBTTagCompound nbtTagCompound = nbtList.getCompoundTagAt(i);
                NBTBase nbt = nbtTagCompound.getTag("Slot");
                int j = -1;
                if (nbt instanceof NBTTagByte)
                {
                    j = nbtTagCompound.getByte("Slot") & 255;
                } else
                {
                    j = nbtTagCompound.getShort("Slot");
                }

                if (j >= 0)
                {
                    ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbtTagCompound);
                    if (itemstack != null)
                    {
                        numItems += itemstack.stackSize;
                    }
                }
            }

            list.add("Contains " + numItems + " items");
        }
    }

    private boolean isChestAt(World world, int x, int y, int z)
    {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        return ChestRegistry.isChest(block, meta);
    }

    private int getNewDamageFromChest(Block block, int metadata)
    {
        TransportableChest chest = ChestRegistry.getChest(block, metadata);
        return chest != null ? chest.getTransporterDV() : 0;
    }

    private ItemStack getStackFromDamage(int damage)
    {
        return ChestRegistry.dvToChest.containsKey(damage) ? ChestRegistry.dvToChest.get(damage).createChestStack() : null;
    }

    private void moveItemsIntoStack(IInventory chest, ItemStack stack)
    {
        if (stack.stackTagCompound == null)
        {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagList nbtList = new NBTTagList();

        for (int i = 0; i < chest.getSizeInventory(); ++i)
        {
            if (chest.getStackInSlot(i) != null)
            {
                NBTTagCompound nbtTabCompound2 = new NBTTagCompound();
                nbtTabCompound2.setShort("Slot", (short) i);
                chest.getStackInSlot(i).copy().writeToNBT(nbtTabCompound2);
                chest.setInventorySlotContents(i, null);
                nbtList.appendTag(nbtTabCompound2);
            }
        }
        stack.stackTagCompound.setTag("Items", nbtList);
    }

    private void moveItemsIntoChest(ItemStack stack, IInventory chest)
    {
        NBTTagList nbtList = stack.stackTagCompound.getTagList("Items", 10);

        for (int i = 0; i < nbtList.tagCount(); ++i)
        {
            NBTTagCompound nbtTagCompound = nbtList.getCompoundTagAt(i);
            NBTBase nbt = nbtTagCompound.getTag("Slot");
            int j;
            if (nbt instanceof NBTTagByte)
            {
                j = nbtTagCompound.getByte("Slot") & 255;
            } else
            {
                j = nbtTagCompound.getShort("Slot");
            }

            if (j >= 0 && j < chest.getSizeInventory())
            {
                chest.setInventorySlotContents(j, ItemStack.loadItemStackFromNBT(nbtTagCompound).copy());
            }
        }

        stack.stackTagCompound.removeTag("Items");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister)
    {
        icons = new IIcon[18];
        for (int i = 0; i < icons.length; i++)
        {
            icons[i] = iconRegister.registerIcon("chesttransporter:" + i);
        }
    }
}
