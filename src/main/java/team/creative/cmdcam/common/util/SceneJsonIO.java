package team.creative.cmdcam.common.util;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.creativecore.common.util.registry.exception.RegistryException;

import java.io.*;

public class SceneJsonIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File BASE_DIR = new File(Minecraft.getInstance().gameDirectory, "cam-scenes/worlds");
    private static final File SERVER_DIR = new File(Minecraft.getInstance().gameDirectory, "cam-scenes/servers");

    static {
        if (!BASE_DIR.exists() && !BASE_DIR.mkdirs()) {
            throw new RuntimeException("Failed to create base directory: " + BASE_DIR.getAbsolutePath());
        }
    }

    public static void save(String worldName, String name, ListTag scenes) {
        save(worldName, name, scenes, false);
    }

    public static void save(String worldName, String name, ListTag scenes, boolean isServer) {
        File file = getSceneFile(worldName, name, isServer);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + file.getParentFile().getAbsolutePath());
        }

        JsonArray jsonScenes = new JsonArray();
        for (Tag tag: scenes) {
            if (tag instanceof CompoundTag) {
                jsonScenes.add(NBTJsonConverter.toJson((CompoundTag) tag));
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(jsonScenes, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CamScene[] load(String worldName, String name, boolean isServer) {
        File file = getSceneFile(worldName, name, isServer);
        if (!file.exists()) {
            return new CamScene[0];
        }

        try (FileReader reader = new FileReader(file)) {
            JsonArray sceneJsonArray = JsonParser.parseReader(reader).getAsJsonArray();
            CamScene[] scenes = new CamScene[sceneJsonArray.size()];
            for (int i = 0; i < sceneJsonArray.size(); i++) {
                JsonObject jsonScene = sceneJsonArray.get(i).getAsJsonObject();
                CompoundTag nbt = NBTJsonConverter.fromJson(jsonScene);
                scenes[i] = new CamScene(nbt);
            }
            return scenes;
        } catch (IOException | RegistryException e) {
            e.printStackTrace();
            return new CamScene[0];
        }
    }

    private static File getSceneFile(String worldName, String name, boolean isServer) {
        return new File(isServer ? SERVER_DIR : BASE_DIR, worldName + File.separator + name + ".json");
    }
}