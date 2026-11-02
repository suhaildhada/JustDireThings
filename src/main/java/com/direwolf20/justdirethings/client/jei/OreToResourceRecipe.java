package com.direwolf20.justdirethings.client.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public record OreToResourceRecipe(ItemStack oreBlock, ItemStack output) {
    public OreToResourceRecipe(Block oreBlock, ItemStack output) {
        this(new ItemStack(oreBlock), output);
    }
}
