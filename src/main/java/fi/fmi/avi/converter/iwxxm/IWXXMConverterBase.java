package fi.fmi.avi.converter.iwxxm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * Created by rinne on 20/07/17.
 */
public abstract class IWXXMConverterBase {
    private static JAXBContext jaxbCtx = null;
    private static Map<String, Object> classToObjectFactory = new HashMap<>();
    private static Map<String, Object> objectFactoryMap = new HashMap<>();

    /**
     * Singleton for accessing the shared JAXBContext for IWXXM JAXB handling.
     *
     * @return the context
     */
    protected static synchronized JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbCtx == null) {
            // NOTE: this can take several seconds, needs to scan all the jars in classpath!!
            jaxbCtx = JAXBContext.newInstance("icao.iwxxm21:aero.aixm511:net.opengis.gml32:org.iso19139.ogc2007.gmd:org.iso19139.ogc2007.gco:org"
                    + ".iso19139.ogc2007.gss:org.iso19139.ogc2007.gts:org.iso19139.ogc2007.gsr:net.opengis.om20:net.opengis.sampling:net.opengis.sampling"
                    + ".spatial:wmo.metce2013:wmo.opm2013:org.w3c.xlink11");
        }
        return jaxbCtx;
    }

    protected static <T> T create(final Class<T> clz) throws IllegalArgumentException {
        return create(clz, null);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T create(final Class<T> clz, final JAXBElementConsumer<T> consumer) throws IllegalArgumentException {
        Object result = null;
        Object objectFactory = getObjectFactory(clz);
        if (objectFactory != null) {
            String methodName = null;
            if (clz.getEnclosingClass() != null) {
                Class<?> encClass = clz.getEnclosingClass();
                StringBuilder sb = new StringBuilder("create").append(encClass.getSimpleName().substring(0, 1).toUpperCase())
                        .append(encClass.getSimpleName().substring(1));
                while (encClass.getEnclosingClass() != null) {
                    sb.append(clz.getSimpleName());
                    encClass = encClass.getEnclosingClass();
                }
                methodName = sb.append(clz.getSimpleName()).toString();
            } else {
                methodName = new StringBuilder("create").append(clz.getSimpleName().substring(0, 1).toUpperCase())
                        .append(clz.getSimpleName().substring(1))
                        .toString();
            }
            try {
                Method toCall = objectFactory.getClass().getMethod(methodName);
                result = toCall.invoke(objectFactory);
            } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | IllegalAccessError | InvocationTargetException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to create JAXB element object for type " + clz, e);
            }
            if (consumer != null) {
                consumer.consume((T) result);
            }
        } else {
            throw new IllegalArgumentException("Unable to find ObjectFactory for JAXB element type " + clz);
        }
        return (T) result;
    }

    protected static <T> JAXBElement<T> createAndWrap(Class<T> clz) {
        return createAndWrap(clz, null);
    }

    protected static <T> JAXBElement<T> createAndWrap(Class<T> clz, final JAXBElementConsumer<T> consumer) {
        T element = create(clz);
        return wrap(element, clz, consumer);
    }

    protected static <T> JAXBElement<T> wrap(T element, Class<T> clz) {
        return wrap(element, clz, null);
    }

    @SuppressWarnings("unchecked")
    protected static <T> JAXBElement<T> wrap(T element, Class<T> clz, final JAXBElementConsumer<T> consumer) {
        Object result = null;
        Object objectFactory = getObjectFactory(clz);
        if (objectFactory != null) {
            String methodName = new StringBuilder("create").append(clz.getSimpleName().substring(0, 1).toUpperCase())
                    .append(clz.getSimpleName().substring(1, clz.getSimpleName().lastIndexOf("Type")))
                    .toString();
            try {
                Method toCall = objectFactory.getClass().getMethod(methodName, clz);
                result = toCall.invoke(objectFactory, element);
            } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | IllegalAccessError | InvocationTargetException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to create JAXBElement wrapper", e);
            }
        } else {
            throw new IllegalArgumentException("Unable to find ObjectFactory for JAXB element type " + clz);
        }
        if (consumer != null) {
            consumer.consume(element);
        }
        return (JAXBElement<T>) result;
    }

    private static Object getObjectFactory(Class<?> clz) {
        Object objectFactory = null;
        try {
            synchronized (objectFactoryMap) {

                objectFactory = classToObjectFactory.get(clz.getCanonicalName());
                if (objectFactory == null) {
                    String objectFactoryPath = clz.getPackage().getName();
                    String objectFactoryName = null;
                    Class<?> ofClass = null;
                    while (objectFactory == null && objectFactoryPath != null) {
                        objectFactoryName = objectFactoryPath + ".ObjectFactory";
                        objectFactory = objectFactoryMap.get(objectFactoryName);
                        if (objectFactory == null) {
                            try {
                                ofClass = IWXXMConverterBase.class.getClassLoader().loadClass(objectFactoryName);
                                break;
                            } catch (ClassNotFoundException cnfe) {
                                int nextDot = objectFactoryPath.lastIndexOf('.');
                                if (nextDot == -1) {
                                    objectFactoryPath = null;
                                } else {
                                    objectFactoryPath = objectFactoryPath.substring(0, nextDot);
                                }
                            }
                        }
                    }
                    if (ofClass != null) {
                        Constructor<?> c = ofClass.getConstructor();
                        objectFactory = c.newInstance();
                        objectFactoryMap.put(objectFactoryName, objectFactory);
                    }
                    classToObjectFactory.put(clz.getCanonicalName(), objectFactory);
                }
            }
            return objectFactory;
        } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | IllegalAccessError | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to get ObjectFactory for " + clz.getCanonicalName(), e);
        }
    }

    @FunctionalInterface
    interface JAXBElementConsumer<V> {
        public void consume(V element);
    }
}
