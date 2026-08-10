package cn.breezeth.ordertocook.screen;

import cn.breezeth.ordertocook.core.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TakeoutBoxScreen extends HandledScreen<TakeoutBoxScreenHandler> {
    private static final Identifier BG_TEXTURE=Identifier.of(ModConstants.MOD_ID,"textures/gui/takeout_box.png");
    private static final Identifier QUERY_NONE_TEXTURE=Identifier.of(ModConstants.MOD_ID,"textures/gui/takeout_query_none.png");
    private static final Identifier QUERY_MISSING_TEXTURE=Identifier.of(ModConstants.MOD_ID,"textures/gui/takeout_query_missing.png");
    private static final Identifier QUERY_READY_TEXTURE=Identifier.of(ModConstants.MOD_ID,"textures/gui/takeout_query_ready.png");
    private static final Identifier QUERY_HOVER_TEXTURE=Identifier.of(ModConstants.MOD_ID,"textures/gui/takeout_query_hover.png");
    private static final int ACTION_BUTTON_X=134,PACK_BUTTON_Y=23,PLATE_BUTTON_Y=45,ACTION_BUTTON_WIDTH=38,ACTION_BUTTON_HEIGHT=20;
    private static final int QUERY_BUTTON_X=156,QUERY_BUTTON_Y=4,QUERY_TEXTURE_SIZE=27,QUERY_BUTTON_SIZE=16;
    private ButtonWidget packButton,plateButton,queryButton;
    public TakeoutBoxScreen(TakeoutBoxScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);}
    @Override protected void init(){super.init();int px=(width-backgroundWidth)/2,py=(height-backgroundHeight)/2;packButton=addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ordertocook.countertop.pack"),b->clickButton(0)).dimensions(px+ACTION_BUTTON_X,py+PACK_BUTTON_Y,ACTION_BUTTON_WIDTH,ACTION_BUTTON_HEIGHT).build());plateButton=addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ordertocook.countertop.plate"),b->clickButton(1)).dimensions(px+ACTION_BUTTON_X,py+PLATE_BUTTON_Y,ACTION_BUTTON_WIDTH,ACTION_BUTTON_HEIGHT).build());queryButton=addDrawableChild(ButtonWidget.builder(Text.empty(),b->clickButton(2)).dimensions(px+QUERY_BUTTON_X,py+QUERY_BUTTON_Y,QUERY_BUTTON_SIZE,QUERY_BUTTON_SIZE).build());queryButton.visible=false;queryButton.setAlpha(0.0F);}
    private void clickButton(int id){if(client!=null&&client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,id);}
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY){RenderSystem.setShader(GameRenderer::getPositionTexProgram);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShaderTexture(0,BG_TEXTURE);context.drawTexture(BG_TEXTURE,x,y,0,0,backgroundWidth,backgroundHeight);}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){renderBackground(context,mouseX,mouseY,delta);if(queryButton!=null&&client!=null&&client.world!=null)queryButton.visible=handler.getIngredientStatus()!=0&&handler.isQueryButtonVisible(client.world);super.render(context,mouseX,mouseY,delta);renderQueryIcon(context);if(queryButton!=null&&queryButton.visible&&queryButton.isHovered())context.drawTooltip(textRenderer,buildQueryTooltip(),mouseX,mouseY);else if(packButton!=null&&packButton.isHovered())context.drawTooltip(textRenderer,List.of(Text.translatable("screen.ordertocook.countertop.available_bags",handler.getAvailablePackagingCount())),mouseX,mouseY);else if(plateButton!=null&&plateButton.isHovered())context.drawTooltip(textRenderer,List.of(Text.translatable("screen.ordertocook.countertop.available_plates",handler.getAvailablePlateCount())),mouseX,mouseY);drawMouseoverTooltip(context,mouseX,mouseY);}
    private List<Text> buildQueryTooltip(){List<Text> lines=new ArrayList<>();if(handler.getIngredientStatus()==1)lines.add(Text.translatable("message.ordertocook.countertop.query_ready"));else if(handler.getMissingIngredientCount()==0)lines.add(Text.translatable("message.ordertocook.countertop.query_invalid"));else{lines.add(Text.translatable("message.ordertocook.countertop.query_missing_header"));for(int i=0;i<handler.getMissingIngredientCount();i++){ItemStack food=new ItemStack(Registries.ITEM.get(handler.getMissingIngredientId(i)));int required=handler.getMissingIngredientRequired(i),available=handler.getMissingIngredientAvailable(i);lines.add(Text.translatable("screen.ordertocook.countertop.query.tooltip_detail",food.getName(),available,required,Math.max(0,required-available)));}}lines.add(Text.translatable("screen.ordertocook.countertop.query.click_hint"));return lines;}
    private void renderQueryIcon(DrawContext context){if(queryButton==null||!queryButton.visible)return;Identifier texture=handler.getIngredientStatus()==1?QUERY_READY_TEXTURE:(handler.getMissingIngredientCount()>0?QUERY_MISSING_TEXTURE:QUERY_NONE_TEXTURE);float scale=(float)QUERY_BUTTON_SIZE/QUERY_TEXTURE_SIZE;context.getMatrices().push();context.getMatrices().translate(queryButton.getX(),queryButton.getY(),0.0F);context.getMatrices().scale(scale,scale,1.0F);context.drawTexture(texture,0,0,0,0,QUERY_TEXTURE_SIZE,QUERY_TEXTURE_SIZE,QUERY_TEXTURE_SIZE,QUERY_TEXTURE_SIZE);if(queryButton.isHovered())context.drawTexture(QUERY_HOVER_TEXTURE,0,0,0,0,QUERY_TEXTURE_SIZE,QUERY_TEXTURE_SIZE,QUERY_TEXTURE_SIZE,QUERY_TEXTURE_SIZE);context.getMatrices().pop();}
}
