package de.rototor.pdfbox.graphics2d;

import java.lang.reflect.*;

/**
 * This is an internal class which does dirty private field access.
 */
class PrivateFieldAccessor
{
    public static <T> T getPrivateField(Object object, String property) {
        Class<?> cls = object.getClass();
        while (cls != null) {
            for (Field f : cls.getDeclaredFields()) {
                if (!f.getName().equals(property))
                    continue;
                setAccessible(f);
                try {
                    // noinspection unchecked
                    return (T) f.get(object);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Error while accessing private field " + property + " of class " + cls, e);
                }
            }
            cls = cls.getSuperclass();
        }
        throw new NoSuchFieldError(property);
    }


    /*
     * Default Implementation für Java <= 8
     */
    private static Consumer<AccessibleObject> performSetAccessible = new PrivateFieldAccessor.Consumer<AccessibleObject>() {
        @Override
        public void accept(AccessibleObject obj) {
            obj.setAccessible(true);
        }
    };
    /* We are not on JDK 8 yet ... */
    private interface Consumer<T> {
        void accept(T t);
    }

    static {
        try {
            /*
             * This stuff is for JDK 9+ only, and it only works there too..
             */
            final Method setAccessible = AccessibleObject.class.getDeclaredMethod("setAccessible0", boolean.class);
            Field methodModifiers = Method.class.getDeclaredField("modifiers");

            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");

            Constructor<?> unsafeConstructor = unsafeClass.getDeclaredConstructor();
            unsafeConstructor.setAccessible(true);
            Object unsafe = unsafeConstructor.newInstance();
            Method objectFieldOffsetMethod = unsafeClass.getMethod("objectFieldOffset", Field.class);
            long methodModifiersOffset = (Long) objectFieldOffsetMethod.invoke(unsafe, methodModifiers);

            Method getAndSetInt = unsafeClass.getMethod("getAndSetInt", Object.class, Long.TYPE, Integer.TYPE);
            getAndSetInt.invoke(unsafe, setAccessible, methodModifiersOffset, Modifier.PUBLIC);

            performSetAccessible = new PrivateFieldAccessor.Consumer<AccessibleObject>()
            {
                @Override
                public void accept(AccessibleObject obj)
                {
                    try
                    {
                        setAccessible.invoke(obj, true);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(
                                e); // you definitely should do this in a different way :D
                    }
                }
            };
        } catch (Exception ignored) {
        }
    }

    private static void setAccessible(AccessibleObject object) {
        performSetAccessible.accept(object);
    }
}
