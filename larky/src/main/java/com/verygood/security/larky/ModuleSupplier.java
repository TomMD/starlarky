/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.nativelib.LarkyC99Math;
import com.verygood.security.larky.nativelib.LarkyHashlib;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.lib.json.Json;
import net.starlark.java.lib.proto.Proto;

import java.util.Map;
import java.util.function.Function;

/**
 * A supplier of modules for Larky
 */
public class ModuleSupplier {

  private final Map<String, Object> environment;

  public ModuleSupplier() {
    this(ImmutableMap.<String,Object>builder().build());
  }

  public ModuleSupplier(Map<String, Object> environment) {
    this.environment = Preconditions.checkNotNull(environment);
  }

  public ModuleSupplier(ImmutableSet<StarlarkValue> moduleSet) {
    this.environment = moduleSetAsMap(Preconditions.checkNotNull(moduleSet));
  }

  /**
   * Get all available modules
   */
  public ImmutableSet<StarlarkValue> getModules() {
    return ImmutableSet.of(
        Proto.INSTANCE,
        Json.INSTANCE,
        new LarkyHashlib(),
        new LarkyC99Math()
    );
  }

  public Map<String, Object> getEnvironment() {
    return environment;
  }

  public final ModuleSet create() {
    return new ModuleSet(ImmutableMap.<String, Object>builder()
            .putAll(modulesToVariableMap())
            .putAll(getEnvironment())
            .build()); // should allow overrides ;
  }

  private ImmutableMap<String, Object> moduleSetAsMap(ImmutableSet<StarlarkValue> moduleSet) {
    return moduleSet
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                   this::findClosestStarlarkBuiltinName,
                   Function.identity()));
  }

  public ImmutableMap<String, Object> modulesToVariableMap() {
    return moduleSetAsMap(getModules());
  }

  private String findClosestStarlarkBuiltinName(Object o) {
    Class<?> cls = o.getClass();
    while (cls != null && cls != Object.class) {
      StarlarkBuiltin annotation = cls.getAnnotation(StarlarkBuiltin.class);
      if (annotation != null) {
        return annotation.name();
      }
      cls = cls.getSuperclass();
    }
    throw new IllegalStateException("Cannot find @StarlarkBuiltin for " + o.getClass());
  }

  /**
   * A set of modules and options for evaluating a Skylark config file.
   */
  public static class ModuleSet {

    private final ImmutableMap<String, Object> modules;

    ModuleSet(ImmutableMap<String, Object> modules) {
      this.modules = Preconditions.checkNotNull(modules);
    }

     /**
     * Non-static modules.
     */
    public ImmutableMap<String, Object> getModules() {
      return modules;
    }

  }
}