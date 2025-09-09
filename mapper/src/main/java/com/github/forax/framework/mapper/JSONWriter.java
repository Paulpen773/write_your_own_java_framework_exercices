package com.github.forax.framework.mapper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class JSONWriter {

    private record Property(String prefix, Method getter){}
    private static final class PropertieDescriptorClassValue extends ClassValue<List<Property>>{
        @Override
        protected List<Property> computeValue(Class<?> type){
            var beanInfo = Utils.beanInfo(type);
            return Arrays.stream(beanInfo.getPropertyDescriptors()).filter(propertyDescriptor -> !propertyDescriptor.getName().equals(("class"))).map(property -> {
                var prefix ='"' + property.getName() + "\": ";
                var getter =property.getReadMethod();
                return new Property(prefix,getter);
            }).toList();
        }
    }

    private static final PropertieDescriptorClassValue CLASS_VALUE= new PropertieDescriptorClassValue();

    public String toJSON(Object o) {
          return switch(o){
              case null -> "null";
              case Boolean b -> b.toString();
              case String s -> "\""+ s + "\"";
              case Integer i -> i.toString();
              case Double d -> d.toString();
              case Object o2 -> {
                  var properties =CLASS_VALUE.get(o2.getClass());
                  yield properties.stream()
                          .map(property ->{
                              var value = Utils.invokeMethod(o2,property.getter);
                              return  property.prefix + toJSON(value);
                          })
                          .collect(Collectors.joining(", ","{","}"));
              }
          };
  }
}
