package net.luckyowlstudios.sentience.client;

import net.luckyowlstudios.sentience.kingdom.Kingdom;
import net.luckyowlstudios.sentience.network.CreateKingdomPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.item.DyeColor;

/**
 * "Create Your Kingdom": a name box, the sixteen dye colours to pick from, and Create / Cancel.
 * Built only from vanilla widgets so it looks like the rest of the game.
 */
public class CreateKingdomScreen extends Screen {

    private static final Component NAME_LABEL = Component.translatable("sentience.kingdom.create.name");
    private static final Component COLOR_LABEL = Component.translatable("sentience.kingdom.create.color");
    private static final int SWATCH_SIZE = 20;
    private static final int SWATCH_GAP = 4;
    private static final int SWATCHES_PER_ROW = 8;

    private final BlockPos townHall;
    private EditBox nameBox;
    private Button createButton;
    private DyeColor color = DyeColor.BLUE;

    public CreateKingdomScreen(BlockPos townHall) {
        super(Component.translatable("sentience.kingdom.create.title"));
        this.townHall = townHall;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 80;

        String previous = this.nameBox != null ? this.nameBox.getValue() : "";
        this.nameBox = new EditBox(this.font, centerX - 100, top + 34, 200, 20, NAME_LABEL);
        this.nameBox.setMaxLength(Kingdom.MAX_NAME_LENGTH);
        this.nameBox.setValue(previous);
        this.nameBox.setResponder(value -> this.updateCreateButton());
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        int rowWidth = SWATCHES_PER_ROW * SWATCH_SIZE + (SWATCHES_PER_ROW - 1) * SWATCH_GAP;
        int swatchLeft = centerX - rowWidth / 2;
        int swatchTop = top + 82;
        DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            int x = swatchLeft + (i % SWATCHES_PER_ROW) * (SWATCH_SIZE + SWATCH_GAP);
            int y = swatchTop + (i / SWATCHES_PER_ROW) * (SWATCH_SIZE + SWATCH_GAP);
            this.addRenderableWidget(new ColorSwatch(x, y, colors[i]));
        }

        int buttonTop = swatchTop + 2 * (SWATCH_SIZE + SWATCH_GAP) + 12;
        this.createButton = this.addRenderableWidget(Button.builder(Component.translatable("sentience.kingdom.create.confirm"), button -> this.create())
                .bounds(centerX - 102, buttonTop, 100, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(centerX + 2, buttonTop, 100, 20).build());
        this.updateCreateButton();
    }

    private void updateCreateButton() {
        if (this.createButton != null) {
            this.createButton.active = !this.nameBox.getValue().isBlank();
        }
    }

    private void create() {
        String name = this.nameBox.getValue().strip();
        if (name.isEmpty() || this.minecraft == null || this.minecraft.getConnection() == null) {
            return;
        }
        this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new CreateKingdomPayload(this.townHall, name, this.color)));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter in the name box creates the kingdom, like vanilla's create-world screen.
        if ((keyCode == 257 || keyCode == 335) && this.createButton.active) {
            this.create();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int top = this.height / 2 - 80;
        graphics.drawCenteredString(this.font, this.title, centerX, top, 0xFFFFFF);
        graphics.drawString(this.font, NAME_LABEL, centerX - 100, top + 22, 0xA0A0A0);
        graphics.drawString(this.font, COLOR_LABEL, centerX - 100, top + 70, 0xA0A0A0);
        // Preview of the name in the chosen colour, as it will show in chat.
        String name = this.nameBox.getValue().strip();
        if (!name.isEmpty()) {
            graphics.drawString(this.font, Component.literal(name), centerX + 100 - this.font.width(name), top + 70,
                    this.color.getTextColor() | 0xFF000000);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void open(BlockPos townHall) {
        Minecraft.getInstance().setScreen(new CreateKingdomScreen(townHall));
    }

    /** One colour to pick: drawn like a vanilla button with the colour inside, outlined in white when chosen. */
    private final class ColorSwatch extends AbstractButton {

        private final DyeColor swatchColor;

        ColorSwatch(int x, int y, DyeColor swatchColor) {
            super(x, y, SWATCH_SIZE, SWATCH_SIZE, colorName(swatchColor));
            this.swatchColor = swatchColor;
            this.setTooltip(Tooltip.create(colorName(swatchColor)));
        }

        @Override
        public void onPress() {
            CreateKingdomScreen.this.color = this.swatchColor;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            boolean selected = CreateKingdomScreen.this.color == this.swatchColor;
            // Dark frame like a vanilla slot, then the colour.
            graphics.fill(x, y, x + this.width, y + this.height, 0xFF000000);
            graphics.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, 0xFF8B8B8B);
            graphics.fill(x + 3, y + 3, x + this.width - 3, y + this.height - 3, this.swatchColor.getTextureDiffuseColor() | 0xFF000000);
            if (selected) {
                graphics.renderOutline(x, y, this.width, this.height, 0xFFFFFFFF);
                graphics.renderOutline(x + 1, y + 1, this.width - 2, this.height - 2, 0xFFFFFFFF);
            } else if (this.isHoveredOrFocused()) {
                graphics.renderOutline(x, y, this.width, this.height, 0xFFA0A0A0);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.getMessage());
        }
    }

    private static Component colorName(DyeColor color) {
        return Component.translatable("color.minecraft." + color.getName());
    }
}
