package team.creative.cmdcam.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.*;

import java.util.Map;

public class NBTJsonConverter {

    public static JsonObject toJson(CompoundTag nbt) {
        JsonObject json = new JsonObject();

        for (String key : nbt.getAllKeys()) {
            json.add(key, tagToJson(nbt.get(key)));
        }
        return json;
    }

    public static CompoundTag fromJson(JsonObject json) {
        CompoundTag nbt = new CompoundTag();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            nbt.put(entry.getKey(), jsonToTag(entry.getValue()));
        }
        return nbt;
    }

    private static JsonElement tagToJson(Tag tag) {
        if (tag instanceof CompoundTag) {
            return toJson((CompoundTag) tag);
        }

        if (tag instanceof ListTag) {
            JsonArray array = new JsonArray();
            for (Tag element : (ListTag) tag) {
                array.add(tagToJson(element));
            }
            return array;
        }

        if (tag instanceof NumericTag) {
            return new JsonPrimitive(((NumericTag) tag).getAsNumber());
        }

        if (tag instanceof StringTag) {
            return new JsonPrimitive(tag.getAsString());
        }

        if (tag instanceof ByteTag) {
            return new JsonPrimitive(((ByteTag) tag).getAsByte());
        }

        if (tag instanceof IntArrayTag) {
            JsonArray array = new JsonArray();
            for (int i : ((IntArrayTag) tag).getAsIntArray()) {
                array.add(i);
            }
            return array;
        }

        if (tag instanceof LongArrayTag) {
            JsonArray array = new JsonArray();
            for (long l : ((LongArrayTag) tag).getAsLongArray()) {
                array.add(l);
            }
            return array;
        }

        return new JsonPrimitive(tag.getAsString());
    }

    private static Tag jsonToTag(JsonElement json) {
        if (json.isJsonObject()) {
            return fromJson(json.getAsJsonObject());
        }

        if (json.isJsonArray()) {
            ListTag list = new ListTag();
            for (JsonElement element : json.getAsJsonArray()) {
                list.add(jsonToTag(element));
            }
            return list;
        }

        if (json.isJsonPrimitive()) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsString().contains(".") ?
                        DoubleTag.valueOf(primitive.getAsDouble()) : LongTag.valueOf(primitive.getAsLong());
            }
            if (primitive.isBoolean()) {
                return ByteTag.valueOf(primitive.getAsBoolean());
            }
            return StringTag.valueOf(primitive.getAsString());
        }

        return StringTag.valueOf(json.getAsString());
    }
}