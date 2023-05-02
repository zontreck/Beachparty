package net.satisfyu.beachparty.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.satisfyu.beachparty.BeachpartyIdentifier;
import net.satisfyu.beachparty.client.gui.handler.MiniFridgeGuiHandler;
import net.satisfyu.beachparty.client.screen.recipe.custom.MiniFridgeRecipeBook;

public class MiniFridgeGui extends AbstractRecipeBookGUIScreen<MiniFridgeGuiHandler> {

    private static final ResourceLocation BG = new BeachpartyIdentifier("textures/gui/freezer.png");

    public MiniFridgeGui(MiniFridgeGuiHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, new MiniFridgeRecipeBook(), BG);
    }

    @Override
    protected void init() {
        this.titleLabelX += 2;
        this.titleLabelY += -3;
        super.init();
    }

    protected void renderProgressArrow(PoseStack matrices) {
        final int progressX = this.menu.getShakeXProgress();
        this.blit(matrices, leftPos + 94, topPos + 45, 177, 26, progressX, 10);
        final int progressY = this.menu.getShakeYProgress();
        this.blit(matrices, leftPos + 96, topPos + 22 + 20 - progressY, 179, 2 + 20 - progressY, 15, progressY);
    }
}

