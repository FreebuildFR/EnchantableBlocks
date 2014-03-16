package com.github.jikoo.enchantedfurnace;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * Listener for all furnace-related events.
 * 
 * @author Jikoo
 */
public class FurnaceListener implements Listener {
	@EventHandler
	public void onFurnaceConsumeFuel(FurnaceBurnEvent e) {
		Furnace f = EnchantedFurnace.getInstance().getFurnace(e.getBlock());
		if (f == null) {
			return;
		}
		if (f.isPaused()) {
			e.setCancelled(true);
			f.resume();
			return;
		}
		if (f.getBurnModifier() > 0) {
			// + 1/5 fuel burn length per level unbreaking
			e.setBurnTime((int) ((1 + .2 * f.getBurnModifier()) * e.getBurnTime()));
		}
	}

	@EventHandler
	public void onItemSmelt(FurnaceSmeltEvent e) {
		Furnace f = EnchantedFurnace.getInstance().getFurnace(e.getBlock());
		if (f == null) {
			return;
		}

		org.bukkit.block.Furnace tile = f.getFurnaceTile();
		FurnaceInventory i = tile.getInventory();
		if (f.getFortune() > 0) {
			int extraResults = 0;
			for (int j = 0; j < f.getFortune(); j++) {
				// 1/3 chance per level fortune
				extraResults += (int) (1.50 * Math.random());
			}
			ItemStack newResult = i.getResult();
			if (newResult == null ) {
				Iterator<Recipe> ri = Bukkit.recipeIterator();
				Set<FurnaceRecipe> matches = new HashSet<FurnaceRecipe>();
				extraResults -= 1;
				while (ri.hasNext()) {
					Recipe r = ri.next();
					if (r instanceof FurnaceRecipe) {
						if (((FurnaceRecipe) r).getInput().getType() == i.getSmelting().getType()) {
							matches.add((FurnaceRecipe) r);
						}
					}
				}
				for (FurnaceRecipe r : matches) {
					newResult = new ItemStack(r.getResult());
					if (r.getInput().getData().equals(i.getSmelting().getData())) {
						break;
					}
				}
			}
			int newAmount = newResult.getAmount() + extraResults;
			newResult.setAmount(newAmount > 64 ? 64 : newAmount);
			i.setResult(newResult);
			tile.update(true);
		}

		if (i.getSmelting().getAmount() == 1 || (i.getResult() != null && i.getResult().getAmount() == 64)) {
			f.pause();
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (e.getItemInHand().getType() == Material.FURNACE) {
			EnchantedFurnace.getInstance().createFurnace(e.getBlock(), e.getItemInHand());
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getBlock().getType() != Material.FURNACE && e.getBlock().getType() != Material.BURNING_FURNACE) {
			return;
		}
		ItemStack is = EnchantedFurnace.getInstance().destroyFurnace(e.getBlock());
		if (is != null) {
			e.setCancelled(true);
			// TODO does furnace drop contents on set air?
			e.getBlock().setType(Material.AIR);
			if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
				e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), is);
			}
		}
	}

	@EventHandler
	public void onInventoryInteract(InventoryClickEvent e) {
		Inventory top = e.getView().getTopInventory();
		if (top instanceof FurnaceInventory) {
			Furnace f = EnchantedFurnace.getInstance().getFurnace(((org.bukkit.block.Furnace)top.getHolder()).getBlock());
			if (f != null && f.isPaused()) {
				updateSilk(f);
			}
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent e) {
		Inventory top = e.getView().getTopInventory();
		if (top instanceof FurnaceInventory) {
			Furnace f = EnchantedFurnace.getInstance().getFurnace(((org.bukkit.block.Furnace)top.getHolder()).getBlock());
			if (f != null && f.isPaused()) {
				updateSilk(f);
			}
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryMoveItemEvent e) {
		Inventory top = e.getDestination().getType() == InventoryType.HOPPER ? e.getSource() : e.getDestination();
		if (top.getType() == InventoryType.FURNACE) {
			Furnace f = EnchantedFurnace.getInstance().getFurnace(((org.bukkit.block.Furnace)top.getHolder()).getBlock());
			if (f != null && f.isPaused()) {
				updateSilk(f);
			}
		}
	}

	private void updateSilk(final Furnace f) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(EnchantedFurnace.getInstance(),
				new Runnable() {
					@Override
					public void run() {
						f.resume();
					}
				});
	}
}