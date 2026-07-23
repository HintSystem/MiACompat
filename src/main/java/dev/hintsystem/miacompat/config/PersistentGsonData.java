package dev.hintsystem.miacompat.config;

import net.minecraft.resources.Identifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class PersistentGsonData<T> extends PersistentData {
    protected static final Gson DEFAULT_GSON = new GsonBuilder()
        .registerTypeAdapter(Identifier.class, new IdentifierTypeAdapter())
        .create();

    protected Gson getGson() { return DEFAULT_GSON; }

    protected abstract Class<T> getDataClass();

    protected abstract void applyData(T data);

    @Override
    protected String serialize() {
        return getGson().toJson(this);
    }

    @Override
    protected void deserialize(String json) {
        T data = getGson().fromJson(json, getDataClass());

        if (data != null) {
            applyData(data);
        }
    }
}
