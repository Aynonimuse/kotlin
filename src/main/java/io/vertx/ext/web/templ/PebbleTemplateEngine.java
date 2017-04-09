/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.web.templ;

import io.vertx.core.Vertx;
import io.vertx.ext.web.templ.impl.PebbleTemplateEngineImpl;

/**
 * A template engine that uses the Pebble library.
 *
 * @author Dan Kristensen
 */
public interface PebbleTemplateEngine extends TemplateEngine {

	/**
	 * Default max number of templates to cache
	 */
	int DEFAULT_MAX_CACHE_SIZE = 10000;

	/**
	 * Default template extension
	 */
	String DEFAULT_TEMPLATE_EXTENSION = "peb";

	/**
	 * Create a template engine using defaults
	 *
	 * @return the engine
	 */
	static io.vertx.ext.web.templ.PebbleTemplateEngine create(Vertx vertx) {
		return new PebbleTemplateEngineImpl(vertx);
	}

	/**
	 * Set the extension for the engine
	 *
	 * @param extension
	 *            the extension
	 * @return a reference to this for fluency
	 */
	io.vertx.ext.web.templ.PebbleTemplateEngine setExtension(String extension);

	/**
	 * Set the max cache size for the engine
	 *
	 * @param maxCacheSize
	 *            the maxCacheSize
	 * @return a reference to this for fluency
	 */
	io.vertx.ext.web.templ.PebbleTemplateEngine setMaxCacheSize(int maxCacheSize);
}
