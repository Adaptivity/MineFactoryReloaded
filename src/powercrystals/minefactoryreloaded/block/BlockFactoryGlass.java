package powercrystals.minefactoryreloaded.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemDye;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cofh.util.position.BlockPosition;
import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;
import powercrystals.minefactoryreloaded.api.rednet.connectivity.IRedNetDecorative;
import powercrystals.minefactoryreloaded.gui.MFRCreativeTab;
import powercrystals.minefactoryreloaded.render.IconOverlay;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockFactoryGlass extends BlockGlass implements IRedNetDecorative
{
	static IIcon _texture;

	public BlockFactoryGlass()
	{
		super(Material.glass, false);
		this.setCreativeTab(CreativeTabs.tabDecorations);
		setBlockName("mfr.stainedglass.block");
		setHardness(0.3F);
		setStepSound(soundTypeGlass);
		setCreativeTab(MFRCreativeTab.tab);
	}

	@Override
	public int damageDropped(int par1)
	{
		return par1;
	}

	@Override
	public int getRenderBlockPass()
	{
		return 1;
	}

	@Override
	public boolean recolourBlock(World world, int x, int y, int z, ForgeDirection side, int colour)
	{
		int meta = world.getBlockMetadata(x, y, z);
		if (meta != colour)
		{
			return world.setBlockMetadataWithNotify(x, y, z, colour, 3);
		}
		return false;
	}

	@Override
	public int getRenderColor(int meta)
	{
		return ItemDye.field_150922_c[15 - Math.min(Math.max(meta, 0), 15)];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister ir)
	{
		_texture = ir.registerIcon("minefactoryreloaded:tile.mfr.stainedglass");
	}

	@Override
	public IIcon getIcon(int side, int meta)
	{
		return new IconOverlay(_texture, 8, 8, meta > 15 ? 6 : 7, 7);
	}

	public IIcon getBlockOverlayTexture()
	{
		return new IconOverlay(_texture, 8, 8, 0, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side)
	{
		if (world.getBlock(x, y, z).getMaterial() == Material.glass && isBlockFullCube(world, x, y, z))
			return false;
		return super.shouldSideBeRendered(world, x, y, z, side);
	}

	public boolean isBlockFullCube(IBlockAccess world, int x, int y, int z)
	{
		Block block = world.getBlock(x, y, z);
		if (block == null)
			return false;
		block.setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(block.getBlockBoundsMinX(),
				block.getBlockBoundsMinY(), block.getBlockBoundsMinZ(),
				block.getBlockBoundsMaxX(), block.getBlockBoundsMaxY(),
				block.getBlockBoundsMaxZ()).getAverageEdgeLength() >= 1.0D;
	}

	public IIcon getBlockOverlayTexture(IBlockAccess world, int x, int y, int z, int side)
	{
		BlockPosition bp;
		boolean[] sides = new boolean[8];
		if (side <= 1)
		{
			bp = new BlockPosition(x, y, z, ForgeDirection.NORTH);
			bp.moveRight(1);
			sides[0] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveBackwards(1);
			sides[4] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveLeft(1);
			sides[1] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveLeft(1);
			sides[5] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveForwards(1);
			sides[3] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveForwards(1);
			sides[6] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveRight(1);
			sides[2] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveRight(1);
			sides[7] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
		}
		else
		{
			bp = new BlockPosition(x, y, z, ForgeDirection.VALID_DIRECTIONS[side]);
			bp.moveRight(1);
			sides[0] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveDown(1);
			sides[4] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveLeft(1);
			sides[1] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveLeft(1);
			sides[5] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveUp(1);
			sides[3] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveUp(1);
			sides[6] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveRight(1);
			sides[2] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
			bp.moveRight(1);
			sides[7] = world.getBlock(bp.x,bp.y,bp.z).equals(this);
		}
		return new IconOverlay(_texture, 8, 8, sides);
	}

	@Override
	public int getRenderType()
	{
		return MineFactoryReloadedCore.renderIdFactoryGlass;
	}
}
