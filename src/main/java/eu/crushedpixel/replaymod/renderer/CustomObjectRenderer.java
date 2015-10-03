package eu.crushedpixel.replaymod.renderer;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Allows users to render custom images in the World.
 */
public class CustomObjectRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void renderCustomObjects(RenderWorldLastEvent event) {
        // TODO
//        if(!ReplayHandler.isInReplay() || mc.getRenderViewEntity() == null || ReplayHandler.getCustomImageObjects().isEmpty()) return;
//
//        double dX = mc.getRenderViewEntity().lastTickPosX + (mc.getRenderViewEntity().posX - mc.getRenderViewEntity().lastTickPosX) * (double)event.partialTicks;
//        double dY = mc.getRenderViewEntity().lastTickPosY + (mc.getRenderViewEntity().posY - mc.getRenderViewEntity().lastTickPosY) * (double)event.partialTicks;
//        double dZ = mc.getRenderViewEntity().lastTickPosZ + (mc.getRenderViewEntity().posZ - mc.getRenderViewEntity().lastTickPosZ) * (double)event.partialTicks;
//
//        GlStateManager.enableTexture2D();
//
//        GlStateManager.enableBlend();
//        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//
//        for(CustomImageObject object : ReplayHandler.getCustomImageObjects()) {
//            drawCustomImageObject(dX, dY, dZ, object);
//        }
//    }
//
//    private void drawCustomImageObject(double playerX, double playerY, double playerZ, CustomImageObject customImageObject) {
//        ResourceLocation resourceLocation = customImageObject.getResourceLocation();
//
//        if(customImageObject.getLinkedAsset() == null
//                || ReplayHandler.getAssetRepository().getAssetByUUID(customImageObject.getLinkedAsset()) == null
//                || resourceLocation == null) return;
//
//        GlStateManager.pushMatrix();
//        GlStateManager.pushAttrib();
//
//        Tessellator tessellator = Tessellator.getInstance();
//        WorldRenderer renderer = tessellator.getWorldRenderer();
//
//        mc.renderEngine.bindTexture(resourceLocation);
//
//        int renderTimestamp;
//        if(mc.currentScreen instanceof GuiObjectManager) {
//            renderTimestamp = ((GuiObjectManager) mc.currentScreen).getObjectKeyframeTimeline().cursorPosition;
//        } else if(ReplayProcess.isVideoRecording()) {
//            renderTimestamp = ReplayProcess.getVideoRenderer().getVideoTime();
//        } else {
//            renderTimestamp = ReplayHandler.getRealTimelineCursor();
//        }
//
//        Transformation transformation = customImageObject.getTransformations().getTransformationForTimestamp(renderTimestamp);
//
//        Position objectAnchor = transformation.getAnchor();
//        Position objectPosition = transformation.getPosition();
//        Position objectOrientation = transformation.getOrientation();
//
//        double x = objectPosition.getX() - playerX;
//        double y = objectPosition.getY() - playerY;
//        double z = objectPosition.getZ() - playerZ;
//
//        GL11.glNormal3f(0, 1, 0);
//
//        GlStateManager.translate(x, y + 1.4, z);
//
//        GlStateManager.rotate((float) -objectOrientation.getX(), 0, 1, 0);
//        GlStateManager.rotate((float) objectOrientation.getY(), 0, 0, 1);
//        GlStateManager.rotate((float) objectOrientation.getZ(), 1, 0, 0);
//
//        float opacity = (float)transformation.getOpacity() / 100;
//        GlStateManager.color(1, 1, 1, opacity);
//
//        float width = (float)(customImageObject.getWidth() * transformation.getScale().getX() / 100f);
//        float height = (float)(customImageObject.getHeight() * transformation.getScale().getY() / 100f);
//
//        float minX = (float)(-width/2 + objectAnchor.getX());
//        float maxX = (float)(width/2 + objectAnchor.getX());
//        float minY = (float)(-height/2 + objectAnchor.getY());
//        float maxY = (float)(height/2 + objectAnchor.getY());
//
//        renderer.startDrawingQuads();
//
//        renderer.addVertexWithUV(minX, minY, 0, 1, 1);
//        renderer.addVertexWithUV(minX, maxY, 0, 1, 0);
//        renderer.addVertexWithUV(maxX, maxY, 0, 0, 0);
//        renderer.addVertexWithUV(maxX, minY, 0, 0, 1);
//
//        renderer.addVertexWithUV(maxX, maxY, 0, 0, 0);
//        renderer.addVertexWithUV(minX, maxY, 0, 1, 0);
//        renderer.addVertexWithUV(minX, minY, 0, 1, 1);
//        renderer.addVertexWithUV(maxX, minY, 0, 0, 1);
//
//        tessellator.draw();
//        renderer.setTranslation(0, 0, 0);
//
//        GlStateManager.popAttrib();
//        GlStateManager.popMatrix();
    }
}
