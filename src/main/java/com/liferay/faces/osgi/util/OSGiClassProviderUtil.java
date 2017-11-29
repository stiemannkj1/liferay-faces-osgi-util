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
import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;


/**
 * This static utility is designed to replace calls to class loading methods such as {@link
 * Class#forName(java.lang.String)}, {@link Class#forName(java.lang.String, boolean, java.lang.ClassLoader)}, and {@link
 * ClassLoader#loadClass(java.lang.String)}. This utility is designed to be used in conjunction with the Liferay Faces
 * OSGi Weaver which replaces calls to the aforementioned methods with appropriate calls to this utility's methods at
 * the bytecode level (however, code authors are free to call this utility's methods directly as well).
 *
 * @author  Kyle Stiemann
 */
public final class OSGiClassProviderUtil {

	private OSGiClassProviderUtil() {
		throw new AssertionError();
	}

	/**
	 * This method is intended to replace {@link Class#forName(java.lang.String)} in an OSGi environment, so in
	 * accordance with the <code>Class.forName()</code> JavaDoc, this method obtains the {@link ClassLoader} of the
	 * calling class and calls through to {@link #classForName(java.lang.String, boolean,
	 * javax.faces.context.FacesContext, java.lang.ClassLoader)} passing the class name, <code>true</code>, the {@link
	 * ServletContext}, and the ClassLoader obtained from the calling class.
	 *
	 * @param   className       The name of the class to be obtained. For more information, see {@link
	 *                          Class#forName(java.lang.String)}.
	 * @param   servletContext  The {@link ServletContext} of the current WAB or null if the ServletContext cannot be
	 *                          obtained.
	 * @param   callingClass    The calling class which will be used to obtain the ClassLoader which the caller wished
	 *                          to use to load the class. In static contexts pass MyClass.class. In non-static contexts
	 *                          pass the return value of this.getClass().
	 *
	 * @return
	 *
	 * @throws  ClassNotFoundException
	 */
	public static Class<?> classForName(String className, ServletContext servletContext, Class<?> callingClass)
		throws ClassNotFoundException {

		ClassLoader classLoader = callingClass.getClassLoader();

		return getClass(className, true, servletContext, classLoader);
	}

	/**
	 * This method is intended to replace {@link Class#forName(java.lang.String)} in an OSGi environment, so in
	 * accordance with the <code>Class.forName()</code> JavaDoc, this method obtains the {@link ClassLoader} of the
	 * calling class and calls through to {@link #classForName(java.lang.String, boolean,
	 * javax.faces.context.FacesContext, java.lang.ClassLoader)} passing the class name, <code>true</code>, the {@link
	 * FacesContext}, and the ClassLoader obtained from the calling class.
	 *
	 * @see     #classForName(java.lang.String, boolean, javax.faces.context.FacesContext, java.lang.ClassLoader)
	 *
	 * @param   className     The name of the class to be obtained. For more information, see {@link
	 *                        Class#forName(java.lang.String)}.
	 * @param   facesContext  The {@link FacesContext} of the current WAB (which can be obtained from {@link
	 *                        FacesContext#getCurrentInstance()}) or null if the FacesContext hasn't been initialized
	 *                        yet.
	 * @param   callingClass  The calling class which will be used to obtain the ClassLoader which the caller wished to
	 *                        use to load the class. In static contexts pass MyClass.class. In non-static contexts pass
	 *                        the return value of this.getClass().
	 *
	 * @return
	 *
	 * @throws  ClassNotFoundException
	 */
	public static Class<?> classForName(String className, FacesContext facesContext, Class<?> callingClass)
		throws ClassNotFoundException {

		ClassLoader classLoader = callingClass.getClassLoader();

		return classForName(className, true, facesContext, classLoader);
	}

