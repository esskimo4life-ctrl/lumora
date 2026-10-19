package dev.aurea.visuals;

import java.time.LocalTime;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public enum AureaModule {
    FULLBRIGHT("Макс. яркость", "Поднимает яркость игры до максимума.", Category.VISUALS, false, false),
    CUSTOM_CROSSHAIR("Акцентный прицел", "Тонкая рамка вокруг центрального прицела.", Category.VISUALS, false, false),
    NO_BOBBING("Без качания камеры", "Отключает покачивание при ходьбе.", Category.VISUALS, false, false),
    COMPACT_HUD("Компактные плашки", "Уменьшает отступы информационного HUD.", Category.VISUALS, true, false),

    FPS("Счётчик FPS", "Показывает текущую частоту кадров.", Category.HUD, true, true),
    COORDS("Координаты", "Текущие координаты игрока в мире.", Category.HUD, true, true),
    DIRECTION("Направление", "Показывает направление взгляда.", Category.HUD, false, true),
    WORLD_TIME("Часы", "Показывает текущее локальное время.", Category.HUD, false, true),
    HELD_ITEM("Предмет в руке", "Название предмета и его прочность.", Category.HUD, false, true),
    ARMOR("Прочность брони", "Количество частей брони и средний запас прочности.", Category.HUD, true, true),
    TARGET("Информация о цели", "Имя сущности под прицелом и здоровье цели.", Category.HUD, false, true),
    SPEED("Скорость", "Приблизительная скорость передвижения в блоках в секунду.", Category.HUD, false, true),
    POTIONS("Эффекты", "Количество активных эффектов игрока.", Category.HUD, false, true),
    SESSION("Время сессии", "Время с момента входа в текущий мир.", Category.HUD, false, true),
    FPS_GRAPH("График FPS", "Небольшая история частоты кадров в правом верхнем углу.", Category.HUD, false, false),

    BIOME("Название биома", "Показывает биом, в котором находится игрок.", Category.WORLD, false, true),
    PING("Задержка сервера", "Пинг текущего подключения, если он доступен.", Category.WORLD, false, true),

    LOW_RENDER_DISTANCE("Дистанция 6 чанков", "Временно снижает дальность прорисовки для слабых устройств.", Category.PERFORMANCE, false, false);

    public enum Category {
        VISUALS("Визуал"),
        HUD("HUD"),
        WORLD("Мир"),
        PERFORMANCE("Производительность");

        private final String title;

        Category(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private final String title;
    private final String description;
    private final Category category;
    private final boolean defaultEnabled;
    private final boolean hudText;

    AureaModule(String title, String description, Category category, boolean defaultEnabled, boolean hudText) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
        this.hudText = hudText;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Category category() {
        return category;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public boolean hasHudText() {
        return hudText;
    }

    public String hudText(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }

        var player = client.player;
        return switch (this) {
            case FPS -> "FPS: " + client.getFps();
            case COORDS -> "XYZ: " + player.blockPosition().getX() + "  "
                    + player.blockPosition().getY() + "  " + player.blockPosition().getZ();
            case DIRECTION -> "Направление: " + player.getDirection().getName().toUpperCase(Locale.ROOT);
            case WORLD_TIME -> "Время: " + LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
            case HELD_ITEM -> {
                ItemStack stack = player.getMainHandItem();
                if (stack.isEmpty()) {
                    yield "В руке: пусто";
                }
                if (stack.isDamageableItem()) {
                    int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
                    int percent = (int) Math.round(remaining * 100.0 / Math.max(1, stack.getMaxDamage()));
                    yield stack.getHoverName().getString() + " · " + percent + "%";
                }
                yield "В руке: " + stack.getHoverName().getString();
            }
            case ARMOR -> {
                int count = 0;
                int totalPercent = 0;
                int durabilityPieces = 0;
                EquipmentSlot[] armorSlots = {
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS, EquipmentSlot.FEET
                };
                for (EquipmentSlot slot : armorSlots) {
                    ItemStack stack = player.getItemBySlot(slot);
                    if (!stack.isEmpty()) {
                        count++;
                        if (stack.isDamageableItem()) {
                            int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
                            totalPercent += (int) Math.round(remaining * 100.0 / Math.max(1, stack.getMaxDamage()));
                            durabilityPieces++;
                        }
                    }
                }
                if (count == 0) {
                    yield "Броня: нет";
                }
                yield durabilityPieces == 0
                        ? "Броня: " + count + " предмета"
                        : "Броня: " + count + " · прочность " + (totalPercent / durabilityPieces) + "%";
            }
            case TARGET -> {
                Entity target = client.crosshairPickEntity;
                if (target == null) {
                    yield null;
                }
                if (target instanceof LivingEntity living) {
                    yield "Цель: " + target.getName().getString() + " · "
                            + Math.max(0, Math.round(living.getHealth())) + " HP";
                }
                yield "Цель: " + target.getName().getString();
            }
            case SPEED -> "Скорость: " + String.format(Locale.ROOT, "%.1f", player.getDeltaMovement().horizontalDistance() * 20.0) + " бл/с";
            case POTIONS -> "Эффекты: " + player.getActiveEffects().size();
            case SESSION -> {
                long seconds = Math.max(0L, (System.currentTimeMillis() - AureaClient.sessionStartedAt()) / 1000L);
                long hours = seconds / 3600L;
                long minutes = (seconds % 3600L) / 60L;
                long remainder = seconds % 60L;
                yield "Сессия: " + String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, remainder);
            }
            case BIOME -> {
                String biome = client.level.getBiome(player.blockPosition()).unwrapKey()
                        .map(key -> {
                            String value = key.toString();
                            int separator = value.lastIndexOf(" / ");
                            if (separator >= 0) {
                                value = value.substring(separator + 3);
                            }
                            if (value.endsWith("]")) {
                                value = value.substring(0, value.length() - 1);
                            }
                            int namespace = value.indexOf(':');
                            if (namespace >= 0) {
                                value = value.substring(namespace + 1);
                            }
                            return value.replace('_', ' ');
                        })
                        .orElse("неизвестно");
                yield "Биом: " + biome;
            }
            case PING -> {
                var connection = client.getConnection();
                var info = connection == null ? null : connection.getPlayerInfo(player.getUUID());
                yield info == null ? "Пинг: недоступен" : "Пинг: " + info.getLatency() + " мс";
            }
            default -> null;
        };
    }
}
