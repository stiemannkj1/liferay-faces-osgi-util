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

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @author  Kyle Stiemann
 */
public final class FacesBundleUtil {

	// Private Constants
	private static final boolean FRAMEWORK_UTIL_DETECTED;

	// Package-Private Constants
	/* package-private */ static final String PRIMEFACES_SYMBOLIC_NAME = "org.primefaces";
	/* package-private */ static final String MOJARRA_SYMBOLIC_NAME = "org.glassfish.javax.faces";

	static {

		boolean frameworkUtilDetected = false;

		try {

			Class.forName("org.osgi.framework.FrameworkUtil");
			frameworkUtilDetected = true;
		}
		catch (Throwable t) {

			if (!((t instanceof NoClassDefFoundError) || (t instanceof ClassNotFoundException))) {

				System.err.println("An unexpected error occurred when attempting to detect OSGi:");
				t.printStackTrace(System.err);
			}
		}

		FRAMEWORK_UTIL_DETECTED = frameworkUtilDetected;
	}

	private FacesBundleUtil() {
		throw new AssertionError();
	}

	public static Map<String, Bundle> getFacesBundles(Object context) {

		Map<String, Bundle> facesBundles;

		if (FRAMEWORK_UTIL_DETECTED) {

			facesBundles = (Map<String, Bundle>) getServletContextAttribute(context, FacesBundleUtil.class.getName());

			if (facesBundles == null) {

				// LinkedHashMap is used to ensure that the WAB is the first bundle when iterating over all bundles.
				facesBundles = new LinkedHashMap<String, Bundle>();

				Bundle wabBundle = getCurrentFacesWab(context);
				facesBundles.put("currentFacesWab", wabBundle);

				// If the WAB's dependencies are not contained in the WAB's WEB-INF/lib, find all the WAB's
				// dependencies and return them as well.
				if (!FacesBundleUtil.isCurrentBundleThickWab()) {

					addRequiredBundlesRecurse(facesBundles, wabBundle);
					addBridgeImplBundles(facesBundles);
				}

				facesBundles = Collections.unmodifiableMap(facesBundles);
				setServletContextAttribute(context, FacesBundleUtil.class.getName(), facesBundles);
			}
		}
		else {
			facesBundles = Collections.emptyMap();
		}

		return facesBundles;
	}

	public static boolean isCurrentWarThinWab() {
		return FRAMEWORK_UTIL_DETECTED && !isCurrentBundleThickWab();
	}

	private static void addBridgeImplBundles(Map<String, Bundle> facesBundles) {

		Collection<Bundle> bundles = facesBundles.values();

		for (Bundle bundle : bundles) {

			String symbolicName = bundle.getSymbolicName();

			if (isBridgeBundle(symbolicName, "api")) {

				BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
				List<BundleWire> bundleWires = bundleWiring.getProvidedWires(BundleRevision.PACKAGE_NAMESPACE);
				boolean addedBridgeImplBundle = false;
				boolean addedBridgeExtBundle = false;

				for (BundleWire bundleWire : bundleWires) {

					Bundle bundleDependingOnBridgeAPI = bundleWire.getRequirer().getBundle();
					symbolicName = bundleDependingOnBridgeAPI.getSymbolicName();

					if (isBridgeBundle(symbolicName, "impl")) {

						facesBundles.put(symbolicName, bundleDependingOnBridgeAPI);
						addRequiredBundlesRecurse(facesBundles, bundleDependingOnBridgeAPI);
						addedBridgeImplBundle = true;
					}
					else if (isBridgeBundle(symbolicName, "ext")) {

						facesBundles.put(symbolicName, bundleDependingOnBridgeAPI);
						addRequiredBundlesRecurse(facesBundles, bundleDependingOnBridgeAPI);
						addedBridgeExtBundle = true;
					}

					if (addedBridgeImplBundle && addedBridgeExtBundle) {
						break;
					}
				}

				break;
			}
		}
	}