	/**
	 * This method is intended to replace {@link Class#forName(java.lang.String, boolean, java.lang.ClassLoader)} in an
	 * OSGi environment. This method attempts to load the named class by iterating over the list of OSGi bundles
	 * returned by {@link FacesBundleUtil#getFacesBundles(java.lang.Object)} and checking if the bundle's ClassLoader
	 * can load the class (using {@link Class#forName(java.lang.String, boolean, java.lang.ClassLoader)}). If the class
	 * cannot be loaded by any bundle, the suggested ClassLoader is used in a final attempt to load the class.
	 *
	 * @param   className             The name of the class to be obtained. For more information, see {@link
	 *                                Class#forName(java.lang.String, boolean, java.lang.ClassLoader)}.
	 * @param   initialize            Determines whether the class should be initialized before it is returned. For more
	 *                                information, see {@link Class#forName(java.lang.String, boolean,
	 *                                java.lang.ClassLoader). @paramsuggestedClassLoaderThe ClassLoader which the caller
	 *                                wished to use to load the class. This is ignored unless all relevant OSGi bundles
	 *                                fail to load the class in which case it will be used in a final attempt to load
	 *                                the class. For more information, see {@link Class#forName(java.lang.String,
	 *                                boolean, java.lang.ClassLoader)}.
	 * @param   facesContext          The {@link FacesContext} of the current WAB (which can be obtained from {@link
	 *                                FacesContext#getCurrentInstance()}) or null if the FacesContext hasn't been
	 *                                initialized yet.
	 * @param   suggestedClassLoader  The ClassLoader which the caller wished to use to load the class. This is ignored
	 *                                unless all relevant OSGi bundles fail to load the class in which case it will be
	 *                                used in a final attempt to load the class. For more information, see {@link
	 *                                Class#forName(java.lang.String, boolean, java.lang.ClassLoader)}.
	 *
	 * @return
	 *
	 * @throws  ClassNotFoundException
	 */
	public static Class<?> classForName(String className, boolean initialize, FacesContext facesContext,
		ClassLoader suggestedClassLoader) throws ClassNotFoundException {
		return getClass(className, initialize, facesContext, suggestedClassLoader);
	}

	/**
	 * This method is intended to replace {@link ClassLoader#loadClass(java.lang.String)} in an OSGi environment. This
	 * method attempts to load the named class by iterating over the list of OSGi bundles returned by {@link
	 * FacesBundleUtil#getFacesBundles(java.lang.Object)} and checking if the bundle's ClassLoader can load the class
	 * (using {@link ClassLoader#loadClass(java.lang.String)}). If the class cannot be loaded by any bundle, the
	 * suggested ClassLoader is used in a final attempt to load the class.
	 *
	 * @param   className             The name of the class to be obtained. For more information, see {@link
	 *                                ClassLoader#loadClass(java.lang.String)}.
	 * @param   facesContext          The {@link FacesContext} of the current WAB (which can be obtained from {@link
	 *                                FacesContext#getCurrentInstance()}) or null if the FacesContext hasn't been
	 *                                initialized yet.
	 * @param   suggestedClassLoader  The ClassLoader which the caller wished to use to load the class. This is ignored
	 *                                unless all relevant OSGi bundles fail to load the class in which case it will be
	 *                                used in a final attempt to load the class. For more information, see {@link
	 *                                ClassLoader#loadClass(java.lang.String)}.
	 *
	 * @return
	 *
	 * @throws  ClassNotFoundException
	 */
	public static Class<?> loadClass(String className, FacesContext facesContext, ClassLoader suggestedClassLoader)
		throws ClassNotFoundException {
		return getClass(className, null, facesContext, suggestedClassLoader);
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

	private static Class<?> getClass(String className, Boolean initialize, Object context,
		ClassLoader suggestedClassLoader) throws ClassNotFoundException {

		Class<?> clazz = null;

		if (FacesBundleUtil.isCurrentWarThinWab() && (context != null)) {

			Map<String, Bundle> facesBundles = FacesBundleUtil.getFacesBundlesUsingServletContext(context);

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

		// If all else fails, try and do what the code originally intended to do.
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

	private static boolean isClassFileInBundle(String className, Bundle bundle) {

		String classFilePath = "/" + className.replace(".", "/") + ".class";
		URL classFileURL = bundle.getResource(classFilePath);

		return classFileURL != null;
	}
}
