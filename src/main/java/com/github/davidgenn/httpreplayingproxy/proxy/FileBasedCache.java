package com.github.davidgenn.httpreplayingproxy.proxy;


import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The cache used to store the responses.
 */
class FileBasedCache {

    public static final String RESET_CACHE_AT_STARTUP = "reset.httpreplayingproxy.cache";

    private final String rootDirectory;
    private final Map<String, CachedResponse> cache = new HashMap<String, CachedResponse>();
    private final long timeToLiveInSeconds;

    /**
     * Creates a FileBasedCache.
     * @param rootDirectory The directory to cache the responses in.
     * @throws IOException
     */
    public FileBasedCache(String rootDirectory, long timeToLiveInSeconds) throws IOException {
        this.rootDirectory = rootDirectory;
        this.timeToLiveInSeconds = timeToLiveInSeconds;
        resetCacheAtStartup(rootDirectory);
        prePopulateCache();
    }

    private void prePopulateCache() throws IOException {
        File directory = resolveCacheDirectory(rootDirectory);
        Gson gson = getGson();
        if (directory.listFiles() == null) {
            return;
        }
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            CachedResponse requestToProxy = gson.fromJson(IOUtils.toString(new FileReader(file)), CachedResponse.class);
            cache.put(file.getAbsolutePath(), requestToProxy);
        }
    }

    private static File resolveCacheDirectory(String rootDirectory) {
        File directory = new File(rootDirectory);
        directory.mkdirs();
        return directory;
    }

    private Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Header.class, new InstanceCreator<Header>() {
            @Override
            public Header createInstance(Type type) {
                return new BasicHeader("", "");
            }
        });
        builder.registerTypeAdapter(HttpEntity.class, new InstanceCreator<HttpEntity>() {
            @Override
            public HttpEntity createInstance(Type type) {
                try {
                    return new StringEntity("");
                } catch (UnsupportedEncodingException e) {
                   throw new RuntimeException(e);
                }
            }
        });
        builder.registerTypeAdapter(Header.class, new JsonDeserializer<Header>() {
            @Override
            public Header deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject jsonObject = json.getAsJsonObject();
                return new BasicHeader(jsonObject.get("name").getAsString(), jsonObject.get("value").getAsString());
            }
        });
        builder.registerTypeAdapter(HttpEntity.class, new JsonDeserializer<HttpEntity>() {
            @Override
            public HttpEntity deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                try {
                    return new StringEntity(json.getAsString());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return builder.create();
    }

    private void resetCacheAtStartup(String rootDirectory) {
        if (System.getProperty(RESET_CACHE_AT_STARTUP) != null && !System.getProperty(RESET_CACHE_AT_STARTUP).isEmpty()) {
            reset(rootDirectory);
        }
    }

    /**
     * Adds an entry to the cache.
     * @param filename The file name.
     * @param content The content to cache.
     * @throws IOException
     */
    public void put(String filename, final CachedResponse content) throws IOException {

        String safeFileName = escapeFileName(filename);
        File file = new File(rootDirectory + safeFileName + "-" + new Date().getTime() + ".json");
        if (!file.exists()) {
           file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(HttpEntity.class, new JsonSerializer<HttpEntity>() {
            @Override
            public JsonElement serialize(HttpEntity src, Type typeOfSrc, JsonSerializationContext context) {
                try {
                    return new JsonPrimitive(new String(IOUtils.toByteArray(content.getRequestToProxy().getBody().getContent())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Gson gson = gsonBuilder.create();
        fileWriter.write(gson.toJson(content));
        fileWriter.flush();
        fileWriter.close();

        cache.put(file.getName(), content);

    }

    private String escapeFileName(String filename) {

        return filename.replace("/", "-").replace("?", "+").replace("&", "+");
    }

    /**
     * Returns the cached entry. Returns null for a cache miss.
     * @param requestToProxy The request being proxied.
     * @return The cached response. Null if not present.
     */
    public CachedResponse get(RequestToProxy requestToProxy) {
        CachedResponse responseToReturn = null;
        for (CachedResponse response : cache.values()) {
            if (response.getRequestToProxy().toString().equals(requestToProxy.toString())) {
                responseToReturn = response;
                break;
            }
        }
        if (responseToReturn != null && hasNotExpired(responseToReturn)) {
            return responseToReturn;
        }
        return null;
    }

    private boolean hasNotExpired(CachedResponse responseToReturn) {
        return (responseToReturn.getTimeCreatedUtcMillis() + (timeToLiveInSeconds * 1000)) > new Date().getTime();
    }

    /**
     * Empties the cache.
     * @param rootDirectory The directory storing the cached responses.
     */
    public static void reset(String rootDirectory) {
        File directory = resolveCacheDirectory(rootDirectory);
        if (directory.listFiles() == null) {
            return;
        }
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
               continue;
            }
            file.delete();
        }
    }
}
