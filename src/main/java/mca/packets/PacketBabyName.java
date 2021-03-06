package mca.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import mca.ai.AIProcreate;
import mca.core.MCA;
import mca.core.minecraft.ModAchievements;
import mca.core.minecraft.ModItems;
import mca.data.NBTPlayerData;
import mca.entity.EntityHuman;
import mca.items.ItemBaby;
import mca.util.TutorialManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import radixcore.network.ByteBufIO;
import radixcore.packets.AbstractPacket;
import radixcore.util.RadixLogic;

public class PacketBabyName extends AbstractPacket implements IMessage, IMessageHandler<PacketBabyName, IMessage>
{
	private String babyName;
	private int slot;
	
	public PacketBabyName()
	{
	}

	public PacketBabyName(String babyName, int slot)
	{
		this.babyName = babyName;
		this.slot = slot;
	}

	@Override
	public void fromBytes(ByteBuf byteBuf)
	{
		babyName = (String) ByteBufIO.readObject(byteBuf);
		slot = byteBuf.readInt();
	}

	@Override
	public void toBytes(ByteBuf byteBuf)
	{
		ByteBufIO.writeObject(byteBuf, babyName);
		byteBuf.writeInt(slot);
	}

	@Override
	public IMessage onMessage(PacketBabyName packet, MessageContext context)
	{
		EntityPlayer senderPlayer = this.getPlayer(context);
		ItemStack stack = packet.slot == -1 ? null : senderPlayer.inventory.getStackInSlot(packet.slot); //To avoid index out of bounds.
		NBTPlayerData data = MCA.getPlayerData(senderPlayer);
		EntityHuman playerSpouse = MCA.getHumanByPermanentId(data.getSpousePermanentId());
		
		//Player has the baby.
		if (stack != null && stack.getItem() instanceof ItemBaby)
		{
			NBTTagCompound nbt = stack.getTagCompound();
			nbt.setString("name", packet.babyName);
		}
		
		//Player's spouse will have the baby if stack is null.
		else if (stack == null)
		{
			if (playerSpouse != null)
			{
				int babySlot = playerSpouse.getInventory().getFirstSlotContainingItem(ModItems.babyBoy);
				babySlot = babySlot == -1 ? playerSpouse.getInventory().getFirstSlotContainingItem(ModItems.babyGirl) : babySlot;
				
				if (babySlot != -1)
				{
					playerSpouse.getInventory().getStackInSlot(babySlot).getTagCompound().setString("name", packet.babyName);
				}
			}
		}
		
		//Random chance for twins.
		if (RadixLogic.getBooleanWithProbability(MCA.getConfig().chanceToHaveTwins))
		{
			if (playerSpouse != null)
			{
				final AIProcreate procreateAI = playerSpouse.getAI(AIProcreate.class);
				
				if (!procreateAI.getHasHadTwins())
				{
					playerSpouse.getAI(AIProcreate.class).setIsProcreating(true);
					procreateAI.setHasHadTwins(true);
					senderPlayer.triggerAchievement(ModAchievements.twins);
					
					TutorialManager.sendMessageToPlayer(senderPlayer, "Congratulations! You've just had twins!", "Your spouse can only have twins once.");
				}
			}
		}
		
		return null;
	}
}
