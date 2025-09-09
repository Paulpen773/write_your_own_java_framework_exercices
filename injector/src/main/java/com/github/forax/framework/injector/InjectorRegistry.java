package com.github.forax.framework.injector;

import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

public final class InjectorRegistry {

    private final HashMap<Class<?>, Supplier<?>> registry = new HashMap<Class<?>, Supplier<?>>();

    public <T>void registerInstance(Class<T> type, T o) {
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(o, "o is null");
        registerProvider(type, () -> o);
    }

    public <T> T lookupInstance(Class<T> type) {
        Objects.requireNonNull(type, "type is null");
        var result = registry.get(type);
        if (result == null) {
            throw new IllegalStateException("not injected " + type.getName());
        }
        return type.cast(result.get());
    }

    public <T> void registerProvider(Class<T> type, Supplier<T> supplier) {
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(supplier, "supplier is null");
        var result = registry.putIfAbsent(type,supplier);
        if (result != null) {
            throw new IllegalStateException("already injected " + type.getName());
        }
    }

     static List<PropertyDescriptor> findInjectableProperties(Class<?> type){
        Objects.requireNonNull(type,"type is null");
        var beanInfo = Utils.beanInfo(type);
        return Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(propertyDescriptor ->{
                    var setter=propertyDescriptor.getWriteMethod();
                    return setter != null && setter.isAnnotationPresent(Inject.class);
                }).toList();
    }

    static Constructor<?> injectConstructor(Class<?> type){
        var constructors = type.getConstructors();
        var injectConstructors=  Arrays.stream(constructors).
                filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .toList();
        if(injectConstructors.size() > 1){
            throw new IllegalStateException("a lot of constructor");
        }
        if(injectConstructors.isEmpty()){
            return Utils.defaultConstructor(type);
        }
        return  injectConstructors.getFirst();
    }

    public <T> void registerProviderClass(Class<T> type, Class<? extends T> implementation){
        Objects.requireNonNull(type,"type is null");
        Objects.requireNonNull(implementation,"implementation is null");

        var constructor = injectConstructor(implementation);
        var properties = findInjectableProperties(type);
        registerProvider(type, () -> {
            var constructorParameterTypes = constructor.getParameterTypes();
            var constructorArgs = Arrays.stream(constructorParameterTypes).map(this::lookupInstance).toArray();
            var object = type.cast(Utils.newInstance(constructor,constructorArgs));
            for (var property : properties) {
                var seter = property.getWriteMethod();
                var propertyType = property.getPropertyType();
                var value = lookupInstance(propertyType);
                Utils.invokeMethod(object, seter, value);
            }
            return object;
        });
    }

    public void registerProviderClass(Class<?> type){
        registerProviderClass2(type);
    }

    private <T> void registerProviderClass2(Class<T> type) {
        registerProviderClass(type,type);
    }
}
