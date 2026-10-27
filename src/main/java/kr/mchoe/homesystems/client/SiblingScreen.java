package kr.mchoe.homesystems.client;

import kr.mchoe.homesystems.SiblingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SiblingScreen extends AbstractContainerScreen<SiblingMenu> {
    public SiblingScreen(SiblingMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=260;imageHeight=234;}
    @Override protected void init(){
        super.init();
        int count=menu.options().size();
        for(int i=0;i<count;i++){
            var option=menu.options().get(i);
            addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(option.name().getString(),104)),button->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,option.slot());})
                .bounds(leftPos+12+(i%2)*120,topPos+42+(i/2)*20,116,20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),button->onClose()).bounds(leftPos+70,topPos+210,120,20).build());
    }
    @Override protected void containerTick(){super.containerTick();int index=0;for(var widget:renderables)if(widget instanceof Button button&&index<menu.options().size()){var option=menu.options().get(index++);button.active=menu.selected()!=option.slot();}}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){graphics.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff182a38);graphics.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+28,0xff304c60);}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){graphics.drawString(font,title,12,10,0xffffff,false);graphics.drawString(font,Component.translatable("screen.homesystems.sibling_hint"),12,31,0xb9cedb,false);}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);for(int i=0;i<menu.options().size();i++)if(x>=leftPos+12+(i%2)*120&&x<leftPos+128+(i%2)*120&&y>=topPos+42+(i/2)*20&&y<topPos+62+(i/2)*20)graphics.renderTooltip(font,menu.options().get(i).name(),x,y);}
}
