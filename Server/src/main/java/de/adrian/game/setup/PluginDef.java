package de.adrian.game.setup;

import de.adrian.game.GamePlugin;

import java.io.File;

public record PluginDef(File file, Class<? extends GamePlugin> main, String version, String name) {
}
