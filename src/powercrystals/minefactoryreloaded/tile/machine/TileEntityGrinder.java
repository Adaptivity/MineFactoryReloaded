package powercrystals.minefactoryreloaded.tile.machine;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import powercrystals.minefactoryreloaded.MFRRegistry;
import powercrystals.minefactoryreloaded.api.IFactoryGrindable;
import powercrystals.minefactoryreloaded.api.MobDrop;
import powercrystals.minefactoryreloaded.core.GrindingDamage;
import powercrystals.minefactoryreloaded.core.HarvestAreaManager;
import powercrystals.minefactoryreloaded.core.IHarvestAreaContainer;
import powercrystals.minefactoryreloaded.core.ITankContainerBucketable;
import powercrystals.minefactoryreloaded.gui.client.GuiFactoryInventory;
import powercrystals.minefactoryreloaded.gui.client.GuiFactoryPowered;
import powercrystals.minefactoryreloaded.gui.container.ContainerFactoryPowered;
import powercrystals.minefactoryreloaded.setup.Machine;
import powercrystals.minefactoryreloaded.tile.base.TileEntityFactoryPowered;
import powercrystals.minefactoryreloaded.world.GrindingWorld;
import powercrystals.minefactoryreloaded.world.GrindingWorldServer;
import powercrystals.minefactoryreloaded.world.IGrindingWorld;

public class TileEntityGrinder extends TileEntityFactoryPowered implements ITankContainerBucketable, IHarvestAreaContainer
{
	public static final int DAMAGE = 500000; 
	private static Field recentlyHit;
	
	static
	{
		String[] q = ObfuscationReflectionHelper.remapFieldNames("net.minecraft.entity.EntityLivingBase", "field_70718_bc", "recentlyHit");
		recentlyHit = ReflectionHelper.findField(EntityLivingBase.class, q);
	}
	
	protected HarvestAreaManager _areaManager;
	protected FluidTank _tank;
	protected Random _rand;
	protected IGrindingWorld _grindingWorld;
	protected GrindingDamage _damageSource;
	
	protected TileEntityGrinder(Machine machine)
	{
		super(machine);
		_areaManager = new HarvestAreaManager(this, 2, 2, 1);
		_tank = new FluidTank(4 * FluidContainerRegistry.BUCKET_VOLUME);
		_rand = new Random();
	}
	
	public TileEntityGrinder()
	{
		this(Machine.Grinder);
		_damageSource = new GrindingDamage();
	}
	
	@Override
	public String getGuiBackground()
	{
		return "grinder.png";
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public GuiFactoryInventory getGui(InventoryPlayer inventoryPlayer)
	{
		return new GuiFactoryPowered(getContainer(inventoryPlayer), this);
	}
	
	@Override
	public ContainerFactoryPowered getContainer(InventoryPlayer inventoryPlayer)
	{
		return new ContainerFactoryPowered(this, inventoryPlayer);
	}
	
	@Override
	public void setWorldObj(World world)
	{
		super.setWorldObj(world);
		if(_grindingWorld != null) _grindingWorld.clearReferences();
		if(this.worldObj instanceof WorldServer)
			_grindingWorld = new GrindingWorldServer((WorldServer)this.worldObj, this);
		else
			_grindingWorld = new GrindingWorld(this.worldObj, this);
	}
	
	public Random getRandom()
	{
		return _rand;
	}
	
	@Override
	protected boolean shouldPumpLiquid()
	{
		return true;
	}
	
	@Override
	public int getEnergyStoredMax()
	{
		return 32000;
	}
	
	@Override
	public int getWorkMax()
	{
		return 1;
	}
	
	@Override
	public int getIdleTicksMax()
	{
		return 200;
	}
	
	@Override
	public IFluidTank getTank()
	{
		return _tank;
	}
	
	@Override
	public HarvestAreaManager getHAM()
	{
		return _areaManager;
	}
	
	@Override
	public boolean activateMachine()
	{
		_grindingWorld.cleanReferences();
		List<?> entities = worldObj.getEntitiesWithinAABB(EntityLiving.class, _areaManager.getHarvestArea().toAxisAlignedBB());
		
		entityList: for(Object o : entities)
		{
			EntityLiving e = (EntityLiving)o;
			if(e instanceof EntityAgeable && ((EntityAgeable)e).getGrowingAge() < 0 || e.isEntityInvulnerable() || e.getHealth() <= 0)
			{
				continue;
			}
			boolean processMob = false;
			processEntity:
			{
				if(MFRRegistry.getGrindables().containsKey(e.getClass()))
				{
					IFactoryGrindable r = MFRRegistry.getGrindables().get(e.getClass());
					List<MobDrop> drops = r.grind(e.worldObj, e, getRandom());
					if(drops != null && drops.size() > 0 && WeightedRandom.getTotalWeight(drops) > 0)
					{
						ItemStack drop = ((MobDrop)WeightedRandom.getRandomItem(_rand, drops)).getStack();
						doDrop(drop);
					}
					if(r.processEntity(e))
					{
						processMob = true;
						if(e.getHealth() <= 0)
						{
							continue entityList;
						}
						break processEntity;
					}
				}
				for(Class<?> t : MFRRegistry.getGrinderBlacklist())
				{
					if(t.isInstance(e))
					{
						continue entityList;
					}
				}
				if(!_grindingWorld.addEntityForGrinding(e))
				{
					continue entityList;
				}
			}
			if(processMob && e.worldObj.getGameRules().getGameRuleBooleanValue("doMobLoot"))
			{
				try
				{
					e.worldObj.getGameRules().setOrCreateGameRule("doMobLoot", "false");
					damageEntity(e);
					if(e.getHealth() <= 0)
					{
						_tank.fill(FluidRegistry.getFluidStack("mobEssence", 100), true);
					}
				}
				finally
				{
					e.worldObj.getGameRules().setOrCreateGameRule("doMobLoot", "true");
					setIdleTicks(20);
				}
				return true;
			}
			damageEntity(e);
			if(e.getHealth() <= 0)
			{
				_tank.fill(FluidRegistry.getFluidStack("mobEssence", 100), true);
				setIdleTicks(20);
			}
			else
			{
				setIdleTicks(10);
			}
			return true;
		}
		setIdleTicks(getIdleTicksMax());
		return false;
	}
	
	protected void setRecentlyHit(EntityLiving entity, int t)
	{
		try
		{
			recentlyHit.set(entity, t);
		}
		catch(Throwable e)
		{
		}
	}
	
	protected void damageEntity(EntityLiving entity)
	{
		setRecentlyHit(entity, 100);
		entity.attackEntityFrom(_damageSource, DAMAGE);
	}
	
	@Override
	public int getSizeInventory()
	{
		return 0;
	}
	
	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		return 0;
	}
	
	@Override
	public boolean allowBucketDrain()
	{
		return true;
	}
	
	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return null;
	}
	
	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		return null;
	}
	
	@Override
	public IFluidTank getTank(ForgeDirection direction, FluidStack type)
	{
		return _tank;
	}
	
	@Override
	public boolean manageSolids()
	{
		return true;
	}
	
	@Override
	public boolean canRotate()
	{
		return true;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return false;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return false;
	}
}
