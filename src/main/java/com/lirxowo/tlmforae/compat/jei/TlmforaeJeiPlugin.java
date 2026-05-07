package com.lirxowo.tlmforae.compat.jei;

import com.lirxowo.tlmforae.Tlmforae;
import com.lirxowo.tlmforae.client.gui.AEAutocraftConfigScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class TlmforaeJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Tlmforae.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(AEAutocraftConfigScreen.class, new AEAutocraftGhostIngredientHandler());
    }

    private static class AEAutocraftGhostIngredientHandler implements IGhostIngredientHandler<AEAutocraftConfigScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(AEAutocraftConfigScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            return ingredient.getItemStack()
                    .<List<Target<I>>>map(stack -> {
                        List<Target<I>> targets = new ArrayList<>();
                        for (AEAutocraftConfigScreen.TargetSlotArea area : gui.getVisibleTargetSlotAreas()) {
                            targets.add(new Target<>() {
                                @Override
                                public Rect2i getArea() {
                                    return new Rect2i(area.x(), area.y(), area.width(), area.height());
                                }

                                @Override
                                public void accept(I ignored) {
                                    gui.setTargetStack(area.index(), stack);
                                }
                            });
                        }
                        AEAutocraftConfigScreen.TargetSlotArea appendArea = gui.getAppendTargetArea();
                        targets.add(new Target<>() {
                            @Override
                            public Rect2i getArea() {
                                return new Rect2i(appendArea.x(), appendArea.y(), appendArea.width(), appendArea.height());
                            }

                            @Override
                            public void accept(I ignored) {
                                gui.addTargetStack(stack);
                            }
                        });
                        return targets;
                    })
                    .orElseGet(List::of);
        }

        @Override
        public void onComplete() {
        }
    }
}
