package com.itiancai.galaxy.dts.annotation;

public class DefaultParamParser implements ParamParser {
  @Override
  public String parse(Object o) {
    return String.valueOf(o);
  }
}
