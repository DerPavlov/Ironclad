package at.pavlov.ironclad.utils;

import java.util.Comparator;

import org.bukkit.block.BlockFace;

import at.pavlov.ironclad.craft.CraftDesign;

public class DesignComparator implements Comparator<CraftDesign>
{

	@Override
	public int compare(CraftDesign design1, CraftDesign design2)
	{
		int amount1 = getCannonBlockAmount(design1);
		int amount2 = getCannonBlockAmount(design2);
		
		return amount2 - amount1;
	}
	
	private Integer getCannonBlockAmount(CraftDesign design)
	{
		if (design == null) return 0;
		//if the design is invalid something goes wrong, message the user
		if (design.getAllCraftBlocks(BlockFace.NORTH) == null)
		{
			System.out.println("[Ironclad] invalid craft design for " + design.getDesignName());
			return 0;
		}
		
		return design.getAllCraftBlocks(BlockFace.NORTH).size();
	}

}
