package com.mkyong.util;

import java.io.IOException;
import java.util.Date;


import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator;
import org.apache.tinkerpop.shaded.jackson.core.JsonProcessingException;
import org.apache.tinkerpop.shaded.jackson.core.type.TypeReference;
import org.apache.tinkerpop.shaded.jackson.databind.Module;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectReader;
import org.apache.tinkerpop.shaded.jackson.databind.SerializerProvider;
import org.apache.tinkerpop.shaded.jackson.databind.module.SimpleModule;
import org.apache.tinkerpop.shaded.jackson.databind.ser.std.StdSerializer;

import com.google.common.collect.ImmutableSet;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ImmutableSet<String> SPECIAL_FLOATS;

    static {
        SimpleModule module = new SimpleModule();
        SPECIAL_FLOATS = ImmutableSet.of("-Infinity", "Infinity", "NaN");

        module.addSerializer(RawJson.class, new RawJsonSerializer());

//        HugeGraphSONModule.registerCommonSerializers(module);
//        HugeGraphSONModule.registerIdSerializers(module);
//        HugeGraphSONModule.registerSchemaSerializers(module);
//        HugeGraphSONModule.registerGraphSerializers(module);

        MAPPER.registerModule(module);
    }

    public static void registerModule(Module module) {
        MAPPER.registerModule(module);
    }

    public static String toJson(Object object) throws Exception {
        return MAPPER.writeValueAsString(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
//        E.checkState(json != null,
//                "Json value can't be null for '%s'",
//                clazz.getSimpleName());
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            //throw new HugeException("Can't read json: %s", e, e.getMessage());
            throw new Exception("error");
        }
    }

    public static <T> T fromJson(String json, TypeReference<?> typeRef) throws Exception {
//        E.checkState(json != null,
//                "Json value can't be null for '%s'",
//                typeRef.getType());
        try {
            ObjectReader reader = MAPPER.readerFor(typeRef);
            return reader.readValue(json);
        } catch (IOException e) {
            //throw new HugeException("Can't read json: %s", e, e.getMessage());
            throw new Exception("error");
        }
    }

    /**
     * Number collection will be parsed to Double Collection via fromJson,
     * this method used to cast element in collection to original number type
     *
     * @param object original number
     * @param clazz  target type
     * @return target number
     */
    public static Object castNumber(Object object, Class<?> clazz) {
        if (object instanceof Number) {
            Number number = (Number) object;
            if (clazz == Byte.class) {
                object = number.byteValue();
            } else if (clazz == Integer.class) {
                object = number.intValue();
            } else if (clazz == Long.class) {
                object = number.longValue();
            } else if (clazz == Float.class) {
                object = number.floatValue();
            } else if (clazz == Double.class) {
                assert object instanceof Double : object;
            } else {
                assert clazz == Date.class : clazz;
            }
        }
        return object;
    }

    public static <V> boolean isInfinityOrNaN(V value) {
        return value instanceof String && SPECIAL_FLOATS.contains(value);
    }

    public static Object asJson(Object value) {
        RawJson rj = null;
        try {
            rj = new RawJson(toJson(value));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rj;
    }

    public static Object asJson(String value) {
        return new RawJson(value);
    }

    private static class RawJson {

        private final String value;

        public RawJson(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    private static class RawJsonSerializer extends StdSerializer<RawJson> {

        private static final long serialVersionUID = 3240301861031054251L;

        public RawJsonSerializer() {
            super(RawJson.class);
        }

        @Override
        public void serialize(RawJson json, JsonGenerator generator,
                              SerializerProvider provider) throws IOException {
            generator.writeRawValue(json.value());
        }
    }
}

