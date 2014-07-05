package davidgenn.httpreplayingproxy.proxy;


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

public class FileBasedCache {

    private final String rootDirectory;
    private final Map<String, CachedResponse> cache = new HashMap<String, CachedResponse>();

    public FileBasedCache(String rootDirectory) throws IOException {
        this.rootDirectory = rootDirectory;
        File directory = new File(rootDirectory);
        directory.mkdir();
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
        Gson gson = builder.create();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            CachedResponse requestToProxy = gson.fromJson(IOUtils.toString(new FileReader(file)), CachedResponse.class);
            cache.put(file.getAbsolutePath(), requestToProxy);
        }

    }

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

    public CachedResponse get(RequestToProxy requestToProxy) {
        for (CachedResponse response : cache.values()) {
            if (response.getRequestToProxy().toString().equals(requestToProxy.toString())) {
                return response;
            }
        }
        return null;
    }

    public static void reset(String rootDirectory) {
        File directory = new File(rootDirectory);
        directory.mkdir();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
               continue;
            }
            file.delete();
        }
    }
}
