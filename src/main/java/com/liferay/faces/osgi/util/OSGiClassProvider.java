/**
 * Copyright (c) 2000-2017 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.liferay.faces.osgi.util;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @author  Kyle Stiemann
 */
public class OSGiClassProvider {

	public static Class<?> classForName(String name) throws ClassNotFoundException {
		return getClass(name, true, OSGiClassProvider.class.getClassLoader());
	}

	public static Class<?> classForName(String name, boolean initialize, ClassLoader suggestedClassLoader)
		throws ClassNotFoundException {
		return getClass(name, initialize, suggestedClassLoader);
	}

	public static Class<?> loadClass(String name, ClassLoader suggestedLoader) throws ClassNotFoundException {
		return getClass(name, null, suggestedLoader);
	}

	private static Class<?> getClass(String className, Boolean initialize, ClassLoader suggestedClassLoader)
		throws ClassNotFoundException {

		Class<?> clazz = null;

		if (FacesBundleUtil.isCurrentWarThinWab()) {

			FacesContext facesContext = FacesContext.getCurrentInstance();

			if (facesContext != null) {

				Map<String, Bundle> facesBundles = FacesBundleUtil.getFacesBundles(facesContext);

				if (className.startsWith("com.sun.faces") || className.startsWith("javax.faces")) {

					Bundle bundle = facesBundles.get(FacesBundleUtil.MOJARRA_SYMBOLIC_NAME);
					clazz = getClass(className, initialize, bundle);
				}
				else if (className.startsWith("com.liferay.faces.util")) {

					Bundle bundle = facesBundles.get("com.liferay.faces.util");
					clazz = getClass(className, initialize, bundle);
				}
				else if (className.startsWith("javax.portlet.faces")) {

					Bundle bundle = facesBundles.get("com.liferay.faces.bridge.api");
					clazz = getClass(className, initialize, bundle);
				}
				else if (className.startsWith("com.liferay.faces.bridge.ext")) {

					Bundle bundle = facesBundles.get("com.liferay.faces.bridge.ext");
					clazz = getClass(className, initialize, bundle);
				}
				else if (className.startsWith("com.liferay.faces.bridge") ||
						className.startsWith("com.liferay.faces.portlet")) {

					if (!className.contains(".internal.")) {

						Bundle bundle = facesBundles.get("com.liferay.faces.bridge.api");
						clazz = getClass(className, initialize, bundle);
					}

					if (clazz == null) {

						Bundle bundle = facesBundles.get("com.liferay.faces.bridge.impl");
						clazz = getClass(className, initialize, bundle);
					}
				}

//              else if (className.startsWith("com.liferay.faces.alloy")) {
//
//                  Bundle bundle = facesBundles.get("com.liferay.faces.alloy");
//                  clazz = getClass(className, initialize, bundle);
//              }
//              else if (className.startsWith(FacesBundleUtil.PRIMEFACES_SYMBOLIC_NAME)) {
//
//                  Bundle bundle = facesBundles.get(FacesBundleUtil.PRIMEFACES_SYMBOLIC_NAME);
//                  clazz = getClass(className, initialize, bundle);
//              }

				if (clazz == null) {

					Collection<Bundle> bundles = facesBundles.values();

					for (Bundle bundle : bundles) {

						if (!isClassFileInBundle(className, bundle)) {
							continue;
						}

						clazz = getClass(className, initialize, bundle);

						if (clazz != null) {
							break;
						}
					}
				}
			}
		}

		if (clazz == null) {

			if (initialize != null) {
				clazz = Class.forName(className, initialize, suggestedClassLoader);
			}
			else {
				clazz = suggestedClassLoader.loadClass(className);
			}
		}

		return clazz;
	}

	private static Class<?> getClass(String name, Boolean initialize, Bundle bundle) {

		Class<?> clazz = null;
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		ClassLoader classLoader = bundleWiring.getClassLoader();

		try {

			if (initialize != null) {
				clazz = Class.forName(name, initialize, classLoader);
			}
			else {
				clazz = classLoader.loadClass(name);
			}
		}
		catch (ClassNotFoundException e) {
			// no-op
		}

		return clazz;
	}

	private static boolean isClassFileInBundle(String className, Bundle bundle) {

		String classFilePath = "/" + className.replace(".", "/") + ".class";
		URL classFileURL = bundle.getResource(classFilePath);

		return classFileURL != null;
	}
}
