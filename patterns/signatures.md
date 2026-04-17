# Method Signature Changes — Cross-Version Pattern Reference

> All entries verified via actual compilation or official sources.

---

## Screen.keyPressed

### 1.21.4 and earlier
```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ENTER) { ... }
    return super.keyPressed(keyCode, scanCode, modifiers);
}
```

### 1.21.10+
```java
@Override
public boolean keyPressed(KeyEvent event) {
    if (event.key() == GLFW.GLFW_KEY_ENTER) { ... }
    return super.keyPressed(event);
}
```

---

## Screen.mouseClicked

### 1.21.4 and earlier
```java
@Override
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // mouseX, mouseY available as params
    return super.mouseClicked(mouseX, mouseY, button);
}
```

### 1.21.10+
```java
@Override
public boolean mouseClicked(MouseButtonEvent event, boolean dragging) {
    double mouseX = event.x();
    double mouseY = event.y();
    return super.mouseClicked(event, dragging);
}
```

---

## Screen.render

### 1.19.4 and earlier
```java
@Override
public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    fill(matrices, x1, y1, x2, y2, color);
    drawTextWithShadow(matrices, text, x, y, color);
    super.render(matrices, mouseX, mouseY, delta);
}
```

### 1.20.1+
```java
@Override
public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    guiGraphics.fill(x1, y1, x2, y2, color);
    guiGraphics.drawString(font, text, x, y, color, false);
    super.render(guiGraphics, mouseX, mouseY, delta);
}
```

---

## KeyMapping constructor

### 1.21.4 and earlier
```java
public static final KeyMapping MY_KEY = new KeyMapping(
    "key.mymod.action",
    InputConstants.Type.KEYSYM,
    GLFW.GLFW_KEY_H,
    "key.categories.misc"  // plain String
);
```

### 1.21.10+
```java
public static final KeyMapping.Category MY_CATEGORY =
    KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("mymod", "mymod"));

public static final KeyMapping MY_KEY = new KeyMapping(
    "key.mymod.action",
    InputConstants.Type.KEYSYM,
    GLFW.GLFW_KEY_H,
    MY_CATEGORY  // KeyMapping.Category object
);
```

---

## GuiGraphics.blit (skin rendering)

### 1.21.4
```java
// Pixel UV coordinates, with RenderType
graphics.blit(RenderType::guiTextured, skin, x, y, uPx, vPx, width, height, texWidth, texHeight);
```

### 1.21.10+
```java
// Fractional UV (0.0–1.0)
graphics.blit(skin, x, y, width, height, u0, v0, u1, v1);
```
