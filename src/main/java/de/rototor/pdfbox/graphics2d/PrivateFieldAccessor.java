package de.rototor.pdfbox.graphics2d;

import java.lang.reflect.*;

/**
 * Ugly Hack to access private fields. This will for sure break with
 * future JDK versions.
 */
class PrivateFieldAccessor
{
    /*
     * Default implementation for Java <= 8
     */
    private static IAccessableSetter performSetAccessible = new IAccessableSetter()
    {
        @Override
        public void setAccessible(AccessibleObject obj)
        {
            obj.setAccessible(true);
        }
    };

    private interface IAccessableSetter
    {
        void setAccessible(AccessibleObject obj);
    }

    static
    {
        try
        {
            /*
             * We need this for JDK 9+, and only their this works at all...
             */
            final Method setAccessible = AccessibleObject.class
                    .getDeclaredMethod("setAccessible0", boolean.class);
            Field methodModifiers = Method.class.getDeclaredField("modifiers");

            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");

            Constructor<?> unsafeConstructor = unsafeClass.getDeclaredConstructor();
            unsafeConstructor.setAccessible(true);
            Object unsafe = unsafeConstructor.newInstance();
            Method objectFieldOffsetMethod = unsafeClass
                    .getMethod("objectFieldOffset", Field.class);
            long methodModifiersOffset = (Long) objectFieldOffsetMethod
                    .invoke(unsafe, methodModifiers);

            Method getAndSetInt = unsafeClass
                    .getMethod("getAndSetInt", Object.class, Long.TYPE, Integer.TYPE);
            getAndSetInt.invoke(unsafe, setAccessible, methodModifiersOffset, Modifier.PUBLIC);

            performSetAccessible = new IAccessableSetter()
            {
                @Override
                public void setAccessible(AccessibleObject obj)
                {
                    try
                    {
                        setAccessible.invoke(obj, true);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        catch (Exception ignored)
        {
        }
    }

    @SuppressWarnings("WeakerAccess")
    private static void setAccessible(AccessibleObject object)
    {
        performSetAccessible.setAccessible(object);
    }

    public static <T> T getPrivateField(Object object, String property)
    {
        Class<?> cls = object.getClass();
        while (cls != null)
        {
            for (Field f : cls.getDeclaredFields())
            {
                if (!f.getName().equals(property))
                    continue;
                setAccessible(f);
                try
                {
                    // noinspection unchecked
                    return (T) f.get(object);
                }
                catch (RuntimeException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(
                            "Error while accessing field " + property + " of " + cls, e);
                }
            }
            cls = cls.getSuperclass();
        }
        throw new NoSuchFieldError(property);
    }

}
