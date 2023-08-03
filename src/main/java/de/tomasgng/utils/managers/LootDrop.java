package de.tomasgng.utils.managers;

import lombok.Getter;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class LootDrop {

    @Getter
    private EntityType entity;
    @Getter
    private Map<ItemStack, Integer> itemStacks;

    public LootDrop(EntityType entity, Map<ItemStack, Integer> itemStacks) {
        this.entity = entity;
        this.itemStacks = itemStacks;
    }


}
