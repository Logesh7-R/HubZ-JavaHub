package hubz.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;

public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private JsonUtil() {}

    //Object to String Json
    //Serialization
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    //String json to particular class type like CommitModel, TreeModel.
    //Deserialization
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    //String json to complex class type like List<Map<String,String>>
    //Deserialization - overloading method
    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }
}
