package net.winterly.rxjersey.server;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.message.MessageBodyWorkers;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * Generic {@link MessageBodyWriter} that overrides writer to serialise incoming entity as type of generic class. <br>
 * This class will only redirect writing to another {@link MessageBodyWriter} <br>
 * Requires list of supported types <br>
 * <p>
 * Providers implementing {@link RxGenericBodyWriter} must be programmatically registered in {@link ServiceLocator}
 */
public abstract class RxGenericBodyWriter implements MessageBodyWriter<Object> {

    private final List<Class> allowedTypes;

    @Inject
    private Provider<MessageBodyWorkers> workers;

    /**
     * @param allowedTypes list of types to be processed by this writer
     */
    protected RxGenericBodyWriter(Class<?>... allowedTypes) {
        this.allowedTypes = Arrays.asList(allowedTypes);
    }

    /**
     * @param genericType type to process
     * @return the raw type without generics
     */
    private static Class raw(Type genericType) {
        if(genericType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) genericType;
            return (Class) parameterizedType.getRawType();
        }

        return (Class) genericType;
    }

    /**
     * @param genericType type to process
     * @return first type from generic list
     */
    private static Type actual(Type genericType) {
        if(genericType instanceof ParameterizedType) {
            final ParameterizedType actualGenericType = (ParameterizedType) genericType;
            return actualGenericType.getActualTypeArguments()[0];
        }

        return Object.class;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        Class rawType = raw(genericType);
        return allowedTypes.contains(rawType);
    }

    @Override
    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0; //skip
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(Object entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {

        final Type actualTypeArgument = actual(genericType);
        final MessageBodyWriter writer = workers.get().getMessageBodyWriter((Class) actualTypeArgument, actualTypeArgument, annotations, mediaType);

        if (writer == null) {
            return;
        }

        writer.writeTo(entity, entity.getClass(), actualTypeArgument, annotations, mediaType, httpHeaders, entityStream);
    }
}