	private static void addRequiredBundlesRecurse(Map<String, Bundle> facesBundles, Bundle bundle) {

		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		List<BundleWire> bundleWires = bundleWiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);

		for (BundleWire bundleWire : bundleWires) {

			bundle = bundleWire.getProvider().getBundle();

			long bundleId = bundle.getBundleId();

			if (!((bundleId == 0) || facesBundles.containsValue(bundle))) {

				String key = Long.toString(bundleId);
				String symbolicName = bundle.getSymbolicName();

				if (symbolicName.startsWith("com.liferay.faces") || MOJARRA_SYMBOLIC_NAME.equals(symbolicName) ||
						PRIMEFACES_SYMBOLIC_NAME.equals(symbolicName)) {
					key = symbolicName;
				}

				facesBundles.put(key, bundle);
				addRequiredBundlesRecurse(facesBundles, bundle);
			}
		}
	}

	private static Bundle getCurrentFacesWab(Object context) {

		BundleContext bundleContext = (BundleContext) getServletContextAttribute(context, "osgi-bundlecontext");

		return bundleContext.getBundle();
	}

	private static Object getServletContextAttribute(Object context, String servletContextAttributeName) {

		Object servletContextAttributeValue;
		boolean isFacesContext = context instanceof FacesContext;

		if (isFacesContext || (context instanceof ExternalContext)) {

			ExternalContext externalContext;

			if (isFacesContext) {

				FacesContext facesContext = (FacesContext) context;
				externalContext = facesContext.getExternalContext();
			}
			else {
				externalContext = (ExternalContext) context;
			}

			Map<String, Object> applicationMap = externalContext.getApplicationMap();
			servletContextAttributeValue = applicationMap.get(servletContextAttributeName);
		}
		else if (context instanceof ServletContext) {

			ServletContext servletContext = (ServletContext) context;
			servletContextAttributeValue = servletContext.getAttribute(servletContextAttributeName);
		}
		else {
			throw new IllegalArgumentException("context [" + context.getClass().getName() + "] is not an instanceof " +
				FacesContext.class.getName() + " or " + ExternalContext.class.getName() + " or " +
				ServletContext.class.getName());
		}

		return servletContextAttributeValue;
	}

	private static boolean isBridgeBundle(String symbolicName, String bundleSymbolicNameSuffix) {

		String bridgeBundleSymbolicName = "com.liferay.faces.bridge." + bundleSymbolicNameSuffix;

		return symbolicName.equals(bridgeBundleSymbolicName);
	}

	private static boolean isCurrentBundleThickWab() {

		Bundle bundle = FrameworkUtil.getBundle(FacesBundleUtil.class);

		return isWab(bundle);
	}

	private static boolean isWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String webContextPathHeader = headers.get("Web-ContextPath");

		return webContextPathHeader != null;
	}

	private static void setServletContextAttribute(Object context, String servletContextAttributeName,
		Object servletContextAttributeValue) {

		boolean isFacesContext = context instanceof FacesContext;

		if (isFacesContext || (context instanceof ExternalContext)) {

			ExternalContext externalContext;

			if (isFacesContext) {

				FacesContext facesContext = (FacesContext) context;
				externalContext = facesContext.getExternalContext();
			}
			else {
				externalContext = (ExternalContext) context;
			}

			Map<String, Object> applicationMap = externalContext.getApplicationMap();
			applicationMap.put(servletContextAttributeName, servletContextAttributeValue);
		}
		else if (context instanceof ServletContext) {

			ServletContext servletContext = (ServletContext) context;
			servletContext.setAttribute(servletContextAttributeName, servletContextAttributeValue);
		}
		else {
			throw new IllegalArgumentException("context [" + context.getClass().getName() + "] is not an instanceof " +
				FacesContext.class.getName() + " or " + ExternalContext.class.getName() + " or " +
				ServletContext.class.getName());
		}
	}
}
