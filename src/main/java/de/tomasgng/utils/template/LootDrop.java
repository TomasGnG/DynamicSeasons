package de.tomasgng.utils.template;

import lombok.Getter;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class LootDrop {

    @Getter
    private EntityType entity;
    @Getter
    private Map<ItemStack, Double> itemStacks;

    public LootDrop(EntityType entity, Map<ItemStack, Double> itemStacks) {
        this.entity = entity;
        this.itemStacks = itemStacks;
    }
}
