package de.a0h.javatemplater;

import java.util.ArrayList;

public class JavaSource {

	public static char FILE_SEPARATOR = System.getProperty("file.separator").charAt(0);

	public static String JAVA_SOURCE_DIR = "src/main/java/".replace('/', FILE_SEPARATOR);

	public static String JAVA_FILE_EXTENSION = ".java";

	public String packageName;

	public String preamble;

	public String caption;

	public String simpleClassName;

	public String body;

	public ArrayList<String> importList = new ArrayList<>();

	public String fileName;

	public static String getJavaFilename(String className) {
		return JAVA_SOURCE_DIR + className.replace('.', FILE_SEPARATOR) + JAVA_FILE_EXTENSION;
	}

	public void setClassName(String className) {
		int dotIdx = className.lastIndexOf('.');

		if (dotIdx != -1) {
			packageName = className.substring(0, dotIdx);
			simpleClassName = className.substring(dotIdx + 1);
		} else {
			packageName = null;
			simpleClassName = className;
		}
	}

	public String getClassName() {
		if (packageName == null) {
			return simpleClassName;
		} else {
			return packageName + '.' + simpleClassName;
		}
	}
}