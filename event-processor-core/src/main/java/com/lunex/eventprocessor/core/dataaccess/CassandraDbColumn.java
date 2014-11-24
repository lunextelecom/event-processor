package com.lunex.eventprocessor.core.dataaccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface CassandraDbColumn {
  String name();

  @SuppressWarnings("rawtypes")
  Class collectionType() default String.class;
  
  @SuppressWarnings("rawtypes")
  Class mapKeyType() default String.class;
  
  @SuppressWarnings("rawtypes")
  Class mapValueType() default String.class;
}
