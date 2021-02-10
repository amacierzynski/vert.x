/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.junit.Test;

import io.vertx.test.spi.FakeFactory;
import io.vertx.test.spi.NotImplementedSPI;
import io.vertx.test.spi.SomeFactory;

/**
 * Check the service helper behavior.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ServiceHelperTest {

  @Test
  public void loadFactory() {
    FakeFactory factory = ServiceHelper.loadFactory(FakeFactory.class);
    assertThat(factory.classloader()).isEqualTo(ServiceHelperTest.class.getClassLoader());
  }

  @Test(expected = IllegalStateException.class)
  public void loadNotImplementedSPI() {
    ServiceHelper.loadFactory(NotImplementedSPI.class);
  }

  @Test
  public void loadFactoryOrNull() {
    NotImplementedSPI factory = ServiceHelper.loadFactoryOrNull(NotImplementedSPI.class);
    assertThat(factory).isNull();

    FakeFactory fake = ServiceHelper.loadFactoryOrNull(FakeFactory.class);
    assertThat(fake).isNotNull();
    assertThat(fake.classloader()).isEqualTo(ServiceHelperTest.class.getClassLoader());
  }

  @Test
  public void loadFactories() {
    Collection<FakeFactory> factories = ServiceHelper.loadFactories(FakeFactory.class);
    assertThat(factories)
        .isNotNull()
        .hasSize(2);

    Collection<NotImplementedSPI> impl = ServiceHelper.loadFactories(NotImplementedSPI.class);
    assertThat(impl)
        .isNotNull()
        .hasSize(0);
  }

  @Test
  public void loadFactoriesWithClassloader() throws Exception {
    ClassLoader custom = new URLClassLoader(new URL[]{new File("target/classpath/servicehelper").toURI().toURL()});

    // Try without the custom classloader.
    Collection<SomeFactory> factories = ServiceHelper.loadFactories(SomeFactory.class);
    assertThat(factories)
        .isNotNull()
        .hasSize(0);

    // Try with the custom classloader
    factories = ServiceHelper.loadFactories(SomeFactory.class, custom);
    assertThat(factories)
        .isNotNull()
        .hasSize(1);
    assertThat(factories.iterator().next().classloader()).isEqualTo(custom);
  }

  @Test
  public void loadFactoriesFromTCCL() throws Exception {
    ClassLoader custom = new URLClassLoader(new URL[]{new File("target/classpath/servicehelper").toURI().toURL()});

    // Try without the TCCL classloader.
    Collection<SomeFactory> factories = ServiceHelper.loadFactories(SomeFactory.class);
    assertThat(factories)
        .isNotNull()
        .hasSize(0);

    // Try with the TCCL classloader
    final ClassLoader originalTCCL = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(custom);
      factories = ServiceHelper.loadFactories(SomeFactory.class);
      assertThat(factories)
          .isNotNull()
          .hasSize(1);
      assertThat(factories.iterator().next().classloader()).isEqualTo(custom);
    } finally {
      Thread.currentThread().setContextClassLoader(originalTCCL);
    }

  }

  @Test
  public void loadFactoriesWithVertxClassloader() throws Exception {
    // This test is a bit more tricky as we need to load the ServiceHelper class from a custom classloader.
    ClassLoader custom = new URLClassLoader(new URL[]{
        new File("target/classes").toURI().toURL(),
        new File("target/test-classes").toURI().toURL(),
        new File("target/classpath/servicehelper").toURI().toURL(),
    }, null);

    Class serviceHelperClass = custom.loadClass(ServiceHelper.class.getName());
    Class someFactoryClass = custom.loadClass(SomeFactory.class.getName());
    assertThat(serviceHelperClass.getClassLoader()).isEqualTo(custom);
    assertThat(someFactoryClass.getClassLoader()).isEqualTo(custom);
    Method method = serviceHelperClass.getMethod("loadFactories", Class.class);
    Collection collection = (Collection) method.invoke(null, someFactoryClass);
    assertThat(collection).hasSize(1);
  }
}
