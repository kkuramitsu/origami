/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.origami.nez.ast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

public interface LocaleFormat {

	public String error();

	public String warning();

	public String notice();

	public default String stringfy(String name) {
		String path = this.getClass().getName();
		try {
			return ResourceBundle.getBundle(path, Locale.getDefault(), new UTF8Control()).getString(name);
		} catch (java.util.MissingResourceException ex) {

		}
		try {
			return ResourceBundle.getBundle(path, Locale.ENGLISH).getString(name);
		} catch (java.util.MissingResourceException ex) {
			return name.replaceAll("__", ": ").replaceAll("_", " ").replace("S", "%s").replace("YY1", "%1$s")
					.replace("YY2", "%2$s");
		}
	}

	public class UTF8Control extends Control {
		@Override
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
				boolean reload) throws IllegalAccessException, InstantiationException, IOException {
			// The below is a copy of the default implementation.
			String bundleName = toBundleName(baseName, locale);
			String resourceName = toResourceName(bundleName, "properties");
			ResourceBundle bundle = null;
			InputStream stream = null;
			if (reload) {
				URL url = loader.getResource(resourceName);
				if (url != null) {
					URLConnection connection = url.openConnection();
					if (connection != null) {
						connection.setUseCaches(false);
						stream = connection.getInputStream();
					}
				}
			} else {
				stream = loader.getResourceAsStream(resourceName);
			}
			if (stream != null) {
				try {
					// Only this line is changed to make it to read properties
					// files as UTF-8.
					bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
				} finally {
					stream.close();
				}
			}
			return bundle;
		}
	}

	public static LocaleFormat wrap(String fmt) {
		class SimpleFormat implements LocaleFormat {

			private final String fmt;

			SimpleFormat(String fmt) {
				this.fmt = fmt;
			}

			@Override
			public String error() {
				return "error";
			}

			@Override
			public String warning() {
				return "warning";
			}

			@Override
			public String notice() {
				return "notice";
			}

			@Override
			public String toString() {
				return fmt;
			}

		}
		return new SimpleFormat(fmt);
	}

}